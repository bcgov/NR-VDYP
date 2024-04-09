package ca.bc.gov.nrs.vdyp.vri;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.RuntimeStandProcessingException;
import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.application.VdypStartApplication;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.PolygonMode;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies.Builder;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.vri.model.VriLayer;
import ca.bc.gov.nrs.vdyp.vri.model.VriPolygon;
import ca.bc.gov.nrs.vdyp.vri.model.VriSite;
import ca.bc.gov.nrs.vdyp.vri.model.VriSpecies;

public class VriStart extends VdypStartApplication<VriPolygon, VriLayer, VriSpecies, VriSite> implements Closeable {

	static final Logger log = LoggerFactory.getLogger(VriStart.class);

	static final float EMPOC = 0.85f;

	public static void main(final String... args) throws IOException {

		try (var app = new VriStart();) {

			doMain(app, args);
		}
	}

	// VRI_SUB
	// TODO Fortran takes a vector of flags (FIPPASS) controlling which stages are
	// implemented. FIPSTART always uses the same vector so far now that's not
	// implemented.
	@Override
	public void process() throws ProcessingException {
		int polygonsRead = 0;
		int polygonsWritten = 0;
		try (
				var polyStream = this.<VriPolygon>getStreamingParser(ControlKey.VRI_INPUT_YIELD_POLY);
				var layerStream = this.<Map<LayerType, VriLayer.Builder>>getStreamingParser(
						ControlKey.VRI_INPUT_YIELD_LAYER
				);
				var speciesStream = this
						.<Collection<VriSpecies>>getStreamingParser(ControlKey.VRI_INPUT_YIELD_SPEC_DIST);
				var siteStream = this.<Collection<VriSite>>getStreamingParser(ControlKey.VRI_INPUT_YIELD_HEIGHT_AGE_SI);
		) {
			log.atDebug().setMessage("Start Stand processing").log();

			while (polyStream.hasNext()) {

				// FIP_GET
				log.atInfo().setMessage("Getting polygon {}").addArgument(polygonsRead + 1).log();
				var polygon = getPolygon(polyStream, layerStream, speciesStream, siteStream);
				try {

					var resultPoly = processPolygon(polygonsRead, polygon);
					if (resultPoly.isPresent()) {
						polygonsRead++;

						// Output
						vriWriter.writePolygonWithSpeciesAndUtilization(resultPoly.get());

						polygonsWritten++;
					}

					log.atInfo().setMessage("Read {} polygons and wrote {}").addArgument(polygonsRead)
							.addArgument(polygonsWritten);

				} catch (StandProcessingException ex) {
					// TODO include some sort of hook for different forms of user output
					// TODO Implement single stand mode that propagates the exception

					log.atWarn().setMessage("Polygon {} bypassed").addArgument(polygon.getPolygonIdentifier())
							.setCause(ex);
				}

			}
		} catch (IOException | ResourceParseException ex) {
			throw new ProcessingException("Error while reading or writing data.", ex);
		}
	}

	VriPolygon getPolygon(
			StreamingParser<VriPolygon> polyStream, StreamingParser<Map<LayerType, VriLayer.Builder>> layerStream,
			StreamingParser<Collection<VriSpecies>> speciesStream, StreamingParser<Collection<VriSite>> siteStream
	) throws StandProcessingException, IOException, ResourceParseException {

		var becLookup = Utils.expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class);

		log.trace("Getting polygon");
		var polygon = polyStream.next();

		BecDefinition bec = becLookup.get(polygon.getBiogeoclimaticZone())
				.orElseThrow(() -> new StandProcessingException("Unknown BEC " + polygon.getBiogeoclimaticZone()));

		log.trace("Getting species for polygon {}", polygon.getPolygonIdentifier());
		Collection<VriSpecies> species;
		try {
			species = speciesStream.next();
		} catch (NoSuchElementException ex) {
			throw validationError("Species file has fewer records than polygon file.", ex);
		}

		Map<LayerType, VriLayer.Builder> layersBuilders = layerStream.next();

