package ca.bc.gov.nrs.vdyp.common;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.abs;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.exp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.log;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.pow;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.ratio;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.sqrt;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSite;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector.BinaryOperatorWithClass;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;

public class EstimationMethods {

	public static final Logger log = LoggerFactory.getLogger(EstimationMethods.class);

	/**
	 * Returns the new value if the index is that of a utilization class that represents a size band, otherwise the old
	 * value
	 */
	public static final BinaryOperatorWithClass COPY_IF_BAND = (
			oldX, newX, uc
	) -> UtilizationClass.UTIL_CLASSES.contains(uc) ? newX : oldX;

	/**
	 * Returns the new value if the index is that of a utilization class is not the small band, otherwise the old value
	 */
	public static final BinaryOperatorWithClass COPY_IF_NOT_SMALL = (
			oldX, newX, uc
	) -> UtilizationClass.ALL_BUT_SMALL.contains(uc) ? newX : oldX;

	public static final float EMPIRICAL_OCCUPANCY = 0.85f;

	ResolvedControlMap controlMap;

	public EstimationMethods(ResolvedControlMap resolvedControlMap) {
		this.controlMap = resolvedControlMap;
	}

	private float heightMultiplier(String genus, Region region, float treesPerHectarePrimary) {
		final var coeMap = controlMap
				.<MatrixMap2<String, Region, Coefficients>>get(ControlKey.HL_PRIMARY_SP_EQN_P1, MatrixMap2.class);
		var coe = coeMap.get(genus, region).reindex(0);
		return coe.get(0) - coe.getCoe(1) + coe.getCoe(1) * exp(coe.getCoe(2) * (treesPerHectarePrimary - 100f));
	}

	/**
	 * EMP050 Method 1: Return the lorey height of the primary species based on the dominant height of the lead species.
	 *
	 * @param leadHeight             dominant height of the lead species
	 * @param genus                  Primary species
	 * @param region                 Region of the polygon
	 * @param treesPerHectarePrimary trees per hectare >7.5 cm of the primary species
	 * @return
	 */
	public float primaryHeightFromLeadHeight(
			float leadHeight, String genus, Region region, float treesPerHectarePrimary
	) {
		return 1.3f + (leadHeight - 1.3f) * heightMultiplier(genus, region, treesPerHectarePrimary);
	}

	/**
	 * EMP050 Method 2: Return the dominant height of the lead species based on the lorey height of the primary species.
	 *
	 * @param primaryHeight          lorey height of the primary species
	 * @param genus                  Primary species
	 * @param region                 Region of the polygon
	 * @param treesPerHectarePrimary trees per hectare >7.5 cm of the primary species
	 */
	public float leadHeightFromPrimaryHeight(
			float primaryHeight, String genus, Region region, float treesPerHectarePrimary
	) {
		return 1.3f + (primaryHeight - 1.3f) / heightMultiplier(genus, region, treesPerHectarePrimary);
	}

	/**
	 * EMP051 Return the lorey height of the primary species based on the dominant height of the lead species.
	 *
	 * @param controlMap Control map containing coefficients
	 * @param leadHeight dominant height of the lead species
	 * @param genus      Primary species
	 * @param region     Region of the polygon
	 */
	public float primaryHeightFromLeadHeightInitial(float leadHeight, String genus, Region region) {
		final var coeMap = controlMap
				.<MatrixMap2<String, Region, Coefficients>>get(ControlKey.HL_PRIMARY_SP_EQN_P2, MatrixMap2.class);
		var coe = coeMap.get(genus, region);
		return 1.3f + coe.getCoe(1) * pow(leadHeight - 1.3f, coe.getCoe(2));
	}

	/**
	 * EMP053 Estimate the lorey height of a non-primary species of a primary layer.
	 * <p>
	 * Using eqns N1 and N2 from ipsjf124.doc
	 *
	 *
	 * @param vspec         The species.
	 * @param vspecPrime    The primary species.
	 * @param leadHeight    lead height of the layer
	 * @param primaryHeight height of the primary species
	 * @throws ProcessingException
	 */
	public float estimateNonPrimaryLoreyHeight(
			BaseVdypSpecies<? extends BaseVdypSite> vspec, BaseVdypSpecies<? extends BaseVdypSite> vspecPrime,
			BecDefinition bec,
			float leadHeight, float primaryHeight
	) throws ProcessingException {
		return estimateNonPrimaryLoreyHeight(vspec.getGenus(), vspecPrime.getGenus(), bec, leadHeight, primaryHeight);
	}

	/**
	 * EMP053 Estimate the lorey height of a non-primary species of a primary layer.
	 * <p>
	 * Using eqns N1 and N2 from ipsjf124.doc
	 *
	 * @param vspec         The species.
	 * @param vspecPrime    The primary species.
	 * @param leadHeight    lead height of the layer
	 * @param primaryHeight height of the primary species
	 * @throws ProcessingException
	 */
	public float estimateNonPrimaryLoreyHeight(
			String vspec, String vspecPrime, BecDefinition bec, float leadHeight, float primaryHeight
	) throws ProcessingException {
		var coeMap = controlMap.<MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>>get(
				ControlKey.HL_NONPRIMARY, MatrixMap3.class
		);

		var coe = coeMap.get(vspec, vspecPrime, bec.getRegion()).orElseThrow(
				() -> new ProcessingException(
						String.format(
								"Could not find Lorey Height Nonprimary Coefficients for %s %s %s", vspec, vspecPrime, bec
										.getRegion()
						)
				)
		);
		var heightToUse = coe.getEquationIndex() == 1 ? leadHeight : primaryHeight;
		return 1.3f + coe.getCoe(1) * pow(heightToUse - 1.3f, coe.getCoe(2));
	}

	// EMP060
	/**
	 * Estimate DQ for a species (primary or not). Using eqn in jf125.doc.
	 *
	 * Enforces mins and maxes from EMP061.
	 *
	 * @param spec                  Species of insterest
	 * @param allSpecies            Collection of all species on the layer
	 * @param region                BEC Region of the stand
	 * @param standQuadMeanDiameter Quadratic mean diameter of the stand
	 * @param standBaseArea         Base area of the stand
	 * @param standTreesPerHectare  Density opf the stand
	 * @param standLoreyHeight      Lorey height of the stand
	 * @return Quadratic mean diameter of the species of interest
	 * @throws ProcessingException
	 */
	public float estimateQuadMeanDiameterForSpecies(
			VdypSpecies spec, // ISP, HLsp, DQsp
			Map<String, VdypSpecies> allSpecies, // FR
			Region region, // INDEX_IC
			float standQuadMeanDiameter, // DQ_TOT
			float standBaseArea, // BA_TOT
			float standTreesPerHectare, // TPH_TOT
			float standLoreyHeight // HL_TOT
	) throws ProcessingException {

		Map<String, Float> basalAreaFractionPerSpecies = new HashMap<>();
		allSpecies.values().stream().forEach(s -> basalAreaFractionPerSpecies.put(s.getGenus(), s.getFractionGenus()));

		return estimateQuadMeanDiameterForSpecies(
				spec.getGenus(), spec.getLoreyHeightByUtilization().get(
						UtilizationClass.ALL
				), spec.getQuadraticMeanDiameterByUtilization().get(
						UtilizationClass.ALL
				), basalAreaFractionPerSpecies, region, standQuadMeanDiameter, standBaseArea, standTreesPerHectare, standLoreyHeight
		);

	}

