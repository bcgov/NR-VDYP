package ca.bc.gov.nrs.vdyp.common;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariableMode;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.model.VolumeComputeMode;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;

public class ComputationMethods {

	public static final Logger log = LoggerFactory.getLogger(VdypStartApplication.class);

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height, on Layer and Species objects.
	 */
	protected static final Collection<PropertyDescriptor> UTILIZATION_VECTOR_ACCESSORS;

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height and Quadratic Mean Diameter, on Layer and
	 * Species objects. These are properties where the values for the layer are the sum of those for its species.
	 */
	public static final Collection<PropertyDescriptor> SUMMABLE_UTILIZATION_VECTOR_ACCESSORS;

	/**
	 * Accessor methods for utilization vectors, except for Lorey Height,and Volume on Layer and Species objects.
	 */
	protected static final Collection<PropertyDescriptor> NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS;

	static {
		try {
			var bean = Introspector.getBeanInfo(VdypUtilizationHolder.class);
			UTILIZATION_VECTOR_ACCESSORS = Arrays.stream(bean.getPropertyDescriptors()) //
					.filter(p -> p.getName().endsWith("ByUtilization")) //
					.filter(p -> !p.getName().startsWith("loreyHeight")) //
					.filter(p -> p.getPropertyType() == UtilizationVector.class) //
					.toList();
		} catch (IntrospectionException e) {
			throw new IllegalStateException(e);
		}

		SUMMABLE_UTILIZATION_VECTOR_ACCESSORS = UTILIZATION_VECTOR_ACCESSORS.stream()
				.filter(x -> !x.getName().startsWith("quadraticMeanDiameter")).toList();

		NON_VOLUME_UTILIZATION_VECTOR_ACCESSORS = UTILIZATION_VECTOR_ACCESSORS.stream()
				.filter(x -> !x.getName().contains("Volume")).toList();
	}

	private final EstimationMethods estimationMethods;

	private final VdypApplicationIdentifier context;

	public ComputationMethods(EstimationMethods estimationMethods, VdypApplicationIdentifier context) {
		this.estimationMethods = estimationMethods;
		this.context = context;
	}

