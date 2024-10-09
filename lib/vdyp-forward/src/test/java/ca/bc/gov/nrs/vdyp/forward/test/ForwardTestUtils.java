package ca.bc.gov.nrs.vdyp.forward.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.opentest4j.AssertionFailedError;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.forward.ForwardControlParser;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingState;
import ca.bc.gov.nrs.vdyp.forward.model.ControlVariable;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class ForwardTestUtils {

	/**
	 * Fill in the decay modifiers in a control map with mock data for testing.
	 *
	 * @param controlMap
	 * @param mapper
	 */
	public static void
			populateControlMapDecayModifiers(Map<String, Object> controlMap, BiFunction<String, Region, Float> mapper) {
		var spec = Arrays.asList(TestUtils.getSpeciesAliases());
		var regions = Arrays.asList(Region.values());
		TestUtils
				.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_DECAY.name(), spec, regions, mapper);
	}

	/**
	 * Fill in the waste modifiers in a control map with mock data for testing.
	 *
	 * @param controlMap
	 * @param mapper
	 */
	public static void
			populateControlMapWasteModifiers(Map<String, Object> controlMap, BiFunction<String, Region, Float> mapper) {
		var spec = Arrays.asList(TestUtils.getSpeciesAliases());
		var regions = Arrays.asList(Region.values());
		TestUtils
				.populateControlMap2(controlMap, ModifierParser.CONTROL_KEY_MOD301_WASTE.name(), spec, regions, mapper);
	}

	/**
	 * Apply modifiers to mock test map to simulate control file parser.
	 *
	 * @param controlMap
	 */
	public static void modifyControlMap(HashMap<String, Object> controlMap) {
		var jprogram = VdypApplicationIdentifier.VDYP_FORWARD;
		TestUtils.populateControlMapFromResource(controlMap, new ModifierParser(jprogram), "mod19813.prm");

	}

	public static Map<String, Object> loadControlMap(ForwardControlParser parser, Class<?> klazz, String resourceName)
			throws IOException, ResourceParseException {
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}

	/**
	 * Load the control map from resources in the test package using the full control map parser.
	 */
	public static Map<String, Object> loadControlMap() {
		var parser = new ForwardControlParser();
		try {
			return loadControlMap(parser, ForwardControlParser.class, "FIPSTART.CTR");
		} catch (IOException | ResourceParseException ex) {
			throw new AssertionFailedError(null, ex);
		}
	}

	public static Map<String, Object> parse(BaseControlParser parser, String resourceName)
			throws IOException, ResourceParseException {

		Class<?> klazz = TestUtils.class;
		try (var is = klazz.getResourceAsStream(resourceName)) {

			return parser.parse(is, TestUtils.fileResolver(klazz), new HashMap<>());
		}
	}

	public static Float[] toFloatArray(float[] af) {
		Float[] aF = new Float[af.length];
		for (int i = 0; i < af.length; i++)
			aF[i] = af[i];
		return aF;
	}

	/**
	 * Explicitly override the per-polygon or overall target year settings to use the given value instead. This will
	 * apply for all polygons not yet processed, unless it is called again later, of course.
	 *
	 * @param fps        the Forward processing state
	 * @param targetYear the desired target year
	 */
	public static void setGrowthTargetYear(ForwardProcessingState fps, int targetYear) {
		try {
			fps.fcm.getForwardControlVariables().setControlVariable(ControlVariable.GROW_TARGET_1, targetYear);
		} catch (ValueParseException e) {
			throw new RuntimeException(e);
		}
	}

	public static VdypLayer normalizeLayer(VdypLayer layer) {

		// Set uc All to the sum of the UC values, UC 7.5 and above only, for the summable
		// values, and calculate quad-mean-diameter from these values.

		UtilizationClass ucAll = UtilizationClass.ALL;
		UtilizationClass ucSmall = UtilizationClass.SMALL;

		for (var species : layer.getSpecies().values()) {

			species.getBaseAreaByUtilization().set(
					ucAll, sumUtilizationClassValues(species.getBaseAreaByUtilization(), UtilizationClass.UTIL_CLASSES)
			);
			species.getTreesPerHectareByUtilization().set(
					ucAll,
					sumUtilizationClassValues(species.getTreesPerHectareByUtilization(), UtilizationClass.UTIL_CLASSES)
			);
			species.getWholeStemVolumeByUtilization().set(
					ucAll,
					sumUtilizationClassValues(species.getWholeStemVolumeByUtilization(), UtilizationClass.UTIL_CLASSES)
			);
			species.getCloseUtilizationVolumeByUtilization().set(
					ucAll,
					sumUtilizationClassValues(
							species.getCloseUtilizationVolumeByUtilization(), UtilizationClass.UTIL_CLASSES
					)
			);
			species.getCloseUtilizationVolumeNetOfDecayByUtilization().set(
					ucAll,
					sumUtilizationClassValues(
							species.getCloseUtilizationVolumeNetOfDecayByUtilization(), UtilizationClass.UTIL_CLASSES
					)
			);
			species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(
					ucAll,
					sumUtilizationClassValues(
							species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(),
							UtilizationClass.UTIL_CLASSES
					)
			);

			if (species.getBaseAreaByUtilization().get(ucAll) > 0.0f) {
				species.getQuadraticMeanDiameterByUtilization().set(
						ucAll,
						BaseAreaTreeDensityDiameter.quadMeanDiameter(
								species.getBaseAreaByUtilization().get(ucAll),
								species.getTreesPerHectareByUtilization().get(ucAll)
						)
				);
			}
		}

		// Set the layer's uc All values (for summable types) to the sum of those of the
		// individual species.

		layer.getBaseAreaByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "BaseArea", UtilizationClass.ALL));
		layer.getTreesPerHectareByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "TreesPerHectare", UtilizationClass.ALL));
		layer.getWholeStemVolumeByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "WholeStemVolume", UtilizationClass.ALL));
		layer.getCloseUtilizationVolumeByUtilization()
				.set(ucAll, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolume", UtilizationClass.ALL));
		layer.getCloseUtilizationVolumeNetOfDecayByUtilization().set(
				ucAll, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolumeNetOfDecay", UtilizationClass.ALL)
		);
		layer.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(
				ucAll,
				sumSpeciesUtilizationClassValues(
						layer, "CloseUtilizationVolumeNetOfDecayAndWaste", UtilizationClass.ALL
				)
		);

		// Calculate the layer's uc All values for quad-mean-diameter and lorey height

		float sumLoreyHeightByBasalAreaSmall = 0.0f;
		float sumBasalAreaSmall = 0.0f;
		float sumLoreyHeightByBasalAreaAll = 0.0f;

		for (var species : layer.getSpecies().values()) {
			sumLoreyHeightByBasalAreaSmall += species.getLoreyHeightByUtilization().get(ucSmall)
					* species.getBaseAreaByUtilization().get(ucSmall);
			sumBasalAreaSmall += species.getBaseAreaByUtilization().get(ucSmall);
			sumLoreyHeightByBasalAreaAll += species.getLoreyHeightByUtilization().get(ucAll)
					* species.getBaseAreaByUtilization().get(ucAll);
		}

		if (layer.getBaseAreaByUtilization().get(ucAll) > 0.0f) {
			layer.getQuadraticMeanDiameterByUtilization().set(
					ucAll,
					BaseAreaTreeDensityDiameter.quadMeanDiameter(
							layer.getBaseAreaByUtilization().get(ucAll),
							layer.getTreesPerHectareByUtilization().get(ucAll)
					)
			);
			layer.getLoreyHeightByUtilization()
					.set(ucAll, sumLoreyHeightByBasalAreaAll / layer.getBaseAreaByUtilization().get(ucAll));
		}

		// Calculate the layer's lorey height uc Small value

		if (sumBasalAreaSmall > 0.0f) {
			layer.getLoreyHeightByUtilization().set(ucSmall, sumLoreyHeightByBasalAreaSmall / sumBasalAreaSmall);
		}

		// Finally, set the layer's summable UC values (other than All, which was computed above) to
		// the sums of those of each of the species.

		for (UtilizationClass uc : UtilizationClass.ALL_CLASSES) {
			layer.getBaseAreaByUtilization().set(uc, sumSpeciesUtilizationClassValues(layer, "BaseArea", uc));
			layer.getTreesPerHectareByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "TreesPerHectare", uc));
			layer.getWholeStemVolumeByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "WholeStemVolume", uc));
			layer.getCloseUtilizationVolumeByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolume", uc));
			layer.getCloseUtilizationVolumeNetOfDecayByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolumeNetOfDecay", uc));
			layer.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.set(uc, sumSpeciesUtilizationClassValues(layer, "CloseUtilizationVolumeNetOfDecayAndWaste", uc));
		}

		return layer;
	}

	private static float sumUtilizationClassValues(UtilizationVector ucValues, List<UtilizationClass> subjects) {
		float sum = 0.0f;

		for (UtilizationClass uc : UtilizationClass.values()) {
			if (subjects.contains(uc)) {
				sum += ucValues.get(uc);
			}
		}

		return sum;
	}

	private static float sumSpeciesUtilizationClassValues(VdypLayer layer, String uvName, UtilizationClass uc) {
		float sum = 0.0f;

		for (VdypSpecies species : layer.getSpecies().values()) {
			switch (uvName) {
			case "CloseUtilizationVolumeNetOfDecayWasteAndBreakage":
				sum += species.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().get(uc);
				break;
			case "CloseUtilizationVolumeNetOfDecayAndWaste":
				sum += species.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc);
				break;
			case "CloseUtilizationVolumeNetOfDecay":
				sum += species.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc);
				break;
			case "CloseUtilizationVolume":
				sum += species.getCloseUtilizationVolumeByUtilization().get(uc);
				break;
			case "WholeStemVolume":
				sum += species.getWholeStemVolumeByUtilization().get(uc);
				break;
			case "TreesPerHectare":
				sum += species.getTreesPerHectareByUtilization().get(uc);
				break;
			case "BaseArea":
				sum += species.getBaseAreaByUtilization().get(uc);
				break;
			default:
				throw new IllegalStateException(uvName + " is not a known utilization vector name");
			}
		}

		return sum;
	}
}
