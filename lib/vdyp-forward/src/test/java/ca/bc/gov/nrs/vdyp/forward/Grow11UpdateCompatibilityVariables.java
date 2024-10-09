package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.Matchers;
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
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

class Grow11UpdateCompatibilityVariables {

	protected static final Logger logger = LoggerFactory.getLogger(Grow11UpdateCompatibilityVariables.class);

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

		fpe.processPolygon(polygon, ExecutionStep.GROW_10_COMPATIBILITY_VARS);

		// VDYP7 reports [], -9, -9, 35.473381, -9, -9)
		LayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();
		assertThat(
				// VDYP7 reports BASAL_AREA = -2.13947629e-07, all others 0.0
				lps.getCvPrimaryLayerSmall()[1],
				Matchers.allOf(
						Matchers.hasEntry(UtilizationClassVariable.BASAL_AREA, -2.1394816e-07f),
						Matchers.hasEntry(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.LOREY_HEIGHT, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f)
				)
		);
		assertThat(
				// VDYP7 reports BASAL_AREA = -4.49605286e-05, QUAD_MEAN_DIAMETER = 0.00236749649
				// LOREY_HEIGHT = 1.19209221e-06, WHOLE_STEM_VOLUME = 0.00102931913
				lps.getCvPrimaryLayerSmall()[2],
				Matchers.allOf(
						Matchers.hasEntry(UtilizationClassVariable.BASAL_AREA, -4.406223e-5f),
						Matchers.hasEntry(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0023196794f),
						Matchers.hasEntry(UtilizationClassVariable.LOREY_HEIGHT, 1.2850753e-6f),
						Matchers.hasEntry(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0010083826f)
				)
		);
		assertThat(
				// VDYP7 reports BASAL_AREA = 4.94660344e-6, QUAD_MEAN_DIAMETER = 0.0
				// LOREY_HEIGHT = -1.55569342e-5, WHOLE_STEM_VOLUME = 0.0
				lps.getCvPrimaryLayerSmall()[3],
				Matchers.allOf(
						Matchers.hasEntry(UtilizationClassVariable.BASAL_AREA, 4.8476713e-6f),
						Matchers.hasEntry(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.LOREY_HEIGHT, -1.5245796e-5f),
						Matchers.hasEntry(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f)
				)
		);
		assertThat(
				// VDYP7 reports 0.0 for all
				lps.getCvPrimaryLayerSmall()[4],
				Matchers.allOf(
						Matchers.hasEntry(UtilizationClassVariable.BASAL_AREA, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.LOREY_HEIGHT, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f)
				)
		);
		assertThat(
				// VDYP7 reports BASAL_AREA = 3.42086423e-06, LOREY_HEIGHT = -5.7758567e-5, 0.0 for all others
				lps.getCvPrimaryLayerSmall()[5],
				Matchers.allOf(
						Matchers.hasEntry(UtilizationClassVariable.BASAL_AREA, 3.352447e-6f),
						Matchers.hasEntry(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f),
						Matchers.hasEntry(UtilizationClassVariable.LOREY_HEIGHT, -5.6603396e-5f),
						Matchers.hasEntry(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f)
				)
		);
	}
}
