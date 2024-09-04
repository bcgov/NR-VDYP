package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.SpeciesErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.CommonData;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

class GeneralForwardProcessingEngineTest extends AbstractForwardProcessingEngineTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(GeneralForwardProcessingEngineTest.class);

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		assertThat(fpe.fps.fcm.getBecLookup(), notNullValue());
		assertThat(fpe.fps.fcm.getGenusDefinitionMap(), notNullValue());
		assertThat(fpe.fps.fcm.getSiteCurveMap(), notNullValue());

		int nPolygonsProcessed = 0;
		while (true) {
			var polygon = forwardDataStreamReader.readNextPolygon();

			if (polygon.isPresent()) {
				fpe.processPolygon(polygon.get());
				nPolygonsProcessed += 1;
			} else {
				break;
			}
		}

		assertEquals(10, nPolygonsProcessed);
	}

	@Test
	void testFindPrimarySpecies() throws IOException, ResourceParseException, ProcessingException {

		var polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		{
			ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
			fpe.fps.setPolygonLayer(polygon, LayerType.PRIMARY);

			LayerProcessingState lps = fpe.fps.getLayerProcessingState();

			ForwardProcessingEngine.calculateCoverages(lps);
			fpe.determinePolygonRankings(CommonData.PRIMARY_SPECIES_TO_COMBINE);

			assertThat(lps.getPrimarySpeciesIndex(), is(3));
			assertThat(lps.getSecondarySpeciesIndex(), is(4));
			assertThat(lps.getInventoryTypeGroup(), is(37));
			assertThat(lps.getPrimarySpeciesGroupNumber(), is(1));
			assertThat(lps.getPrimarySpeciesStratumNumber(), is(1));
		}
		{
			ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
			fpe.fps.setPolygonLayer(polygon, LayerType.PRIMARY);
			LayerProcessingState lps = fpe.fps.getLayerProcessingState();

			var speciesToCombine = Arrays
					.asList(Arrays.asList(lps.getBank().speciesNames[3], lps.getBank().speciesNames[4]));

			ForwardProcessingEngine.calculateCoverages(lps);
			fpe.determinePolygonRankings(speciesToCombine);

			// The test-specific speciesToCombine will combine 3 & 4 into 3 (leaving 4 at 0.0), promoting 2 to
			// secondary.
			assertThat(lps.getPrimarySpeciesIndex(), is(3));
			assertThat(lps.getSecondarySpeciesIndex(), is(2));
			assertThat(lps.getInventoryTypeGroup(), is(37));
			assertThat(lps.getPrimarySpeciesGroupNumber(), is(1));
			assertThat(lps.getPrimarySpeciesStratumNumber(), is(1));
		}
	}

	@Test
	void testGroupAndStratumNumberSpecialCases() throws IOException, ResourceParseException, ProcessingException {

		// We want the
		// "equationModifierGroup.isPresent()"
		// and the
		// " Region.INTERIOR.equals(lps.wallet.getBecZone().getRegion())
		// && exceptedSpeciesIndicies.contains(primarySpeciesIndex)"
		// cases in determinePolygonRankings.

		buildPolygonParserForStream(
				"testPolygon.dat", //
				"01002 S000001 00     1970 IDF  A    99 37  1  1", //
				""
		);

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0 253.9  11.1 0 -9", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.DETERMINE_POLYGON_RANKINGS);

		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesIndex(), is(1));
		assertThrows(IllegalStateException.class, () -> fpe.fps.getLayerProcessingState().getSecondarySpeciesIndex());
		assertThat(fpe.fps.getLayerProcessingState().getInventoryTypeGroup(), is(10));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesGroupNumber(), is(35));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesStratumNumber(), is(24));
	}

	@Test
	void testCombinePercentages() {

		String[] speciesNames = new String[] { "AC", "B", "C", "D", "E", "F", "PW", "H", "PY", "L", "PA", "AT", "S",
				"MB", "Y", "PL" };
		float[] percentages = new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f, 6.6f, 7.7f, 8.8f, 9.9f, 9.3f, 8.4f, 7.1f,
				6.3f, 8.4f, 9.4f, 1.6f };

		List<String> combineGroup;
		float[] testPercentages;

		combineGroup = List.of("C", "Y");
		testPercentages = Arrays.copyOf(percentages, percentages.length);

		ForwardProcessingEngine.combinePercentages(speciesNames, combineGroup, testPercentages);

		assertThat(testPercentages[2], is(0f));
		assertThat(testPercentages[14], is(12.7f));

		combineGroup = List.of("D", "PL");
		testPercentages = Arrays.copyOf(percentages, percentages.length);

		ForwardProcessingEngine.combinePercentages(speciesNames, combineGroup, testPercentages);

		assertThat(testPercentages[3], is(6.0f));
		assertThat(testPercentages[15], is(0.0f));
	}

	@Test
	void testCombinePercentagesOneGenusNotInCombinationList() {

		String[] speciesNames = new String[] { "AC", "C", "D", "E", "F", "PW", "H", "PY", "L", "PA", "AT", "S", "MB",
				"Y", "PL" };
		float[] percentages = new float[] { 1.1f, 3.3f, 4.4f, 5.5f, 6.6f, 7.7f, 8.8f, 9.9f, 9.3f, 8.4f, 7.1f, 6.3f,
				8.4f, 9.4f, 1.6f };

		List<String> combineGroup = List.of("B", "Y");
		float[] testPercentages = Arrays.copyOf(percentages, percentages.length);

		ForwardProcessingEngine.combinePercentages(speciesNames, combineGroup, testPercentages);

		assertThat(testPercentages[13], is(9.4f));
	}

	@Test
	void testCombinePercentagesBothGeneraNotInCombinationList() {

		String[] speciesNames = new String[] { "AC", "D", "E", "F", "PW", "H", "PY", "L", "PA", "AT", "S", "MB", "Y",
				"PL" };
		float[] percentages = new float[] { 1.1f, 4.4f, 5.5f, 6.6f, 7.7f, 8.8f, 9.9f, 9.3f, 8.4f, 7.1f, 6.3f, 8.4f,
				9.4f, 1.6f };

		List<String> combineGroup = List.of("B", "C");
		float[] testPercentages = Arrays.copyOf(percentages, percentages.length);

		ForwardProcessingEngine.combinePercentages(speciesNames, combineGroup, testPercentages);

		assertThat(testPercentages, is(percentages));
	}

	@Test
	void testCombinePercentagesBadCombinationList() {

		String[] speciesNames = new String[] { "AC", "D", "E", "F", "PW", "H", "PY", "L", "PA", "AT", "S", "MB", "Y",
				"PL" };
		float[] percentages = new float[] { 1.1f, 4.4f, 5.5f, 6.6f, 7.7f, 8.8f, 9.9f, 9.3f, 8.4f, 7.1f, 6.3f, 8.4f,
				9.4f, 1.6f };

		List<String> combineGroup = List.of("B", "C", "D");

		try {
			ForwardProcessingEngine.combinePercentages(speciesNames, combineGroup, percentages);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	void testCombinePercentagesBadArrays() {

		String[] speciesNames = new String[] { "D", "E", "F", "PW", "H", "PY", "L", "PA", "AT", "S", "MB", "Y", "PL" };
		float[] percentages = new float[] { 1.1f, 4.4f, 5.5f, 6.6f, 7.7f, 8.8f, 9.9f, 9.3f, 8.4f, 7.1f, 6.3f, 8.4f,
				9.4f, 1.6f };

		List<String> combineGroup = List.of("B", "C");

		try {
			ForwardProcessingEngine.combinePercentages(speciesNames, combineGroup, percentages);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	void testFindInventoryTypeGroup() throws ProcessingException {
		try {
			ForwardProcessingEngine.findInventoryTypeGroup("A", Optional.empty(), 80.0f);
			fail();
		} catch (ProcessingException e) {
			// expected
		}

		Optional<String> ac = Optional.of("AC");
		assertThrows(
				IllegalArgumentException.class, () -> ForwardProcessingEngine.findInventoryTypeGroup("AC", ac, 0.0f)
		);

		assertThat(ForwardProcessingEngine.findInventoryTypeGroup("AC", Optional.empty(), 80.0f), is(36));

		int[] expectedResults = new int[] {
				/* Secondary */
				/* AC B C D E F PW H PY L PA AT S MB Y PL -- */
				/* AC */ 35, 35, 36, 36, 35, 35, 35, 35, 35, 35, 36, 35, 36, 35, 35, 35, //
				/* B */ 20, 19, 20, 20, 20, 20, 19, 20, 20, 20, 20, 20, 20, 19, 20, 20, //
				/* C */ 10, 11, 10, 10, 10, 10, 11, 10, 10, 10, 10, 11, 10, 10, 10, 10, //
				/* D */ 38, 37, 37, 38, 37, 37, 37, 37, 37, 37, 38, 37, 38, 37, 37, 37, //
				/* E */ 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, //
				/* F */ 8, 3, 2, 8, 8, 7, 3, 6, 7, 5, 8, 4, 8, 2, 5, 8, //
				/* PW */ 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, //
				/* H */ 13, 15, 14, 13, 13, 13, 13, 13, 13, 13, 13, 16, 13, 14, 13, 13, //
				/* PY */ 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, //
				/* L */ 34, 34, 34, 34, 34, 33, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, //
				/* PA */ 31, 30, 30, 31, 31, 29, 29, 30, 29, 29, 31, 30, 31, 30, 28, 30, //
				/* AT */ 42, 41, 41, 42, 42, 41, 41, 41, 41, 41, 41, 41, 42, 41, 41, 41, //
				/* S */ 26, 24, 23, 26, 26, 22, 22, 23, 22, 22, 22, 26, 26, 23, 25, 22, //
				/* MB */ 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, //
				/* Y */ 10, 11, 10, 10, 10, 10, 10, 11, 10, 10, 10, 10, 11, 10, 10, 10, //
				/* PL */ 31, 30, 30, 31, 31, 29, 29, 30, 29, 29, 28, 31, 30, 31, 30, 30 };

		int currentAnswerIndex = 0;

		for (String primaryGenus : CommonData.ITG_PURE.keySet()) {
			for (Optional<String> secondaryGenus : CommonData.ITG_PURE.keySet().stream()
					.filter(k -> !k.equals(primaryGenus)).map(k -> Optional.of(k)).collect(Collectors.toList())) {
				int itg = ForwardProcessingEngine.findInventoryTypeGroup(primaryGenus, secondaryGenus, 50.0f);
				assertThat(itg, is(expectedResults[currentAnswerIndex++]));
			}
			int itg = ForwardProcessingEngine.findInventoryTypeGroup(primaryGenus, Optional.empty(), 50.0f);
			assertThat(itg, is(expectedResults[currentAnswerIndex++]));
		}
	}

	@Test
	void testCalculateMissingSiteCurves() throws IOException, ResourceParseException, ProcessingException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  62.9   9.1 0 -9", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0 253.9  11.1 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_MISSING_SITE_CURVES);

		// Cannot check 0 since determinePolygonRankings has not been executed.
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(1), is(12));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(2), is(2));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(3), is(42));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(4), is(10));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(5), is(10));
	}

	@Test
	void testCalculateMissingSiteCurvesNoSiteCurveData()
			throws IOException, ResourceParseException, ProcessingException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C       0.0     0.0     0.0     0.0 13.40 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  62.9   9.1 0 -9", //
				"01002 S000001 00     1970 P  8 H       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0 253.9  11.1 0 -9", //
				"01002 S000001 00     1970 P 15 S       0.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		var siteCurveMap = new MatrixMap2Impl<String, Region, SiteIndexEquation>(
				new ArrayList<String>(), new ArrayList<Region>(), (k1, k2) -> SiteIndexEquation.SI_NO_EQUATION
		);

		controlMap.put(ControlKey.SITE_CURVE_NUMBERS.name(), siteCurveMap);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_MISSING_SITE_CURVES);

		// Cannot check 0 since determinePolygonRankings has not been executed.
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(1), is(118));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(2), is(122));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(3), is(13));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(4), is(99));
		assertThat(fpe.fps.getLayerProcessingState().getSiteCurveNumber(5), is(59));
	}

	@Test
	void testEstimateMissingSiteIndicesStep1() throws ProcessingException, IOException, ResourceParseException,
			CurveErrorException, SpeciesErrorException, NoAnswerException {

		buildPolygonParserForStream("testPolygon.dat", "01002 S000001 00     1970 CWH  A    99 37  1  1");

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 13.40 -9.00  -9.0  -9.0  -9.0 0 11", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 99", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 28.90 265.0 253.9  11.1 1 99", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStandardStream("testUtilizations.dat");

		var siteCurveMap = new MatrixMap2Impl<String, Region, SiteIndexEquation>(
				new ArrayList<String>(), new ArrayList<Region>(), (k1, k2) -> SiteIndexEquation.SI_NO_EQUATION
		);

		controlMap.put(ControlKey.SITE_CURVE_NUMBERS.name(), siteCurveMap);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.ESTIMATE_MISSING_SITE_INDICES);

		// Despite 13.40 being in the data stream, the change (2024/8/29) to ignore site information
		// for all species of the layer except the primary means that method (1) will never be
		// successful, since none of the non-primary species will have a site index value.

