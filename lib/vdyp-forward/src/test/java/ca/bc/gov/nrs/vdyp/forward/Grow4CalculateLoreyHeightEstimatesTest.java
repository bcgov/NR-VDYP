package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class Grow4CalculateLoreyHeightEstimatesTest {

	protected static final Logger logger = LoggerFactory.getLogger(Grow4CalculateLoreyHeightEstimatesTest.class);

	protected static ForwardControlParser parser;
	protected static Map<String, Object> controlMap;

	protected static StreamingParserFactory<PolygonIdentifier> polygonDescriptionStreamFactory;
	protected static StreamingParser<PolygonIdentifier> polygonDescriptionStream;

	protected static ForwardDataStreamReader forwardDataStreamReader;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void beforeTest() throws IOException, ResourceParseException, ProcessingException {
		parser = new ForwardControlParser();
		controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");

		polygonDescriptionStreamFactory = (StreamingParserFactory<PolygonIdentifier>) controlMap
				.get(ControlKey.FORWARD_INPUT_GROWTO.name());
		polygonDescriptionStream = polygonDescriptionStreamFactory.get();

		forwardDataStreamReader = new ForwardDataStreamReader(controlMap);
	}

	@Test
	void testStandardPath() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_4_LAYER_BA_AND_DQTPH_EST);
		LayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();

		float dhStart = 35.3f;
		float dhEnd = 35.473381f;
		float pspTphStart = 290.61615f;
		float pspTphEnd = 287.107788f;
		float pspLhStart = 33.7439995f;

		fpe.growLoreyHeights(lps, dhStart, dhEnd, pspTphStart, pspTphEnd, pspLhStart);

		// Results are stored in bank.loreyHeights[1..nSpecies]
		assertThat(lps.getIndices().length, is(5));
		assertThat(lps.getBank().loreyHeights[1][UtilizationClass.ALL.ordinal()], is(36.9653244f));
		assertThat(lps.getBank().loreyHeights[2][UtilizationClass.ALL.ordinal()], is(23.03769f));
		assertThat(lps.getBank().loreyHeights[3][UtilizationClass.ALL.ordinal()], is(33.930603f));
		assertThat(lps.getBank().loreyHeights[4][UtilizationClass.ALL.ordinal()], is(22.8913193f));
		assertThat(lps.getBank().loreyHeights[5][UtilizationClass.ALL.ordinal()], is(32.2539024f));
	}

	@Test
	void testDebug8Setting2Path() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_4_LAYER_BA_AND_DQTPH_EST);
		LayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();

		float dhStart = 35.3f;
		float dhEnd = 35.3f;
		float pspTphStart = 290.61615f;
		float pspTphEnd = 287.107788f;
		float pspLhStart = 33.7439995f;

		fpe.fps.fcm.getDebugSettings().setValue(ForwardDebugSettings.Vars.LOREY_HEIGHT_CHANGE_STRATEGY_8, 2);

		fpe.growLoreyHeights(lps, dhStart, dhEnd, pspTphStart, pspTphEnd, pspLhStart);

		// Results are stored in bank.loreyHeights[1..nSpecies]
		assertThat(lps.getIndices().length, is(5));
		assertThat(lps.getBank().loreyHeights[1][UtilizationClass.ALL.ordinal()], is(36.7552986f));
		assertThat(lps.getBank().loreyHeights[2][UtilizationClass.ALL.ordinal()], is(22.9584007f));
		assertThat(lps.getBank().loreyHeights[3][UtilizationClass.ALL.ordinal()], is(33.7439995f));
		assertThat(lps.getBank().loreyHeights[4][UtilizationClass.ALL.ordinal()], is(22.7703991f));
		assertThat(lps.getBank().loreyHeights[5][UtilizationClass.ALL.ordinal()], is(32.0125008f));
	}
}