		for (var spec : species) {
			var layerBuilder = layersBuilders.get(spec.getLayer());
			// Validate that species belong to the correct polygon
			if (!spec.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in species file contains species for polygon %s when expecting one for %s.",
						spec.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			if (Objects.isNull(layerBuilder)) {
				throw validationError(
						"Species entry references layer %s of polygon %s but it is not present.", spec.getLayer(),
						polygon.getPolygonIdentifier()
				);
			}
			layerBuilder.addSpecies(spec);
		}

		log.trace("Getting sites for polygon {}", polygon.getPolygonIdentifier());
		Collection<VriSite> sites;
		try {
			sites = siteStream.next();
		} catch (NoSuchElementException ex) {
			throw validationError("Sites file has fewer records than polygon file.", ex);
		}

		for (var site : sites) {
			var layerBuilder = layersBuilders.get(site.getLayer());
			// Validate that species belong to the correct polygon
			if (!site.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in site file contains site for polygon %s when expecting one for %s.",
						site.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			if (Objects.isNull(layerBuilder)) {
				throw validationError(
						"Site entry references layer %s of polygon %s but it is not present.", site.getLayer(),
						polygon.getPolygonIdentifier()
				);
			}
			layerBuilder.addSite(site);
		}

		Map<LayerType, VriLayer> layers = getLayersForPolygon(polygon, bec, layersBuilders);

		// Validate that layers belong to the correct polygon
		for (var layer : layers.values()) {
			if (!layer.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in layer file contains layer for polygon %s when expecting one for %s.",
						layer.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			layer.setSpecies(new HashMap<>());
		}

		polygon.setLayers(layers);

		return polygon;

	}

	private Map<LayerType, VriLayer>
			getLayersForPolygon(VriPolygon polygon, BecDefinition bec, Map<LayerType, VriLayer.Builder> layersBuilders)
					throws StandProcessingException {
		log.trace("Getting layers for polygon {}", polygon.getPolygonIdentifier());
		Map<LayerType, VriLayer> layers;
		try {

			// Do some additional processing then build the layers.
			layers = layersBuilders.values().stream().map(builder -> {

				var layerType = builder.getLayerType().get();

				builder.buildChildren(); // Make sure all children are built before getting them.
				var layerSpecies = builder.getSpecies();

				if (layerType == LayerType.PRIMARY) {
					builder.percentAvailable(polygon.getPercentAvailable().orElse(1f));
				}
				if (!layerSpecies.isEmpty()) {
					var primarySpecs = this.findPrimarySpecies(layerSpecies);
					int itg;
					try {
						itg = findItg(primarySpecs);

						builder.inventoryTypeGroup(itg);
					} catch (StandProcessingException ex) {
						throw new RuntimeStandProcessingException(ex);
					}
					builder.primaryGenus(primarySpecs.get(0).getGenus());

					if (layerType == LayerType.PRIMARY) {

						// This was being done in VRI_CHK but I moved it here to when the object is
						// being built instead.
						if (builder.getBaseArea().flatMap(
								ba -> builder.getTreesPerHectare()
										.map(tph -> BaseAreaTreeDensityDiameter.quadMeanDiameter(ba, tph) < 7.5f)
						).orElse(false)) {
							builder.baseArea(Optional.empty());
							builder.treesPerHectare(Optional.empty());
						}

						if (primarySpecs.size() > 1) {
							builder.secondaryGenus(primarySpecs.get(1).getGenus());
						}

						builder.empiricalRelationshipParameterIndex(
								findEmpiricalRelationshipParameterIndex(primarySpecs.get(0).getGenus(), bec, itg)
						);
					}
				}
				if (layerType == LayerType.VETERAN && (builder.getBaseArea().map(x -> x <= 0f).orElse(true)
						|| builder.getTreesPerHectare().map(x -> x <= 0f).orElse(true))) {
					// BA or TPH missing from Veteran layer.

					builder.treesPerHectare(0f);

					float crownClosure = builder.getCrownClosure().filter(x -> x > 0f).orElseThrow(
							() -> new RuntimeStandProcessingException(
									validationError(
											"Expected a positive crown closure for veteran layer but was %s",
											Utils.optNa(builder.getCrownClosure())
									)
							)
					);
					// If the primary layer base area is positive, multiply that by veteran crown
					// closure, otherwise just use half the veteran crown closure.
					builder.baseArea(
							layersBuilders.get(LayerType.PRIMARY).getBaseArea().filter(x -> x > 0f)
									.map(pba -> crownClosure / 100f * pba).orElse(crownClosure / 2f)
					);

				}
				return builder;
			}).map(VriLayer.Builder::build).collect(Collectors.toUnmodifiableMap(VriLayer::getLayer, x -> x));

		} catch (NoSuchElementException ex) {
			throw validationError("Layers file has fewer records than polygon file.", ex);
		} catch (RuntimeStandProcessingException ex) {
			throw ex.getCause();
		}
		return layers;
	}

	static final EnumSet<PolygonMode> ACCEPTABLE_MODES = EnumSet.of(PolygonMode.START, PolygonMode.YOUNG);

	Optional<VdypPolygon> processPolygon(int polygonsRead, VriPolygon polygon) throws ProcessingException {
		VdypPolygon resultPoly;
		log.atInfo().setMessage("Read polygon {}, preparing to process").addArgument(polygon.getPolygonIdentifier())
				.log();

		// if (MODE .eq. -1) go to 100

		final var mode = polygon.getModeFip().orElse(PolygonMode.START);

		if (mode != PolygonMode.DONT_PROCESS) {
			log.atInfo().setMessage("Skipping polygon with mode {}").addArgument(mode).log();
			return Optional.empty();
		}

		// IP_IN = IP_IN+1
		// if (IP_IN .gt. MAXPOLY) go to 200

		// IPASS = 1
		// CALL FIP_CHK( IPASS, IER)
		// if (ier .gt. 0) go to 1000
		//
		// if (IPASS .le. 0) GO TO 120

		log.atInfo().setMessage("Checking validity of polygon {}:{}").addArgument(polygonsRead)
				.addArgument(polygon.getPolygonIdentifier()).log();
		checkPolygon(polygon);

		// TODO
		return Optional.empty();

	}

	// VRI_CHK
	void checkPolygon(VriPolygon polygon) throws ProcessingException {

		BecDefinition bec = Utils.getBec(polygon.getBiogeoclimaticZone(), controlMap);

		var primaryLayer = requireLayer(polygon, LayerType.PRIMARY);

		// At this point the Fortran implementation nulled the BA and TPH of Primary
		// layers if the BA and TPH were present and resulted in a DQ <7.5
		// I did that in getPolygon instead of here.

		for (var layer : polygon.getLayers().values()) {
			checkLayer(polygon, layer);
		}

		checkPolygonForMode(polygon, bec);

		VriLayer veteranLayer = polygon.getLayers().get(LayerType.VETERAN);

		Map<String, Float> minMap = Utils.expectParsedControl(controlMap, ControlKey.MINIMA, Map.class);

		float veteranMinHeight = minMap.get(VriControlParser.MINIMUM_VETERAN_HEIGHT);

		if (veteranLayer != null) {
			Optional<Float> veteranHeight = veteranLayer.getPrimarySite().flatMap(VriSite::getHeight);
			validateMinimum("Veteran layer primary species height", veteranHeight, veteranMinHeight, true);
		}
	}

	private void checkLayer(VriPolygon polygon, VriLayer layer) throws StandProcessingException {
		if (layer.getSpecies().isEmpty())
			return;
		if (layer.getLayer() == LayerType.PRIMARY)
			this.getPercentTotal(layer); // Validate that percent total is close to 100%
		Optional<VriSite> primarySite = layer.getPrimaryGenus().flatMap(id -> Utils.optSafe(layer.getSites().get(id)));
		var ageTotal = primarySite.flatMap(VriSite::getAgeTotal);
		var treesPerHectare = layer.getTreesPerHectare();
		var height = primarySite.flatMap(VriSite::getHeight);
		if (polygon.getModeFip().map(x -> x == PolygonMode.YOUNG).orElse(false)
				&& layer.getLayer() == LayerType.PRIMARY) {
			if (ageTotal.map(x -> x <= 0f).orElse(true) || treesPerHectare.map(x -> x <= 0f).orElse(true)) {
				throw validationError(
						"Age Total and Trees Per Hectare must be positive for a PRIMARY layer in mode YOUNG"
				);
			}
		} else {
			if (height.map(x -> x <= 0f).orElse(true)) {
				throw validationError(
						"Height must be positive for a VETERAN layer or a PRIMARY layer not in mode YOUNG"
				);
			}
		}
	}

	static final String SITE_INDEX_PROPERTY_NAME = "Site index";
	static final String AGE_TOTAL_PROPERTY_NAME = "Age total";
	static final String BREAST_HEIGHT_AGE_PROPERTY_NAME = "Breast height age";
	static final String YEARS_TO_BREAST_HEIGHT_PROPERTY_NAME = "Years to breast height";
	static final String HEIGHT_PROPERTY_NAME = "Height";
	static final String BASE_AREA_PROPERTY_NAME = "Base area";
	static final String TREES_PER_HECTARE_PROPERTY_NAME = "Trees per hectare";
	static final String CROWN_CLOSURE_PROPERTY_NAME = "Crown closure";

	protected void checkPolygonForMode(VriPolygon polygon, BecDefinition bec) throws StandProcessingException {
		VriLayer primaryLayer = polygon.getLayers().get(LayerType.PRIMARY);
		Optional<VriSite> primarySite = primaryLayer.getPrimaryGenus()
				.flatMap(id -> Utils.optSafe(primaryLayer.getSites().get(id)));
		var ageTotal = primarySite.flatMap(VriSite::getAgeTotal);
		var height = primarySite.flatMap(VriSite::getHeight);
		var siteIndex = primarySite.flatMap(VriSite::getSiteIndex);
		var yearsToBreastHeight = primarySite.flatMap(VriSite::getYearsToBreastHeight);
		var baseArea = primaryLayer.getBaseArea();
		var treesPerHectare = primaryLayer.getTreesPerHectare();
		var crownClosure = primaryLayer.getCrownClosure();
		var percentForest = polygon.getPercentAvailable();

		try {
			PolygonMode mode = polygon.getModeFip().orElseGet(() -> {
				try {
					return findDefaultPolygonMode(
							ageTotal, yearsToBreastHeight, height, baseArea, treesPerHectare, percentForest,
							primaryLayer.getSpecies().values(), bec,
							primaryLayer.getEmpericalRelationshipParameterIndex()
					);
				} catch (StandProcessingException e) {
					throw new RuntimeStandProcessingException(e);
				}
			});
			polygon.setModeFip(Optional.of(mode));
			Optional<Float> primaryBreastHeightAge = Utils.mapBoth(
					primaryLayer.getPrimarySite().flatMap(VriSite::getAgeTotal),
					primaryLayer.getPrimarySite().flatMap(VriSite::getYearsToBreastHeight), (at, ytbh) -> at - ytbh
			);
			log.atDebug().setMessage("Polygon mode {} checks").addArgument(mode);
			switch (mode) {

			case START:
				validateMinimum(SITE_INDEX_PROPERTY_NAME, siteIndex, 0f, false);
				validateMinimum(AGE_TOTAL_PROPERTY_NAME, ageTotal, 0f, false);
				validateMinimum(BREAST_HEIGHT_AGE_PROPERTY_NAME, primaryBreastHeightAge, 0f, false);
				validateMinimum(HEIGHT_PROPERTY_NAME, height, 4.5f, false);
				validateMinimum(BASE_AREA_PROPERTY_NAME, baseArea, 0f, false);
				validateMinimum(TREES_PER_HECTARE_PROPERTY_NAME, treesPerHectare, 0f, false);
				break;

			case YOUNG:
				validateMinimum(SITE_INDEX_PROPERTY_NAME, siteIndex, 0f, false);
				validateMinimum(AGE_TOTAL_PROPERTY_NAME, ageTotal, 0f, false);
				validateMinimum(YEARS_TO_BREAST_HEIGHT_PROPERTY_NAME, yearsToBreastHeight, 0f, false);
				break;

			case BATN:
				validateMinimum(SITE_INDEX_PROPERTY_NAME, siteIndex, 0f, false);
				validateMinimum(AGE_TOTAL_PROPERTY_NAME, ageTotal, 0f, false);
				validateMinimum(BREAST_HEIGHT_AGE_PROPERTY_NAME, primaryBreastHeightAge, 0f, false);
				validateMinimum(HEIGHT_PROPERTY_NAME, height, 1.3f, false);
				break;

			case BATC:
				validateMinimum(SITE_INDEX_PROPERTY_NAME, siteIndex, 0f, false);
				validateMinimum(AGE_TOTAL_PROPERTY_NAME, ageTotal, 0f, false);
				validateMinimum(BREAST_HEIGHT_AGE_PROPERTY_NAME, primaryBreastHeightAge, 0f, false);
				validateMinimum(HEIGHT_PROPERTY_NAME, height, 1.3f, false);
				validateMinimum(CROWN_CLOSURE_PROPERTY_NAME, crownClosure, 0f, false);
				break;

			case DONT_PROCESS:
				log.atDebug().setMessage("Skipping validation for ignored polygon");
				// Do Nothing
				break;
			}
		} catch (RuntimeStandProcessingException e) {
			throw e.getCause();
		}
	}

	void validateMinimum(String fieldName, float value, float minimum, boolean inclusive)
			throws StandProcessingException {
		if (value < minimum || (value == minimum && !inclusive))
			throw validationError(
					"%s %s should be %s %s", fieldName, value, inclusive ? "greater than or equal to" : "greater than",
					minimum
			);
	}

	void validateMinimum(String fieldName, Optional<Float> value, float minimum, boolean inclusive)
			throws StandProcessingException {
		validateMinimum(
				fieldName, value.orElseThrow(() -> validationError("%s is not present", fieldName)), minimum, inclusive
		);
	}

	// EMP106
	float estimateBaseAreaYield(
			float dominantHeight, float breastHeightAge, Optional<Float> baseAreaOverstory, boolean fullOccupancy,
			Collection<? extends BaseVdypSpecies> species, BecDefinition bec, int baseAreaGroup
	) throws StandProcessingException {
		var coe = estimateBaseAreaYieldCoefficients(species, bec);

		// UPPERGEN Method 1
		var upperBoundsMap = Utils
				.<Map<Integer, Coefficients>>expectParsedControl(controlMap, ControlKey.BA_DQ_UPPER_BOUNDS, Map.class);
		var upperBounds = Utils.<Coefficients>optSafe(upperBoundsMap.get(baseAreaGroup))
				.orElseThrow(() -> new IllegalStateException("Could not find base area group " + baseAreaGroup));

		float ageToUse = breastHeightAge;

		// TODO NDEBUG(2)

		if (ageToUse <= 0f) {
			throw new StandProcessingException("Age was not positive");
		}

		float trAge = FloatMath.log(ageToUse);

		float a00 = Math.max(coe.getCoe(0) + coe.getCoe(1) * trAge, 0f);
		float ap = Math.max(coe.getCoe(3) + coe.getCoe(4) * trAge, 0f);

		float bap;
		if (dominantHeight <= coe.getCoe(2)) {
			bap = 0f;
		} else {
			bap = a00 * FloatMath.pow(dominantHeight - coe.getCoe(2), ap)
					* FloatMath.exp(coe.getCoe(5) * dominantHeight + coe.getCoe(6) * baseAreaOverstory.orElse(0f));
		}

		if (fullOccupancy)
			bap /= EMPOC;

		return bap;
	}

	Coefficients estimateBaseAreaYieldCoefficients(Collection<? extends BaseVdypSpecies> species, BecDefinition bec) {
		var coeMap = Utils.<MatrixMap2<String, String, Coefficients>>expectParsedControl(
				controlMap, ControlKey.BA_YIELD, MatrixMap2.class
		);

		var coe = weightedCoefficientSum(
				7, 0, //
				species, //
				BaseVdypSpecies::getFractionGenus, // Weight by fraction
				spec -> coeMap.get(bec.getDecayBec().getAlias(), spec.getGenus())
		);

		// TODO confirm going over 0.5 should drop to 0 as this seems odd.
		coe.scalarInPlace(5, x -> x > 0.0f ? 0f : x);
		return coe;
	}

	PolygonMode findDefaultPolygonMode(
			Optional<Float> ageTotal, Optional<Float> yearsToBreastHeight, Optional<Float> height,
			Optional<Float> baseArea, Optional<Float> treesPerHectare, Optional<Float> percentForest,
			Collection<VriSpecies> species, BecDefinition bec, Optional<Integer> baseAreaGroup
	) throws StandProcessingException {
		Optional<Float> ageBH = ageTotal.map(at -> at - yearsToBreastHeight.orElse(3f));

		float bap;
		if (ageBH.map(abh -> abh >= 1).orElse(false)) {
			bap = this.estimateBaseAreaYield(
					height.get(), ageBH.get(), Optional.empty(), false, species, bec, baseAreaGroup.get()
			);
		} else {
			bap = 0;
		}

		var mode = PolygonMode.START;

		Map<String, Float> minMap = Utils.expectParsedControl(controlMap, ControlKey.MINIMA, Map.class);

		float minHeight = minMap.get(VriControlParser.MINIMUM_HEIGHT);
		float minBA = minMap.get(VriControlParser.MINIMUM_BASE_AREA);
		float minPredictedBA = minMap.get(VriControlParser.MINIMUM_PREDICTED_BASE_AREA);

		if (height.map(h -> h < minHeight).orElse(true)) {
			mode = PolygonMode.YOUNG;

			log.atDebug().setMessage("Mode {} because Height {} is below minimum {}.").addArgument(mode)
					.addArgument(height).addArgument(minHeight).log();
		} else if (bap < minPredictedBA) {
			mode = PolygonMode.YOUNG;

			log.atDebug().setMessage("Mode {} because predicted Base Area {} is below minimum {}.").addArgument(mode)
					.addArgument(bap).addArgument(minBA).log();
		} else if (baseArea.map(x -> x == 0).orElse(true) || treesPerHectare.map(x -> x == 0).orElse(true)) {
			mode = PolygonMode.YOUNG;

			log.atDebug().setMessage("Mode {} because given Base Area and Trees Per Hectare were not specified or zero")
					.addArgument(mode).log();
		} else {
			var ration = Utils.mapBoth(baseArea, percentForest, (ba, pf) -> ba * (100f / pf));

			if (ration.map(r -> r < minBA).orElse(false)) {
				mode = PolygonMode.YOUNG;
				log.atDebug().setMessage(
						"Mode {} because ration ({}) of given Base Area ({}) to Percent Forest Land ({}) was below minimum {}"
				).addArgument(mode).addArgument(ration).addArgument(baseArea).addArgument(percentForest)
						.addArgument(minBA).log();

			}
		}
		log.atDebug().setMessage("Defaulting to mode {}.").addArgument(mode).log();

		return mode;
	}

	VdypPolygon createVdypPolygon(VriPolygon sourcePolygon, Map<LayerType, VdypLayer> processedLayers)
			throws ProcessingException {

		// TODO expand this

		var vdypPolygon = VdypPolygon.build(builder -> builder.copy(sourcePolygon, x -> x.get()));
		vdypPolygon.setLayers(processedLayers);
		return vdypPolygon;
	}

	@Override
	public VdypApplicationIdentifier getId() {
		return VdypApplicationIdentifier.VRI_START;
	}

	@Override
	protected BaseControlParser getControlFileParser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected VriSpecies copySpecies(VriSpecies toCopy, Consumer<Builder<VriSpecies>> config) {
		return VriSpecies.build(builder -> {
			builder.copy(toCopy);
		});
	}
}
