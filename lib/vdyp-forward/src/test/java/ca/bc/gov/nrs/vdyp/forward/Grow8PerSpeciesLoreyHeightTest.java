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
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings.Vars;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

class Grow8PerSpeciesLoreyHeightTest {

	protected static final Logger logger = LoggerFactory.getLogger(Grow8PerSpeciesLoreyHeightTest.class);

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

		fpe.fps.fcm.getDebugSettings().setValue(Vars.LOREY_HEIGHT_CHANGE_STRATEGY_8, 0);
		fpe.processPolygon(polygon, ExecutionStep.GROW_8_SPECIES_LH);

		LayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();

		var calculatedLayerDq = lps.getBank().quadMeanDiameters[0][UtilizationClass.ALL.ordinal()];

		// VDYP7 value is 31.3084507
		assertThat(calculatedLayerDq, is(31.308355f));
	}

	@Test
	void testDebug8Setting2() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.fps.fcm.getDebugSettings().setValue(Vars.LOREY_HEIGHT_CHANGE_STRATEGY_8, 2);
		fpe.processPolygon(polygon, ExecutionStep.GROW_8_SPECIES_LH);

		LayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();

		var calculatedLayerDq = lps.getBank().quadMeanDiameters[0][UtilizationClass.ALL.ordinal()];

		// VDYP7 value is 31.3084507
		assertThat(calculatedLayerDq, is(31.308355f));
	}
}