//		var sourceSiteCurve = SiteIndexEquation.SI_CWC_KURUCZ;
//		var sourceSiteIndex = 13.4;
//		var targetSiteCurve = SiteIndexEquation.SI_HWC_WILEYAC;
//		double expectedValue = SiteTool
//				.convertSiteIndexBetweenCurves(sourceSiteCurve, sourceSiteIndex, targetSiteCurve);

		assertThat(fpe.fps.getLayerProcessingState().getBank().siteIndices[4], is(VdypEntity.MISSING_FLOAT_VALUE));
	}

	@Test
	void testEstimateMissingSiteIndicesStep2() throws ProcessingException, IOException, ResourceParseException,
			CurveErrorException, SpeciesErrorException, NoAnswerException {

		buildPolygonParserForStream("testPolygon.dat", "01002 S000001 00     1970 CWH  A    99 37  1  1");

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 13.40 28.90 265.0 253.9  11.1 1 12", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStandardStream("testUtilizations.dat");

		var siteCurveMap = new MatrixMap2Impl<String, Region, SiteIndexEquation>(
				new ArrayList<String>(), new ArrayList<Region>(), (k1, k2) -> SiteIndexEquation.SI_NO_EQUATION
		);

		controlMap.put(ControlKey.SITE_CURVE_NUMBERS.name(), siteCurveMap);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.ESTIMATE_MISSING_SITE_INDICES);

		var sourceSiteCurve = SiteIndexEquation.SI_CWC_BARKER;
		var sourceSiteIndex = 13.4;
		var targetSiteCurve = SiteIndexEquation.SI_CWC_NIGH;
		double expectedValue = SiteTool
				.convertSiteIndexBetweenCurves(sourceSiteCurve, sourceSiteIndex, targetSiteCurve);

		assertThat(fpe.fps.getLayerProcessingState().getBank().siteIndices[2], is((float) expectedValue));
	}

	@Test
	void testEstimateMissingYearsToBreastHeightValues()
			throws ProcessingException, IOException, ResourceParseException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 35.00 35.30  55.0  54.0   1.0 1 13", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(
				polygon, ForwardProcessingEngine.ExecutionStep.ESTIMATE_MISSING_YEARS_TO_BREAST_HEIGHT_VALUES
		);

		assertThat(
				fpe.fps.getLayerProcessingState().getBank().yearsToBreastHeight,
				is(new float[] { 0.0f, 4.7f, 4.6f, 1.0f, 5.0f, 5.0f })
		);
	}

	@Test
	void testCalculateDominantHeightAgeSiteIndex() throws ProcessingException, IOException, ResourceParseException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				// Polygon Year L? x G S1 % S2 % S3 % S4 % Sidx domH age agebh y2bh P? SC
				"01002 S000001 00     1970 P  3 B  B  100.0     0.0     0.0     0.0 -9.00 -9.00  15.0  11.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 34.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P  5 D  D  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 1 13", //
				"01002 S000001 00     1970 P  8 H  H  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970 P 15 S  S  100.0     0.0     0.0     0.0 -9.00 -9.00  -9.0  -9.0  -9.0 0 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStandardStream("testUtilizations.dat");

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		// Since the change to ignore site information for all but non-primary species, there is
		// no way to successfully estimate age for a primary species from the non-primary species.
		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		assertThrows(
				ProcessingException.class,
				() -> fpe.processPolygon(
						polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX
				)
		);
	}

	@Test
	void testSetEquationGroups() throws ResourceParseException, IOException, ProcessingException {

		var reader = new ForwardDataStreamReader(controlMap);
		var polygon = reader.readNextPolygon().get();

		ForwardProcessingState fps = new ForwardProcessingState(controlMap);
		fps.setPolygonLayer(polygon, LayerType.PRIMARY);

		assertThat(
				fps.getLayerProcessingState().volumeEquationGroups,
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 12, 20, 25, 37, 66 })
		);
		assertThat(
				fps.getLayerProcessingState().decayEquationGroups,
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 7, 14, 19, 31, 54 })
		);
		assertThat(
				fps.getLayerProcessingState().breakageEquationGroups,
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 5, 6, 12, 17, 28 })
		);
	}

	@Test
	void testCalculateDominantHeightAgeSiteIndexNoSecondary()
			throws ProcessingException, IOException, ResourceParseException {

		buildSpeciesParserForStream(
				"testSpecies.dat", //
				"01002 S000001 00     1970 P  4 C  C  100.0     0.0     0.0     0.0 34.00 -9.00  22.0  -9.0  -9.0 1 -9", //
				"01002 S000001 00     1970"
		);

		buildUtilizationParserForStream(
				"testUtilizations.dat", //
				"01002 S000001 00     1970 P  0    -1  0.01513     5.24   7.0166   0.0630   0.0000   0.0000   0.0000   0.0000   6.1", //
				"01002 S000001 00     1970 P  0     0 44.93259   595.32  30.9724 620.9775 592.2023 580.1681 577.6229 549.0159  31.0", //
				"01002 S000001 00     1970 P  0     1  0.53100    64.82  -9.0000   2.5979   0.3834   0.3794   0.3788   0.3623  10.2", //
				"01002 S000001 00     1970 P  0     2  1.27855    71.93  -9.0000   9.1057   6.9245   6.8469   6.8324   6.5384  15.0", //
				"01002 S000001 00     1970 P  0     3  2.33020    73.60  -9.0000  22.4019  20.1244  19.8884  19.8375  18.9555  20.1", //
				"01002 S000001 00     1970 P  0     4 40.79285   384.98  -9.0000 586.8720 564.7699 553.0534 550.5741 523.1597  36.7", //
				"01002 S000001 00     1970 P  4 C  -1  0.01243     4.40   6.4602   0.0507   0.0000   0.0000   0.0000   0.0000   6.0", //
				"01002 S000001 00     1970 P  4 C   0  5.04597    83.46  22.9584  43.4686  39.4400  36.2634  35.2930  32.9144  27.7", //
				"01002 S000001 00     1970 P  4 C   1  0.12822    16.12  -9.0000   0.6027   0.1116   0.1094   0.1090   0.1035  10.1", //
				"01002 S000001 00     1970 P  4 C   2  0.31003    17.87  -9.0000   1.9237   1.4103   1.3710   1.3628   1.2915  14.9", //
				"01002 S000001 00     1970 P  4 C   3  0.51339    16.55  -9.0000   3.8230   3.3162   3.2127   3.1859   3.0076  19.9", //
				"01002 S000001 00     1970 P  4 C   4  4.09434    32.92  -9.0000  37.1192  34.6019  31.5703  30.6352  28.5119  39.8", //
				"01002 S000001 00     1970"
		);

		var reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX);

		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesDominantHeight(), is(22.950302f));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesSiteIndex(), is(34.0f));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesTotalAge(), is(22.0f));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesAgeAtBreastHeight(), is(Float.NaN));
		assertThat(fpe.fps.getLayerProcessingState().getPrimarySpeciesAgeToBreastHeight(), is(4.7f));
	}
}
