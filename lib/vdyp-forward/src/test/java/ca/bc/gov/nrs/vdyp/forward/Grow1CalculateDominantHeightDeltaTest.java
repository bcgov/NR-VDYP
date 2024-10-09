package ca.bc.gov.nrs.vdyp.forward;

//import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.coe;
//import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.controlMapHasEntry;
//import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.hasBec;
//import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmEmpty;
//import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.mmHasEntry;
//import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.allOf;
//import static org.hamcrest.Matchers.contains;
//import static org.hamcrest.Matchers.hasEntry;
//import static org.hamcrest.Matchers.hasItem;
//import static org.hamcrest.Matchers.hasKey;
//import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.isA;
//import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

class Grow1CalculateDominantHeightDeltaTest {

	protected static final Logger logger = LoggerFactory.getLogger(Grow1CalculateDominantHeightDeltaTest.class);

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
	void testNormalCurve() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		var polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		// Select the first polygon - 01002 S000001 00(1970)
		fpe.fps.setPolygon(polygon);

		float hd = 35.2999992f;
		int sc = 13;
		float si = 35;
		float ytbh = 1.0f;

		float gdh = fpe.calculateDominantHeightDelta(hd, sc, si, ytbh);

		assertThat(gdh, is(0.173380271f));
	}

	@Test
	void testCurveExtension1() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select polygon 01003AS000001 00(1953) - triggers curve extension code
		VdypPolygon polygon;
		do {
			polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();
		} while (!polygon.getPolygonIdentifier().getName().equals("01003AS000001 00"));

		fpe.fps.setPolygon(polygon);

		float hd = 29.5f;
		int sc = 11;
		float si = 14.8000002f;
		float ytbh = 10.8000002f;

		float gdh = fpe.calculateDominantHeightDelta(hd, sc, si, ytbh);

		assertThat(gdh, is(0.0f));
	}

	@Test
	void testCurveExtension2() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select polygon 01003AS000001 00(1953) - triggers curve extension code
		VdypPolygon polygon;
		do {
			polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();
		} while (!polygon.getPolygonIdentifier().getName().equals("01003AS000001 00"));

		fpe.fps.setPolygon(polygon);

		fpe.processPolygon(polygon, ExecutionStep.GROW_1_LAYER_DHDELTA.predecessor());

		float hd = 26.5f;
		int sc = 11;
		float si = 14.8000002f;
		float ytbh = 5.8000002f;

		float gdh = fpe.calculateDominantHeightDelta(hd, sc, si, ytbh);

		assertThat(gdh, is(0.045883115f));
	}
}