	// EMP060
	/**
	 * Estimate DQ for a species (primary or not). Using eqn in jf125.doc.
	 *
	 * Enforces mins and maxes from EMP061.
	 *
	 * @param spAlias               The alias of the species
	 * @param spLoreyHeight			The lorey height (all utilizations) of the species 
	 * @param spQuadMeanDiameter    The quad-mean-diameter (all utilizations) of the species
	 * @param basalAreaFractionPerSpecies Basal area fractions of the per species in the stand
	 * @param region                BEC Region of the stand
	 * @param standQuadMeanDiameter Quadratic mean diameter of the stand
	 * @param standBaseArea         Base area of the stand
	 * @param standTreesPerHectare  Density opf the stand
	 * @param standLoreyHeight      Lorey height of the stand
	 * @return Quadratic mean diameter of the species of interest
	 * @throws ProcessingException
	 */
	public float estimateQuadMeanDiameterForSpecies(
			String spAlias,
			float spLoreyHeight, // HLsp
			float spQuadMeanDiameter, // DQsp
			Map<String, Float> basalAreaFractionPerSpecies, // FR
			Region region, // INDEX_IC
			float standQuadMeanDiameter, // DQ_TOT
			float standBaseArea, // BA_TOT
			float standTreesPerHectare, // TPH_TOT
			float standLoreyHeight // HL_TOT
	) throws ProcessingException {

		float c = 0.00441786467f;

		float minQuadMeanDiameter = min(7.6f, standQuadMeanDiameter);

		Float spFraction = basalAreaFractionPerSpecies.get(spAlias);

		// Quick solution
		if (spFraction >= 1f || standQuadMeanDiameter < minQuadMeanDiameter) {
			return standQuadMeanDiameter;
		}

		var coeMap = controlMap.<Map<String, Coefficients>>get(ControlKey.BY_SPECIES_DQ, Map.class);
		var specAliases = controlMap.getGenusDefinitionMap().getAliases();

		var specIt = specAliases.iterator();
		var spec1 = specIt.next();

		float a2 = coeMap.get(spec1).getCoe(2);

		float fractionOther = 1f - spFraction; // FR_REST

		float a0 = coeMap.get(spec1).getCoe(0);
		float a1 = coeMap.get(spec1).getCoe(1);

		while (specIt.hasNext()) {
			var specIAlias = specIt.next();
			if (spAlias.equals(specIAlias)) {
				float multI = 1f;
				a0 += multI * coeMap.get(specIAlias).getCoe(0);
				a1 += multI * coeMap.get(specIAlias).getCoe(1);
			} else {
				float spIFraction = basalAreaFractionPerSpecies.get(specIAlias);
				if (spIFraction > 0f) {
					float multI = -spIFraction / fractionOther;
					a0 += multI * coeMap.get(specIAlias).getCoe(0);
					a1 -= multI * coeMap.get(specIAlias).getCoe(1);
				}
			}
		}

		float loreyHeightSpec = spLoreyHeight;
		float loreyHeight1 = max(4f, loreyHeightSpec);
		float loreyHeight2 = (standLoreyHeight - loreyHeightSpec * spFraction) / fractionOther;
		float loreyHeightRatio = clamp( (loreyHeight1 - 3f) / (loreyHeight2 - 3f), 0.05f, 20f);

		float r = exp(
				a0 + a1 * log(loreyHeightRatio) + a2 * log(standQuadMeanDiameter)
		);

		float baseArea1 = spFraction * standBaseArea;
		float baseArea2 = standBaseArea - baseArea1;

		float treesPerHectare1;
		if (abs(r - 1f) < 0.0005) {
			treesPerHectare1 = spFraction * standTreesPerHectare;
		} else {
			float aa = (r - 1f) * c;
			float bb = c * (1f - r) * standTreesPerHectare + baseArea1 + baseArea2 * r;
			float cc = -baseArea1 * standTreesPerHectare;
			float term = bb * bb - 4 * aa * cc;
			if (term <= 0f) {
				throw new ProcessingException(
						"Term for trees per hectare calculation when estimating quadratic mean diameter for species "
								+ spAlias + " was " + term + " but should be positive."
				);
			}
			treesPerHectare1 = (-bb + sqrt(term)) / (2f * aa);
			if (treesPerHectare1 <= 0f || treesPerHectare1 > standTreesPerHectare) {
				throw new ProcessingException(
						"Trees per hectare 1 for species " + spAlias + " was " + treesPerHectare1
								+ " but should be positive and less than or equal to stand trees per hectare "
								+ standTreesPerHectare
				);
			}
		}

		float quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);
		float treesPerHectare2 = standTreesPerHectare - treesPerHectare1;
		float quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, treesPerHectare2);
		var limits = getLimitsForHeightAndDiameter(spAlias, region);

		quadMeanDiameter1 = estimateQuadMeanDiameterClampResult(
				limits, standTreesPerHectare, minQuadMeanDiameter, loreyHeightSpec, baseArea1, baseArea2, quadMeanDiameter1, treesPerHectare2, quadMeanDiameter2
		);
		return quadMeanDiameter1;
	}

	float estimateQuadMeanDiameterClampResult(
			ComponentSizeLimits limits, float standTreesPerHectare, float minQuadMeanDiameter, float loreyHeightSpec,
			float baseArea1, float baseArea2, float quadMeanDiameter1, float treesPerHectare2, float quadMeanDiameter2
	) {
		float treesPerHectare1;
		if (quadMeanDiameter2 < minQuadMeanDiameter) {
			// species 2 is too small. Make target species smaller.
			quadMeanDiameter2 = minQuadMeanDiameter;
			treesPerHectare2 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea2, quadMeanDiameter2);
			treesPerHectare1 = standTreesPerHectare - treesPerHectare2;
			quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);
		}

		final float dqMinSp = max(minQuadMeanDiameter, limits.minQuadMeanDiameterLoreyHeightRatio() * loreyHeightSpec);
		final float dqMaxSp = max(
				7.6f, min(
						limits.quadMeanDiameterMaximum(), limits.maxQuadMeanDiameterLoreyHeightRatio() * loreyHeightSpec
				)
		);
		if (quadMeanDiameter1 < dqMinSp) {
			quadMeanDiameter1 = dqMinSp;
			treesPerHectare1 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea1, quadMeanDiameter1);
			treesPerHectare2 = standTreesPerHectare - treesPerHectare2;
			quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, treesPerHectare2);
		}
		if (quadMeanDiameter1 > dqMaxSp) {
			// target species is too big. Make target species smaller, DQ2 bigger.

			quadMeanDiameter1 = dqMaxSp;
			treesPerHectare1 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea1, quadMeanDiameter1);
			treesPerHectare2 = standTreesPerHectare - treesPerHectare2;

			if (treesPerHectare2 > 0f && baseArea2 > 0f) {
				quadMeanDiameter2 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea2, treesPerHectare2);
			} else {
				quadMeanDiameter2 = 1000f;
			}

			// under rare circumstances, let DQ1 exceed DQMAXsp
			if (quadMeanDiameter2 < minQuadMeanDiameter) {
				quadMeanDiameter2 = minQuadMeanDiameter;
				treesPerHectare2 = BaseAreaTreeDensityDiameter.treesPerHectare(baseArea2, quadMeanDiameter2);
				treesPerHectare1 = standTreesPerHectare - treesPerHectare2;
				quadMeanDiameter1 = BaseAreaTreeDensityDiameter.quadMeanDiameter(baseArea1, treesPerHectare1);
			}

		}
		return quadMeanDiameter1;
	}

	// EMP061
	public ComponentSizeLimits getLimitsForHeightAndDiameter(String genus, Region region) {
		var coeMap = controlMap.<MatrixMap2<String, Region, Coefficients>>get(
				ControlKey.SPECIES_COMPONENT_SIZE_LIMIT, MatrixMap2.class
		);

		var coe = coeMap.get(genus, region);
		return new ComponentSizeLimits(coe.getCoe(1), coe.getCoe(2), coe.getCoe(3), coe.getCoe(4));
	}

	/**
	 * EMP070. Estimate basal area by utilization class from the given parameters, after getting the estimation
	 * coefficients map from the control map.
	 *
	 * @param bec
	 * @param quadMeanDiameterUtil
	 * @param baseAreaUtil
	 * @param genus
	 * @throws ProcessingException
	 */
	public void estimateBaseAreaByUtilization(
			BecDefinition bec, UtilizationVector quadMeanDiameterUtil, UtilizationVector baseAreaUtil, String genus
	) throws ProcessingException {

		final var coeMap = controlMap
				.<MatrixMap3<Integer, String, String, Coefficients>>get(ControlKey.UTIL_COMP_BA, MatrixMap3.class);

		estimateBaseAreaByUtilization(bec, coeMap, quadMeanDiameterUtil, baseAreaUtil, genus);
	}

	/**
	 * EMP070: estimate basal area by utilization class from the given parameters
	 *
	 * @param bec
	 * @param basalAreaUtilCompCoeMap
	 * @param quadMeanDiameterUtil
	 * @param baseAreaUtil
	 * @param genus
	 * @throws ProcessingException
	 */
	public static void estimateBaseAreaByUtilization(
			BecDefinition bec, MatrixMap3<Integer, String, String, Coefficients> basalAreaUtilCompCoeMap,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector baseAreaUtil, String genus
	) throws ProcessingException {

		float dq = quadMeanDiameterUtil.getAll();
		var b = Utils.utilizationVector();
		b.setCoe(0, baseAreaUtil.getAll());

		for (UtilizationClass uc : UtilizationClass.ALL_BANDS_BUT_LARGEST) {
			var coe = basalAreaUtilCompCoeMap.get(uc.index, genus, bec.getGrowthBec().getAlias());

			float a0 = coe.getCoe(1);
			float a1 = coe.getCoe(2);

			float logit;
			if (uc == UtilizationClass.U75TO125) {
				logit = a0 + a1 * pow(dq, 0.25f);
			} else {
				logit = a0 + a1 * dq;
			}
			b.set(uc, b.get(uc.previous().get()) * exponentRatio(logit));
			if (uc == UtilizationClass.U75TO125 && quadMeanDiameterUtil.getAll() < 12.5f) {
				float ba12Max = (1f
						- pow( (quadMeanDiameterUtil.getCoe(1) - 7.4f) / (quadMeanDiameterUtil.getAll() - 7.4f), 2f))
						* b.getCoe(0);
				b.scalarInPlace(1, x -> min(x, ba12Max));
			}
		}

		baseAreaUtil.setCoe(1, baseAreaUtil.getAll() - b.getCoe(1));
		baseAreaUtil.setCoe(2, b.getCoe(1) - b.getCoe(2));
		baseAreaUtil.setCoe(3, b.getCoe(2) - b.getCoe(3));
		baseAreaUtil.setCoe(4, b.getCoe(3));
	}

	/**
	 * EMP071. Estimate DQ by utilization class, see ipsjf120.doc.
	 *
	 * @param bec
	 * @param quadMeanDiameterUtil
	 * @param genus
	 * @throws ProcessingException
	 */
	public void estimateQuadMeanDiameterByUtilization(
			BecDefinition bec, UtilizationVector quadMeanDiameterUtil, String genus
	) throws ProcessingException {

		final var coeMap = controlMap
				.<MatrixMap3<Integer, String, String, Coefficients>>get(ControlKey.UTIL_COMP_DQ, MatrixMap3.class);

		estimateQuadMeanDiameterByUtilization(bec, coeMap, quadMeanDiameterUtil, genus);
	}

	/**
	 * EMP071. Estimate DQ by utilization class, see ipsjf120.doc.
	 *
	 * @param bec
	 * @param coeMap
	 * @param quadMeanDiameterUtil
	 * @param genus
	 * @throws ProcessingException
	 */
	public static void estimateQuadMeanDiameterByUtilization(
			BecDefinition bec, MatrixMap3<Integer, String, String, Coefficients> coeMap,
			UtilizationVector quadMeanDiameterUtil, String genus
	) throws ProcessingException {
		log.atTrace().setMessage("Estimate DQ by utilization class for {} in BEC {}.  DQ for all >7.5 is {}")
				.addArgument(genus).addArgument(bec.getName()).addArgument(quadMeanDiameterUtil.getAll());

		float quadMeanDiameter07 = quadMeanDiameterUtil.getAll();

		for (var uc : UtilizationClass.UTIL_CLASSES) {
			log.atDebug().setMessage("For util level {}").addArgument(uc.className);
			var coe = coeMap.get(uc.index, genus, bec.getGrowthBec().getAlias());

			float a0 = coe.getCoe(1);
			float a1 = coe.getCoe(2);
			float a2 = coe.getCoe(3);

			log.atDebug().setMessage("a0={}, a1={}, a3={}").addArgument(a0).addArgument(a1).addArgument(a2);

			float logit;

			switch (uc) {
			case U75TO125:
				if (quadMeanDiameter07 < 7.5001f) {
					quadMeanDiameterUtil.setAll(7.5f);
				} else {
					log.atDebug().setMessage("DQ = 7.5 + a0 * (1 - exp(a1 / a0*(DQ07 - 7.5) ))**a2' )");

					logit = a1 / a0 * (quadMeanDiameter07 - 7.5f);

					quadMeanDiameterUtil
							.setCoe(uc.index, min(7.5f + a0 * pow(1 - safeExponent(logit), a2), quadMeanDiameter07));
				}
				break;
			case U125TO175, U175TO225:
				log.atDebug().setMessage(
						"LOGIT = a0 + a1*(SQ07 / 7.5)**a2,  DQ = (12.5 or 17.5) + 5 * exp(LOGIT) / (1 + exp(LOGIT))"
				);
				logit = a0 + a1 * pow(quadMeanDiameter07 / 7.5f, a2);

				quadMeanDiameterUtil.setCoe(uc.index, uc.lowBound + 5f * exponentRatio(logit));
				break;
			case OVER225:
				float a3 = coe.getCoe(4);

				log.atDebug().setMessage(
						"Coeff A3 {}, LOGIT = a2 + a1*DQ07**a3,  DQ = DQ07 + a0 * (1 - exp(LOGIT) / (1 + exp(LOGIT)) )"
				);

				logit = a2 + a1 * pow(quadMeanDiameter07, a3);

				quadMeanDiameterUtil
						.setCoe(uc.index, max(22.5f, quadMeanDiameter07 + a0 * (1f - exponentRatio(logit))));
				break;
			case ALL, SMALL:
				throw new IllegalStateException(
						"Should not be attempting to process small component or all large components"
				);
			default:
				throw new IllegalStateException("Unknown utilization class " + uc);
			}

			log.atDebug().setMessage("Util DQ for class {} is {}").addArgument(uc.className)
					.addArgument(quadMeanDiameterUtil.getCoe(uc.index));
		}

		log.atTrace().setMessage("Estimated Diameters {}").addArgument(
				() -> UtilizationClass.UTIL_CLASSES.stream()
						.map(uc -> String.format("%s: %d", uc.className, quadMeanDiameterUtil.getCoe(uc.index)))
		);

	}

	/**
	 * EMP090. Return an estimate of the volume, per tree, of the whole stem, based on the given lorey height and quad
	 * mean diameter.
	 *
	 * @param volumeGroup      the species' volume group
	 * @param loreyHeight      the species' lorey height
	 * @param quadMeanDiameter the species' quadratic mean diameter
	 * @return as described
	 */
	public float estimateWholeStemVolumePerTree(int volumeGroup, float loreyHeight, float quadMeanDiameter) {

		var coeMap = controlMap.<Map<Integer, Coefficients>>get(ControlKey.TOTAL_STAND_WHOLE_STEM_VOL, Map.class);

		return estimateWholeStemVolumePerTree(volumeGroup, loreyHeight, quadMeanDiameter, coeMap);
	}

	/**
	 * EMP106 - estimate basal area yield for the primary layer (from IPSJF160.doc)
	 * 
	 * @param coefficients estimate basal area yield coefficients
	 * @param controlVariable2Setting the value of control variable 2
	 * @param dominantHeight dominant height (m)
	 * @param breastHeightAge breast height age (years)
	 * @param veteranBaseArea basal area of overstory (>= 0)
	 * @param basalAreaGroup index of the basal area group
	 * @param fullOccupancy if true, the empirically fitted curve is increased to become 
	 *                      a full occupancy curve. If false, BAP is for mean conditions.
	 * @return
	 * @throws StandProcessingException
	 */
	public float estimateBaseAreaYield(
			Coefficients estimateBasalAreaYieldCoefficients, int controlVariable2Setting, float dominantHeight,
			float breastHeightAge, Optional<Float> veteranBasalArea, boolean fullOccupancy, int baseAreaGroup
	) throws StandProcessingException {
		float upperBoundBaseArea = upperBoundsBaseArea(baseAreaGroup);

		// The original Fortran had the following comment and a commented out modification to upperBoundsBaseArea
		// (BATOP98):

		/*
		 * And one POSSIBLY one last vestige of grouping by ITG.
		 * 
		 * That limit applies to full occupancy and Empirical occupancy. They were derived as the 98th percentile 
		 * of Empirical stocking, though adjusted PSPs were included. If the ouput of this routine is bumped up 
		 * from empirical to full, MIGHT adjust this limit DOWN here, so that at end, it is correct. Tentatively 
		 * decide NOT to do this:
		 */

		// 		if (fullOccupancy) {
		//     		upperBoundsBaseArea *= EMPIRICAL_OCCUPANCY;
		// 		}

		float ageToUse = breastHeightAge;

		if (controlVariable2Setting > 0) {
			ageToUse = Math.min(ageToUse, controlVariable2Setting * 100);
		}

		if (ageToUse <= 0f) {
			throw new StandProcessingException("Age was not positive");
		}

		float trAge = FloatMath.log(ageToUse);

		float a0 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a1 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a2 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a3 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a4 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a5 = estimateBasalAreaYieldCoefficients.getCoe(0);
		float a6 = estimateBasalAreaYieldCoefficients.getCoe(0);

		float a00 = Math.max(a0 + a1 * trAge, 0);
		float ap = Math.max(a3 + a4 * trAge, 0);

		float bap;
		if (dominantHeight <= a2) {
			bap = 0;
		} else {
			bap = a00 * FloatMath.pow(dominantHeight - a2, ap)
					* FloatMath.exp(a5 * dominantHeight + a6 * veteranBasalArea.orElse(0f));
			bap = Math.min(bap, upperBoundBaseArea);
		}

		if (fullOccupancy) {
			bap /= EMPIRICAL_OCCUPANCY;
		}

		return bap;
	}

	/**
	 * EMP107 - estimate DQ yield for the primary layer (from IPSJF161.doc)
	 * 
	 * @param coefficients coefficients weighted by species and decay bec zone
	 * @param controlVariable2Setting the value of control variable 2
	 * @param dominantHeight dominant height (m)
	 * @param breastHeightAge breast height age (years)
	 * @param veteranBaseArea basal area of overstory (>= 0)
	 * @param basalAreaGroup index of the basal area group
	 * 
	 * @return quad-mean-diameter of primary layer (with DBH >= 7.5)
	 * 
	 * @throws StandProcessingException in the event of a processing error
	 */
	public float estimateQuadMeanDiameterYield(
			Coefficients coefficients, int controlVariable2Setting, float dominantHeight,
			float breastHeightAge, Optional<Float> veteranBaseArea, int basalAreaGroup
	) throws StandProcessingException {

		if (dominantHeight <= 5) {
			return 7.6f;
		}

		final float upperBoundsQuadMeanDiameter = upperBoundsQuadMeanDiameter(basalAreaGroup);

		final float ageUse = breastHeightAge;

		if (ageUse <= 0f) {
			throw new StandProcessingException("Primary breast height age must be positive but was " + ageUse);
		}

		final float trAge = FloatMath.log(ageUse);

		final float c0 = coefficients.getCoe(0);
		final float c1 = Math.max(coefficients.getCoe(1) + coefficients.getCoe(2) * trAge, 0f);
		final float c2 = Math.max(coefficients.getCoe(3) + coefficients.getCoe(4) * trAge, 0f);

		float dq = c0 + c1 * FloatMath.pow(dominantHeight - 5f, c2);

		return FloatMath.clamp(dq, 7.6f, upperBoundsQuadMeanDiameter);
	}

	/**
	 * @param basalAreaGroup the basal area group in question
	 * @return the quad-mean-diameter upper bound for the given basal area group 
	 */
	float upperBoundsQuadMeanDiameter(int basalAreaGroup) {
		return upperBounds(basalAreaGroup).getCoe(2);
	}

	/**
	 * @param basalAreaGroup the basal area group in question
	 * @return the basal area upper bound for the given basal area group 
	 */
	float upperBoundsBaseArea(int basalAreaGroup) {
		return upperBounds(basalAreaGroup).getCoe(1);
	}

	/**
	 * UPPERGEN Method 1 - return the upper bounds of basal area and quad-mean-diameter for a given 
	 * basal area group as a list of (two) floats.
	 * @param basalAreaGroup the basal area group in question
	 * @return as described
	 */
	Coefficients upperBounds(int basalAreaGroup) {
		var upperBoundsMap = controlMap.<Map<Integer, Coefficients>>get(ControlKey.BA_DQ_UPPER_BOUNDS, Map.class);
		return Utils.<Coefficients>optSafe(upperBoundsMap.get(basalAreaGroup)).orElseThrow(
				() -> new IllegalStateException("Could not find limits for basal area group " + basalAreaGroup)
		);
	}

	Coefficients sumCoefficientsWeightedBySpeciesAndDecayBec(
			Collection<? extends BaseVdypSpecies<? extends BaseVdypSite>> species, BecDefinition bec, ControlKey key,
			int size
	) {
		var coeMap = controlMap.<MatrixMap2<String, String, Coefficients>>get(key, MatrixMap2.class);

		final String decayBecAlias = bec.getDecayBec().getAlias();

		return weightedCoefficientSum(
				size, 0, //
				species, //
				BaseVdypSpecies::getFractionGenus, // Weight by fraction
				spec -> coeMap.get(decayBecAlias, spec.getGenus())
		);
	}

	/**
	 * Create a coefficients object where its values are either a weighted sum of those for each of the given entities,
	 * or the value from one arbitrarily chose entity.
	 *
	 * @param <T>             The type of entity
	 * @param size            Size of the resulting coefficients object
	 * @param indexFrom       index from of the resulting coefficients object
	 * @param entities        the entities to do weighted sums over
	 * @param weight          the weight for a given entity
	 * @param getCoefficients the coefficients for a given entity
	 */
	public static <T> Coefficients weightedCoefficientSum(
			int size, int indexFrom, Collection<T> entities, ToDoubleFunction<T> weight,
			Function<T, Coefficients> getCoefficients
	) {
		var weighted = IntStream.range(indexFrom, size + indexFrom).boxed().toList();
		return weightedCoefficientSum(weighted, size, indexFrom, entities, weight, getCoefficients);
	}

	/**
	 * Create a coefficients object where its values are either a weighted sum of those for each of the given entities,
	 * or the value from one arbitrarily chose entity.
	 *
	 * @param <T>             The type of entity
	 * @param weighted        the indicies of the coefficients that should be weighted sums, those that are not included
	 *                        are assumed to be constant across all entities and one is choses arbitrarily.
	 * @param size            Size of the resulting coefficients object
	 * @param indexFrom       index from of the resulting coefficients object
	 * @param entities        the entities to do weighted sums over
	 * @param weight          the weight for a given entity
	 * @param getCoefficients the coefficients for a given entity
	 */
	public static <T> Coefficients weightedCoefficientSum(
			Collection<Integer> weighted, int size, int indexFrom, Collection<T> entities, ToDoubleFunction<T> weight,
			Function<T, Coefficients> getCoefficients
	) {
		Coefficients coe = Coefficients.empty(size, indexFrom);

		// Do the summation in double precision
		var coeWorking = new double[size];
		Arrays.fill(coeWorking, 0.0);

		for (var entity : entities) {
			var entityCoe = getCoefficients.apply(entity);
			double fraction = weight.applyAsDouble(entity);
			log.atInfo().addArgument(entity).addArgument(fraction).addArgument(entityCoe)
					.setMessage("For entity {} with fraction {} adding coefficients {}").log();
			for (int i : weighted) {
				coeWorking[i - indexFrom] += (entityCoe.getCoe(i)) * fraction;
			}
		}
		// Reduce back to float once done
		for (int i : weighted) {
			coe.setCoe(i, (float) coeWorking[i - indexFrom]);
		}

		// Pick one entity to fill in the fixed coefficients
		// Choice is arbitrary, they should all be the same
		var anyCoe = getCoefficients.apply(entities.iterator().next());

		for (int i = indexFrom; i < size + indexFrom; i++) {
			if (weighted.contains(i))
				continue;
			coe.setCoe(i, anyCoe.getCoe(i));
		}
		return coe;
	}

	/**
	 * EMP090. Return an estimate of the volume, per tree, of the whole stem, based on the given lorey height and quad
	 * mean diameter.
	 *
	 * @param controlMap       the control map from which the total stand whole stem volume equation coefficients are
	 *                         retrieved
	 * @param volumeGroup      the species' volume group
	 * @param loreyHeight      the species' lorey height
	 * @param quadMeanDiameter the species' quadratic mean diameter
	 * @return as described
	 */
	public static float estimateWholeStemVolumePerTree(
			int volumeGroup, float loreyHeight, float quadMeanDiameter,
			Map<Integer, Coefficients> totalStandWholeStemVolumeCoeMap
	) {
		var coe = totalStandWholeStemVolumeCoeMap.get(volumeGroup).reindex(0);

		var logMeanVolume = //
				coe.getCoe(UtilizationClass.ALL.index) + //
						coe.getCoe(1) * log(quadMeanDiameter) + //
						coe.getCoe(2) * log(loreyHeight) + //
						coe.getCoe(3) * quadMeanDiameter + //
						coe.getCoe(4) / quadMeanDiameter + //
						coe.getCoe(5) * loreyHeight + //
						coe.getCoe(6) * quadMeanDiameter * quadMeanDiameter + //
						coe.getCoe(7) * loreyHeight * quadMeanDiameter + //
						coe.getCoe(8) * loreyHeight / quadMeanDiameter;

		return exp(logMeanVolume);
	}

	/**
	 * EMP091. Updates wholeStemVolumeUtil with estimated values, getting wholeStemUtilizationComponentMap from the
	 * given controlMap.
	 *
	 * @param utilizationClass
	 * @param adjustCloseUtil
	 * @param volumeGroup
	 * @param hlSp
	 * @param quadMeanDiameterUtil
	 * @param baseAreaUtil
	 * @param wholeStemVolumeUtil
	 * @throws ProcessingException
	 */
	public void estimateWholeStemVolume(
			UtilizationClass utilizationClass, float adjustCloseUtil, int volumeGroup, Float hlSp,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector baseAreaUtil,
			UtilizationVector wholeStemVolumeUtil
	) throws ProcessingException {

		final var wholeStemUtilizationComponentMap = controlMap
				.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>get(
						ControlKey.UTIL_COMP_WS_VOLUME, MatrixMap2.class
				);

		estimateWholeStemVolume(
				utilizationClass, adjustCloseUtil, volumeGroup, hlSp, wholeStemUtilizationComponentMap, quadMeanDiameterUtil, baseAreaUtil, wholeStemVolumeUtil
		);
	}

	/**
	 * EMP091. Updates wholeStemVolumeUtil with estimated values.
	 *
	 * @param utilizationClass
	 * @param adjustCloseUtil
	 * @param volumeGroup
	 * @param hlSp
	 * @param wholeStemUtilizationComponentMap
	 * @param quadMeanDiameterUtil
	 * @param baseAreaUtil
	 * @param wholeStemVolumeUtil
	 * @throws ProcessingException
	 */
	public static void estimateWholeStemVolume(
			UtilizationClass utilizationClass, float adjustCloseUtil, int volumeGroup, Float hlSp,
			MatrixMap2<Integer, Integer, Optional<Coefficients>> wholeStemUtilizationComponentMap,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector baseAreaUtil,
			UtilizationVector wholeStemVolumeUtil
	) throws ProcessingException {
		var dqSp = quadMeanDiameterUtil.getAll();

		estimateUtilization(baseAreaUtil, wholeStemVolumeUtil, utilizationClass, (uc, ba) -> {
			Coefficients wholeStemCoe = wholeStemUtilizationComponentMap.get(uc.index, volumeGroup).orElseThrow(
					() -> new ProcessingException(
							"Could not find whole stem utilization coefficients for group " + volumeGroup
					)
			);

			// Fortran code uses 1 index into array when reading it here, but 0 index when
			// writing into it in the parser. I use 0 for both.
			var a0 = wholeStemCoe.getCoe(0);
			var a1 = wholeStemCoe.getCoe(1);
			var a2 = wholeStemCoe.getCoe(2);
			var a3 = wholeStemCoe.getCoe(3);

			var arg = a0 + a1 * log(hlSp) + a2 * log(quadMeanDiameterUtil.getCoe(uc.index))
					+ ( (uc != UtilizationClass.OVER225) ? a3 * log(dqSp) : a3 * dqSp);

			if (uc == utilizationClass) {
				arg += adjustCloseUtil;
			}

			var vbaruc = exp(arg); // volume base area ?? utilization class?

			return ba * vbaruc;
		}, x -> x <= 0f, 0f);

		if (utilizationClass == UtilizationClass.ALL) {
			normalizeUtilizationComponents(wholeStemVolumeUtil);
		}
	}

	/**
	 * EMP092. Updates closeUtilizationVolumeUtil with estimated values.
	 *
	 * @param utilizationClass
	 * @param aAdjust
	 * @param volumeGroup
	 * @param hlSp
	 * @param quadMeanDiameterUtil
	 * @param wholeStemVolumeUtil
	 * @param closeUtilizationVolumeUtil
	 * @throws ProcessingException
	 */
	public void estimateCloseUtilizationVolume(
			UtilizationClass utilizationClass, Coefficients aAdjust, int volumeGroup, float hlSp,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector wholeStemVolumeUtil,
			UtilizationVector closeUtilizationVolumeUtil
	) throws ProcessingException {

		final var closeUtilizationCoeMap = controlMap.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>get(
				ControlKey.CLOSE_UTIL_VOLUME, MatrixMap2.class
		);
		estimateCloseUtilizationVolume(
				utilizationClass, aAdjust, volumeGroup, hlSp, closeUtilizationCoeMap, quadMeanDiameterUtil, wholeStemVolumeUtil, closeUtilizationVolumeUtil
		);
	}

	/**
	 * EMP092. Updates closeUtilizationVolumeUtil with estimated values.
	 *
	 * @param utilizationClass
	 * @param aAdjust
	 * @param volumeGroup
	 * @param hlSp
	 * @param closeUtilizationCoeMap
	 * @param quadMeanDiameterUtil
	 * @param wholeStemVolumeUtil
	 * @param closeUtilizationVolumeUtil
	 * @throws ProcessingException
	 */
	public static void estimateCloseUtilizationVolume(
			UtilizationClass utilizationClass, Coefficients aAdjust, int volumeGroup, float hlSp,
			MatrixMap2<Integer, Integer, Optional<Coefficients>> closeUtilizationCoeMap,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector wholeStemVolumeUtil,
			UtilizationVector closeUtilizationVolumeUtil
	) throws ProcessingException {
		estimateUtilization(wholeStemVolumeUtil, closeUtilizationVolumeUtil, utilizationClass, (uc, ws) -> {
			Coefficients closeUtilCoe = closeUtilizationCoeMap.get(uc.index, volumeGroup).orElseThrow(
					() -> new ProcessingException(
							"Could not find whole stem utilization coefficients for group " + volumeGroup
					)
			);
			var a0 = closeUtilCoe.getCoe(1);
			var a1 = closeUtilCoe.getCoe(2);
			var a2 = closeUtilCoe.getCoe(3);

			var arg = a0 + a1 * quadMeanDiameterUtil.getCoe(uc.index) + a2 * hlSp + aAdjust.getCoe(uc.index);

			float ratio = ratio(arg, 7.0f);

			return ws * ratio;
		});

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationVolumeUtil);
		}
	}

	/**
	 * EMP093. Estimate volume NET OF DECAY by (DBH) utilization classes
	 *
	 * @param genus
	 * @param region
	 * @param utilizationClass
	 * @param aAdjust
	 * @param decayGroup
	 * @param ageBreastHeight
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @throws ProcessingException
	 */
	public void estimateNetDecayVolume(
			String genus, Region region, UtilizationClass utilizationClass, Coefficients aAdjust, int decayGroup,
			float ageBreastHeight, UtilizationVector quadMeanDiameterUtil, UtilizationVector closeUtilizationUtil,
			UtilizationVector closeUtilizationNetOfDecayUtil
	) throws ProcessingException {

		final var netDecayCoeMap = controlMap.<MatrixMap2<Integer, Integer, Optional<Coefficients>>>get(
				ControlKey.VOLUME_NET_DECAY, MatrixMap2.class
		);
		final var decayModifierMap = controlMap
				.<MatrixMap2<String, Region, Float>>get(ModifierParser.CONTROL_KEY_MOD301_DECAY, MatrixMap2.class);
		estimateNetDecayVolume(
				genus, region, utilizationClass, aAdjust, decayGroup, ageBreastHeight, netDecayCoeMap, decayModifierMap, quadMeanDiameterUtil, closeUtilizationUtil, closeUtilizationNetOfDecayUtil
		);
	}

	/**
	 * EMP093. Estimate volume NET OF DECAY by (DBH) utilization classes
	 *
	 * @param genus
	 * @param region
	 * @param utilizationClass
	 * @param aAdjust
	 * @param decayGroup
	 * @param ageBreastHeight
	 * @param netDecayCoeMap
	 * @param decayModifierMap
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @param lore                           // EMP053 Using eqns N1 and N2 from ipsjf124.doc yHeight
	 * @throws ProcessingException
	 */
	public static void estimateNetDecayVolume(
			String genus, Region region, UtilizationClass utilizationClass, Coefficients aAdjust, int decayGroup,
			float ageBreastHeight, MatrixMap2<Integer, Integer, Optional<Coefficients>> netDecayCoeMap,
			MatrixMap2<String, Region, Float> decayModifierMap, UtilizationVector quadMeanDiameterUtil,
			UtilizationVector closeUtilizationUtil, UtilizationVector closeUtilizationNetOfDecayUtil
	) throws ProcessingException {
		var dqSp = quadMeanDiameterUtil.getAll();

		final var ageTr = (float) Math.log(Math.max(20.0, ageBreastHeight));

		estimateUtilization(closeUtilizationUtil, closeUtilizationNetOfDecayUtil, utilizationClass, (uc, cu) -> {
			Coefficients netDecayCoe = netDecayCoeMap.get(uc.index, decayGroup).orElseThrow(
					() -> new ProcessingException("Could not find net decay coefficients for group " + decayGroup)
			);
			var a0 = netDecayCoe.getCoe(1);
			var a1 = netDecayCoe.getCoe(2);
			var a2 = netDecayCoe.getCoe(3);

			float arg;
			if (uc != UtilizationClass.OVER225) {
				arg = a0 + a1 * log(dqSp) + a2 * ageTr;
			} else {
				arg = a0 + a1 * log(quadMeanDiameterUtil.getCoe(uc.index)) + a2 * ageTr;
			}

			arg += aAdjust.getCoe(uc.index) + decayModifierMap.get(genus, region);

			float ratio = ratio(arg, 8.0f);

			return cu * ratio;
		});

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayUtil);
		}
	}

	/**
	 * EMP094. Estimate utilization net of decay and waste
	 *
	 * @param region
	 * @param utilizationClass
	 * @param aAdjust
	 * @param genus
	 * @param loreyHeight
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @param closeUtilizationNetOfDecayAndWasteUtil
	 * @throws ProcessingException
	 */
	public void estimateNetDecayAndWasteVolume(
			Region region, UtilizationClass utilizationClass, Coefficients aAdjust, String genus, float loreyHeight,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector closeUtilizationUtil,
			UtilizationVector closeUtilizationNetOfDecayUtil, UtilizationVector closeUtilizationNetOfDecayAndWasteUtil
	) throws ProcessingException {

		final var netDecayWasteCoeMap = controlMap
				.<Map<String, Coefficients>>get(ControlKey.VOLUME_NET_DECAY_WASTE, Map.class);
		final var wasteModifierMap = controlMap
				.<MatrixMap2<String, Region, Float>>get(ControlKey.WASTE_MODIFIERS, MatrixMap2.class);

		estimateNetDecayAndWasteVolume(
				region, utilizationClass, aAdjust, genus, loreyHeight, netDecayWasteCoeMap, wasteModifierMap, quadMeanDiameterUtil, closeUtilizationUtil, closeUtilizationNetOfDecayUtil, closeUtilizationNetOfDecayAndWasteUtil
		);
	}

	/**
	 * EMP094. Estimate utilization net of decay and waste
	 *
	 * @param region
	 * @param utilizationClass
	 * @param aAdjust
	 * @param genus
	 * @param loreyHeight
	 * @param netDecayWasteCoeMap
	 * @param wasteModifierMap
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayUtil
	 * @param closeUtilizationNetOfDecayAndWasteUtil
	 * @throws ProcessingException
	 */
	public static void estimateNetDecayAndWasteVolume(
			Region region, UtilizationClass utilizationClass, Coefficients aAdjust, String genus, float loreyHeight,
			Map<String, Coefficients> netDecayWasteCoeMap, MatrixMap2<String, Region, Float> wasteModifierMap,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector closeUtilizationUtil,
			UtilizationVector closeUtilizationNetOfDecayUtil, UtilizationVector closeUtilizationNetOfDecayAndWasteUtil
	) throws ProcessingException {
		estimateUtilization(
				closeUtilizationNetOfDecayUtil, closeUtilizationNetOfDecayAndWasteUtil, utilizationClass, (
						i, netDecay
				) -> {
					if (Float.isNaN(netDecay) || netDecay <= 0f) {
						return 0f;
					}

					Coefficients netWasteCoe = netDecayWasteCoeMap.get(genus);
					if (netWasteCoe == null) {
						throw new ProcessingException("Could not find net waste coefficients for genus " + genus);
					}

					var a0 = netWasteCoe.getCoe(0);
					var a1 = netWasteCoe.getCoe(1);
					var a2 = netWasteCoe.getCoe(2);
					var a3 = netWasteCoe.getCoe(3);
					var a4 = netWasteCoe.getCoe(4);
					var a5 = netWasteCoe.getCoe(5);

					if (i == UtilizationClass.OVER225) {
						a0 += a5;
					}
					var frd = 1.0f - netDecay / closeUtilizationUtil.getCoe(i.index);

					float arg = a0 + a1 * frd + a3 * log(quadMeanDiameterUtil.getCoe(i.index)) + a4 * log(loreyHeight);

					arg += wasteModifierMap.get(genus, region);

					arg = clamp(arg, -10f, 10f);

					var frw = (1.0f - exp(a2 * frd)) * exp(arg) / (1f + exp(arg)) * (1f - frd);
					frw = min(frd, frw);

					float result = closeUtilizationUtil.getCoe(i.index) * (1f - frd - frw);

					/*
					 * Check for an apply adjustments. This is done after computing the result above to allow for
					 * clamping frw to frd
					 */
					if (aAdjust.getCoe(i.index) != 0f) {
						var ratio = result / netDecay;
						if (ratio < 1f && ratio > 0f) {
							arg = log(ratio / (1f - ratio));
							arg += aAdjust.getCoe(i.index);
							arg = clamp(arg, -10f, 10f);
							result = exp(arg) / (1f + exp(arg)) * netDecay;
						}
					}

					return result;
				}
		);

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayAndWasteUtil);
		}
	}

	/**
	 * EMP095. Estimate utilization net of decay, waste, and breakage
	 *
	 * @param controlMap
	 * @param utilizationClass
	 * @param breakageGroup
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayAndWasteUtil
	 * @param closeUtilizationNetOfDecayWasteAndBreakageUtil
	 * @throws ProcessingException
	 */
	public void estimateNetDecayWasteAndBreakageVolume(
			UtilizationClass utilizationClass, int breakageGroup, UtilizationVector quadMeanDiameterUtil,
			UtilizationVector closeUtilizationUtil, UtilizationVector closeUtilizationNetOfDecayAndWasteUtil,
			UtilizationVector closeUtilizationNetOfDecayWasteAndBreakageUtil
	) throws ProcessingException {

		final var netBreakageCoeMap = controlMap.<Map<Integer, Coefficients>>get(ControlKey.BREAKAGE, Map.class);

		estimateNetDecayWasteAndBreakageVolume(
				utilizationClass, breakageGroup, netBreakageCoeMap, quadMeanDiameterUtil, closeUtilizationUtil, closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil
		);
	}

	/**
	 * EMP095. Estimate utilization net of decay, waste, and breakage
	 *
	 * @param controlMap
	 * @param utilizationClass
	 * @param breakageGroup
	 * @param quadMeanDiameterUtil
	 * @param closeUtilizationUtil
	 * @param closeUtilizationNetOfDecayAndWasteUtil
	 * @param closeUtilizationNetOfDecayWasteAndBreakageUtil
	 * @throws ProcessingException
	 */
	public static void estimateNetDecayWasteAndBreakageVolume(
			UtilizationClass utilizationClass, int breakageGroup, Map<Integer, Coefficients> netBreakageCoeMap,
			UtilizationVector quadMeanDiameterUtil, UtilizationVector closeUtilizationUtil,
			UtilizationVector closeUtilizationNetOfDecayAndWasteUtil,
			UtilizationVector closeUtilizationNetOfDecayWasteAndBreakageUtil
	) throws ProcessingException {
		final var coefficients = netBreakageCoeMap.get(breakageGroup);
		if (coefficients == null) {
			throw new ProcessingException("Could not find net breakage coefficients for group " + breakageGroup);
		}

		final var a1 = coefficients.getCoe(1);
		final var a2 = coefficients.getCoe(2);
		final var a3 = coefficients.getCoe(3);
		final var a4 = coefficients.getCoe(4);

		estimateUtilization(
				closeUtilizationNetOfDecayAndWasteUtil, closeUtilizationNetOfDecayWasteAndBreakageUtil, utilizationClass, (
						uc, netWaste
				) -> {

					if (netWaste <= 0f) {
						return 0f;
					}
					var percentBroken = a1 + a2 * log(quadMeanDiameterUtil.getCoe(uc.index));
					percentBroken = clamp(percentBroken, a3, a4);
					var broken = min(percentBroken / 100 * closeUtilizationUtil.getCoe(uc.index), netWaste);
					return netWaste - broken;
				}
		);

		if (utilizationClass == UtilizationClass.ALL) {
			storeSumUtilizationComponents(closeUtilizationNetOfDecayWasteAndBreakageUtil);
		}
	}

	@FunctionalInterface
	public static interface UtilizationProcessor {
		float apply(UtilizationClass utilizationClass, float inputValue) throws ProcessingException;
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the computation, UtilizationClass.ALL for all of
	 *                         them.
	 * @param processor        Given a utilization class, and the source utilization for that class, return the result
	 *                         utilization
	 * @param skip             a utilization class will be skipped and the result set to the default value if this is
	 *                         true for the value of the source utilization
	 * @param defaultValue     the default value
	 * @throws ProcessingException
	 */
	private static void estimateUtilization(
			UtilizationVector input, UtilizationVector output, UtilizationClass utilizationClass,
			UtilizationProcessor processor, Predicate<Float> skip, float defaultValue
	) throws ProcessingException {
		for (var uc : UtilizationClass.UTIL_CLASSES) {
			var inputValue = input.getCoe(uc.index);

			// it seems like this should be done after checking i against utilizationClass,
			// which could just be done as part of the processor definition, but this is how
			// VDYP7 did it.
			if (skip.test(inputValue)) {
				output.setCoe(uc.index, defaultValue);
				continue;
			}

			if (utilizationClass != UtilizationClass.ALL && utilizationClass != uc) {
				continue;
			}

			var result = processor.apply(uc, inputValue);
			output.setCoe(uc.index, result);
		}
	}

	/**
	 * Estimate values for one utilization vector from another
	 *
	 * @param input            source utilization
	 * @param output           result utilization
	 * @param utilizationClass the utilization class for which to do the computation, UtilizationClass.ALL for all of
	 *                         them.
	 * @param processor        Given a utilization class, and the source utilization for that class, return the result
	 *                         utilization
	 * @throws ProcessingException
	 */
	private static void estimateUtilization(
			UtilizationVector input, UtilizationVector output, UtilizationClass utilizationClass,
			UtilizationProcessor processor
	) throws ProcessingException {
		estimateUtilization(input, output, utilizationClass, processor, x -> false, 0f);
	}

	private static float exponentRatio(float logit) throws ProcessingException {
		float exp = safeExponent(logit);
		return exp / (1f + exp);
	}

	private static float safeExponent(float logit) throws ProcessingException {
		if (logit > 88f) {
			throw new ProcessingException("logit " + logit + " exceeds 88");
		}
		return exp(logit);
	}

	/**
	 * Normalizes the utilization components 1-4 so they sum to the value of component UtilizationClass.ALL
	 *
	 * @throws ProcessingException if the sum is not positive
	 */
	private static float normalizeUtilizationComponents(UtilizationVector components) throws ProcessingException {
		var sum = sumUtilizationComponents(components);
		var k = components.getAll() / sum;
		if (sum <= 0f) {
			throw new ProcessingException("Total volume " + sum + " was not positive.");
		}
		UtilizationClass.UTIL_CLASSES.forEach(uc -> components.setCoe(uc.index, components.getCoe(uc.index) * k));
		return k;
	}

	/**
	 * Sums the individual utilization components (1-4)
	 */
	private static float sumUtilizationComponents(UtilizationVector components) {
		return (float) UtilizationClass.UTIL_CLASSES.stream().mapToInt(x -> x.index).mapToDouble(components::getCoe)
				.sum();
	}

	/**
	 * Sums the individual utilization components (1-4) and stores the results in coefficient UtilizationClass.ALL
	 */
	private static float storeSumUtilizationComponents(UtilizationVector components) {
		var sum = sumUtilizationComponents(components);
		components.setCoe(UtilizationClass.ALL.index, sum);
		return sum;
	}
}