	/**
	 * YUC1 - compute Utilization components (quad-mean-diameter, basal area and trees-per-hectare) and, optionally,
	 * volumes for a polygon's primary layer.
	 *
	 * @param bec                       Bec zone
	 * @param vdypLayer                 (primary) layer in question
	 * @param volumeComputeMode         the {@link VolumeComputeMode} under which this method is to operate
	 * @param compatibilityVariableMode the {@link CompatibilityVariableMode} under which this method is to operate.
	 * @throws ProcessingException
	 */
	public void computeUtilizationComponentsPrimary(
			BecDefinition bec, VdypLayer vdypLayer, VolumeComputeMode volumeComputeMode,
			CompatibilityVariableMode compatibilityVariableMode
	) throws ProcessingException {
		log.atTrace().setMessage("computeUtilizationComponentsPrimary for {}, stand total age is {}")
				.addArgument(vdypLayer.getPolygonIdentifier()).addArgument(vdypLayer.getAgeTotal()).log();

		log.atDebug().setMessage("Primary layer for {} has {} species/genera: {}")
				.addArgument(vdypLayer::getPolygonIdentifier) //
				.addArgument(() -> vdypLayer.getSpecies().size()) //
				.addArgument(() -> vdypLayer.getSpecies().keySet().stream().collect(Collectors.joining(", "))) //
				.log();

		for (VdypSpecies spec : vdypLayer.getSpecies().values()) {
			float loreyHeightSpec = spec.getLoreyHeightByUtilization().getAll();
			float baseAreaSpec = spec.getBaseAreaByUtilization().getAll();
			float quadMeanDiameterSpec = spec.getQuadraticMeanDiameterByUtilization().getAll();
			float treesPerHectareSpec = spec.getTreesPerHectareByUtilization().getAll();

			log.atDebug().setMessage("Working with species {}  LH: {}  DQ: {}  BA: {}  TPH: {}")
					.addArgument(spec.getClass()).addArgument(loreyHeightSpec).addArgument(quadMeanDiameterSpec)
					.addArgument(baseAreaSpec).addArgument(treesPerHectareSpec);

			if (volumeComputeMode == VolumeComputeMode.BY_UTIL_WITH_WHOLE_STEM_BY_SPEC) {
				log.atDebug().log("Estimating tree volume");

				var volumeGroup = spec.getVolumeGroup();
				var meanVolume = this.estimationMethods
						.estimateWholeStemVolumePerTree(volumeGroup, loreyHeightSpec, quadMeanDiameterSpec);
				var specWholeStemVolume = treesPerHectareSpec * meanVolume;

				spec.getWholeStemVolumeByUtilization().setAll(specWholeStemVolume);
			}
			float wholeStemVolumeSpec = spec.getWholeStemVolumeByUtilization().getAll();

			var basalAreaUtil = Utils.utilizationVector();
			var quadMeanDiameterUtil = Utils.utilizationVector();
			var treesPerHectareUtil = Utils.utilizationVector();
			var wholeStemVolumeUtil = Utils.utilizationVector();
			var closeVolumeUtil = Utils.utilizationVector();
			var closeVolumeNetDecayUtil = Utils.utilizationVector();
			var closeVolumeNetDecayWasteUtil = Utils.utilizationVector();
			var closeVolumeNetDecayWasteBreakUtil = Utils.utilizationVector();

			basalAreaUtil.setAll(baseAreaSpec); // BAU
			quadMeanDiameterUtil.setAll(quadMeanDiameterSpec); // DQU
			treesPerHectareUtil.setAll(treesPerHectareSpec); // TPHU
			wholeStemVolumeUtil.setAll(wholeStemVolumeSpec); // WSU

			var adjustCloseUtil = Utils.utilizationVector(); // ADJVCU
			var adjustDecayUtil = Utils.utilizationVector(); // ADJVD
			var adjustDecayWasteUtil = Utils.utilizationVector(); // ADJVDW

			// EMP071
			estimationMethods.estimateQuadMeanDiameterByUtilization(bec, quadMeanDiameterUtil, spec.getGenus());

			// EMP070
			estimationMethods.estimateBaseAreaByUtilization(bec, quadMeanDiameterUtil, basalAreaUtil, spec.getGenus());

			// Calculate tree density components
			for (var uc : VdypStartApplication.UTIL_CLASSES) {
				treesPerHectareUtil.set(
						uc,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(basalAreaUtil.getCoe(uc.index), quadMeanDiameterUtil.get(uc))
				);
			}

			// reconcile components with totals

			// YUC1R
			ReconcilationMethods.reconcileComponents(basalAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);

			if (compatibilityVariableMode != CompatibilityVariableMode.NONE) {

				float basalAreaSumForSpecies = 0.0f;
				for (var uc : VdypStartApplication.UTIL_CLASSES) {

					float currentUcBasalArea = basalAreaUtil.get(uc);
					basalAreaUtil.set(uc, currentUcBasalArea + spec.getCvBasalArea(uc, spec.getLayerType()));
					if (basalAreaUtil.get(uc) < 0.0f) {
						basalAreaUtil.set(uc, 0.0f);
					}

					basalAreaSumForSpecies += basalAreaUtil.get(uc);

					float newDqValue = quadMeanDiameterUtil.get(uc)
							+ spec.getCvQuadraticMeanDiameter(uc, spec.getLayerType());
					quadMeanDiameterUtil.set(uc, FloatMath.clamp(newDqValue, uc.lowBound, uc.highBound));
				}

				if (basalAreaSumForSpecies > 0.0f) {
					float baMult = basalAreaUtil.get(UtilizationClass.ALL) / basalAreaSumForSpecies;

					for (UtilizationClass uc : UtilizationClass.ALL_CLASSES) {
						basalAreaUtil.set(uc, basalAreaUtil.get(uc) * baMult);
					}
				}
			}

			// Recalculate TPH's

			for (var uc : VdypStartApplication.UTIL_CLASSES) {
				treesPerHectareUtil.setCoe(
						uc.index,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(basalAreaUtil.getCoe(uc.index), quadMeanDiameterUtil.getCoe(uc.index))
				);
			}

			// Since DQ's may have changed, MUST RECONCILE AGAIN
			// Seems this might only be needed when compatibilityVariableMode is not NONE?

			// YUC1R
			ReconcilationMethods.reconcileComponents(basalAreaUtil, treesPerHectareUtil, quadMeanDiameterUtil);

			if (volumeComputeMode == VolumeComputeMode.ZERO) {
				throw new UnsupportedOperationException("TODO");
			} else {

				// EMP091
				estimationMethods.estimateWholeStemVolume(
						UtilizationClass.ALL, adjustCloseUtil.getCoe(4), spec.getVolumeGroup(), loreyHeightSpec,
						quadMeanDiameterUtil, basalAreaUtil, wholeStemVolumeUtil
				);

				if (compatibilityVariableMode == CompatibilityVariableMode.ALL) {
					// apply compatibility variables to WS volume

					float wholeStemVolumeSum = 0.0f;
					for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
						wholeStemVolumeUtil.set(
								uc,
								wholeStemVolumeUtil.get(uc) * FloatMath
										.exp(spec.getCvVolume(uc, VolumeVariable.WHOLE_STEM_VOL, spec.getLayerType()))
						);
						wholeStemVolumeSum += wholeStemVolumeUtil.get(uc);
					}
					wholeStemVolumeUtil.set(UtilizationClass.ALL, wholeStemVolumeSum);

					// Set the adjustment factors for next three volume types
					for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
						adjustCloseUtil
								.set(uc, spec.getCvVolume(uc, VolumeVariable.CLOSE_UTIL_VOL, spec.getLayerType()));
						adjustDecayUtil.set(
								uc, spec.getCvVolume(uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, spec.getLayerType())
						);
						adjustDecayWasteUtil.set(
								uc,
								spec.getCvVolume(
										uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, spec.getLayerType()
								)
						);
					}
				} else {
					// Do nothing as the adjustment vectors are already set to 0
				}

				// EMP092
				estimationMethods.estimateCloseUtilizationVolume(
						UtilizationClass.ALL, adjustCloseUtil, spec.getVolumeGroup(), loreyHeightSpec,
						quadMeanDiameterUtil, wholeStemVolumeUtil, closeVolumeUtil
				);

				// EMP093
				estimationMethods.estimateNetDecayVolume(
						spec.getGenus(), bec.getRegion(), UtilizationClass.ALL, adjustCloseUtil, spec.getDecayGroup(),
						vdypLayer.getBreastHeightAge().orElse(0f), quadMeanDiameterUtil, closeVolumeUtil,
						closeVolumeNetDecayUtil
				);

				// EMP094
				estimationMethods.estimateNetDecayAndWasteVolume(
						bec.getRegion(), UtilizationClass.ALL, adjustCloseUtil, spec.getGenus(), loreyHeightSpec,
						quadMeanDiameterUtil, closeVolumeUtil, closeVolumeNetDecayUtil, closeVolumeNetDecayWasteUtil
				);

				if (context.isStart()) {
					// EMP095
					estimationMethods.estimateNetDecayWasteAndBreakageVolume(
							UtilizationClass.ALL, spec.getBreakageGroup(), quadMeanDiameterUtil, closeVolumeUtil,
							closeVolumeNetDecayWasteUtil, closeVolumeNetDecayWasteBreakUtil
					);
				}
			}

