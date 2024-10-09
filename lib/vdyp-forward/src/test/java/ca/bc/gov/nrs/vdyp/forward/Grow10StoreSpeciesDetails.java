package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
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
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

class Grow10StoreSpeciesDetails {

	protected static final Logger logger = LoggerFactory.getLogger(Grow10StoreSpeciesDetails.class);

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
	void testStandardPath() throws ProcessingException, ValueParseException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_13_STORE_SPECIES_DETAILS);

		// VDYP7 reports [], -9, -9, 35.473381, -9, -9)
		Bank bank = fpe.fps.getPrimaryLayerProcessingState().getBank();
		assertThat(
				ForwardTestUtils.toFloatArray(bank.dominantHeights),
				is(arrayContaining(0.0f, Float.NaN, Float.NaN, 35.47338f, Float.NaN, Float.NaN))
		);
		// VDYP7 reports [], -9, -9, 35, -9, -9)
		assertThat(
				ForwardTestUtils.toFloatArray(bank.ageTotals),
				is(arrayContaining(0.0f, Float.NaN, Float.NaN, 56.0f, Float.NaN, Float.NaN))
		);
		// VDYP7 reports [], -9, -9, 56, -9, -9)
		assertThat(
				ForwardTestUtils.toFloatArray(bank.siteIndices),
				is(arrayContaining(35.0f, Float.NaN, Float.NaN, 35.0f, Float.NaN, Float.NaN))
		);
		// VDYP7 reports [], -9, -9, 55, -9, -9)
		assertThat(
				ForwardTestUtils.toFloatArray(bank.yearsAtBreastHeight),
				is(arrayContaining(0.0f, Float.NaN, Float.NaN, 55.0f, Float.NaN, Float.NaN))
		);
	}
}