			spec.getBaseAreaByUtilization().pairwiseInPlace(basalAreaUtil, EstimationMethods.COPY_IF_BAND);
			spec.getTreesPerHectareByUtilization().pairwiseInPlace(treesPerHectareUtil, EstimationMethods.COPY_IF_BAND);
			spec.getQuadraticMeanDiameterByUtilization()
					.pairwiseInPlace(quadMeanDiameterUtil, EstimationMethods.COPY_IF_BAND);

			spec.getWholeStemVolumeByUtilization()
					.pairwiseInPlace(wholeStemVolumeUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeByUtilization()
					.pairwiseInPlace(closeVolumeUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeNetOfDecayByUtilization()
					.pairwiseInPlace(closeVolumeNetDecayUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.pairwiseInPlace(closeVolumeNetDecayWasteUtil, EstimationMethods.COPY_IF_NOT_SMALL);
			spec.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization()
					.pairwiseInPlace(closeVolumeNetDecayWasteBreakUtil, EstimationMethods.COPY_IF_NOT_SMALL);

		}

		computeLayerUtilizationComponentsFromSpecies(vdypLayer);

		for (VdypSpecies spec : vdypLayer.getSpecies().values()) {
			if (vdypLayer.getBaseAreaByUtilization().getAll() > 0f) {
				spec.setFractionGenus(
						spec.getBaseAreaByUtilization().getAll() / vdypLayer.getBaseAreaByUtilization().getAll()
				);
			}
			log.atDebug().addArgument(spec.getGenus()).addArgument(spec.getFractionGenus())
					.setMessage("Species {} base area {}%").log();
		}

		log.atDebug().setMessage("Calculating Stand Lorey Height").log();

		vdypLayer.getLoreyHeightByUtilization().setSmall(0f);
		vdypLayer.getLoreyHeightByUtilization().setAll(0f);

		for (VdypSpecies spec : vdypLayer.getSpecies().values()) {
			log.atDebug() //
					.addArgument(spec.getGenus()) //
					.addArgument(() -> spec.getLoreyHeightByUtilization().getAll())
					.addArgument(() -> spec.getBaseAreaByUtilization().getAll())
					.addArgument(
							() -> spec.getLoreyHeightByUtilization().getAll() * spec.getBaseAreaByUtilization().getAll()
					)
					.setMessage(
							"For species {}, Species LH (7.5cm+): {}, Species BA (7.5cm+): {}, Weighted LH (7.5cm+): {}"
					).log();
			vdypLayer.getLoreyHeightByUtilization().scalarInPlace(
					UtilizationClass.SMALL,
					x -> x + spec.getLoreyHeightByUtilization().getSmall() * spec.getBaseAreaByUtilization().getSmall()
			);
			vdypLayer.getLoreyHeightByUtilization().scalarInPlace(
					UtilizationClass.ALL,
					x -> x + spec.getLoreyHeightByUtilization().getAll() * spec.getBaseAreaByUtilization().getAll()
			);
		}
		{
			float baSmall = vdypLayer.getBaseAreaByUtilization().getSmall();
			float baAll = vdypLayer.getBaseAreaByUtilization().getAll();

			if (baSmall > 0) {
				vdypLayer.getLoreyHeightByUtilization().scalarInPlace(UtilizationClass.SMALL, x -> x / baSmall);
			}
			if (baAll > 0) {
				vdypLayer.getLoreyHeightByUtilization().scalarInPlace(UtilizationClass.ALL, x -> x / baAll);
			}

		}

	}

	/**
	 * Sets the Layer's utilization components based on those of its species.
	 *
	 * @param vdypLayer
	 */
	protected static void computeLayerUtilizationComponentsFromSpecies(VdypLayer vdypLayer) {

		// Layer utilization vectors other than quadratic mean diameter are the pairwise
		// sums of those of their species
		sumSpeciesUtilizationVectorsToLayer(vdypLayer);

		{
			var hlVector = Utils.heightVector();
			vdypLayer.getSpecies().values().stream().forEach(spec -> {
				var ba = spec.getBaseAreaByUtilization();
				hlVector.pairwiseInPlace(
						spec.getLoreyHeightByUtilization(),
						(float x, float y, UtilizationClass uc) -> x + y * ba.get(uc)
				);
			});
			var ba = vdypLayer.getBaseAreaByUtilization();
			hlVector.scalarInPlace((float x, UtilizationClass uc) -> ba.get(uc) > 0 ? x / ba.get(uc) : x);
			vdypLayer.setLoreyHeightByUtilization(hlVector);
		}

		// Quadratic mean diameter for the layer is computed from the BA and TPH after
		// they have been found from the species
		{
			var utilVector = vdypLayer.getBaseAreaByUtilization().pairwise(
					vdypLayer.getTreesPerHectareByUtilization(), BaseAreaTreeDensityDiameter::quadMeanDiameter
			);
			vdypLayer.setQuadraticMeanDiameterByUtilization(utilVector);
		}
	}

	// TODO De-reflectify this when we want to make it work in GraalVM
	private static void sumSpeciesUtilizationVectorsToLayer(VdypLayer vdypLayer) throws IllegalStateException {
		try {
			for (var accessors : SUMMABLE_UTILIZATION_VECTOR_ACCESSORS) {
				var utilVector = Utils.utilizationVector();
				for (var vdypSpecies : vdypLayer.getSpecies().values()) {
					var speciesVector = (Coefficients) accessors.getReadMethod().invoke(vdypSpecies);
					utilVector.pairwiseInPlace(speciesVector, (x, y) -> x + y);
				}
				accessors.getWriteMethod().invoke(vdypLayer, utilVector);
			}
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

	// TODO De-reflectify this when we want to make it work in GralVM
	protected static void scaleAllSummableUtilization(VdypUtilizationHolder holder, float factor)
			throws IllegalStateException {
		try {
			for (var accessors : SUMMABLE_UTILIZATION_VECTOR_ACCESSORS) {
				((Coefficients) accessors.getReadMethod().invoke(holder)).scalarInPlace(x -> x * factor);
			}
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
