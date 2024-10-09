package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings.Vars;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

class Grow3CalculateQuadMeanDiameterDeltaTest {

	protected static final Logger logger = LoggerFactory.getLogger(Grow3CalculateQuadMeanDiameterDeltaTest.class);

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
	void testMixedModel() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_3_LAYER_DQDELTA.predecessor());

		ForwardDebugSettings debugSettings = fpe.fps.fcm.getDebugSettings();
		debugSettings.setValue(Vars.DQ_GROWTH_MODEL_6, 2);

		float yabh = 54.0f;
		float hd = 35.2999992f;
		float ba = 45.3864441f;
		float dq = 30.9988747f;
		float growthInHd = 0.173380271f;
		Optional<Float> v_ba_start = Optional.empty();
		Optional<Float> v_ba_end = Optional.empty();
		Reference<Boolean> dqGrowthLimitApplied = new Reference<>();

		float gba = fpe.calculateQuadMeanDiameterDelta(
				yabh, ba, hd, dq, v_ba_start, v_ba_end, growthInHd, dqGrowthLimitApplied
		);

		assertThat(gba, is(0.30947846f));
		assertTrue(dqGrowthLimitApplied.isPresent());
		assertThat(dqGrowthLimitApplied.get(), is(false));
	}

	@Test
	void testFiatOnlyModel() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_3_LAYER_DQDELTA.predecessor());

		ForwardDebugSettings debugSettings = fpe.fps.fcm.getDebugSettings();
		debugSettings.setValue(Vars.DQ_GROWTH_MODEL_6, 0); /* this value will force the fiat only calculations. */

		float yabh = 54.0f;
		float hd = 35.2999992f;
		float ba = 45.3864441f;
		float dq = 30.9988747f;
		float growthInHd = 0.173380271f;
		Optional<Float> v_ba_start = Optional.empty();
		Optional<Float> v_ba_end = Optional.empty();
		Reference<Boolean> dqGrowthLimitApplied = new Reference<>();

		float gba = fpe.calculateQuadMeanDiameterDelta(
				yabh, ba, hd, dq, v_ba_start, v_ba_end, growthInHd, dqGrowthLimitApplied
		);

		assertThat(gba, is(0.3194644f));
		assertTrue(dqGrowthLimitApplied.isPresent());
		assertThat(dqGrowthLimitApplied.get(), is(false));
	}

	@Test
	void testEmpiricalOnlyModel() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_3_LAYER_DQDELTA.predecessor());

		ForwardDebugSettings debugSettings = fpe.fps.fcm.getDebugSettings();
		debugSettings.setValue(Vars.DQ_GROWTH_MODEL_6, 1); /* this value will force the empirical only calculations. */

		float yabh = 54.0f;
		float hd = 35.2999992f;
		float ba = 45.3864441f;
		float dq = 30.9988747f;
		float growthInHd = 0.173380271f;
		Optional<Float> v_ba_start = Optional.empty();
		Optional<Float> v_ba_end = Optional.empty();
		Reference<Boolean> dqGrowthLimitApplied = new Reference<>();

		float gba = fpe.calculateQuadMeanDiameterDelta(
				yabh, ba, hd, dq, v_ba_start, v_ba_end, growthInHd, dqGrowthLimitApplied
		);

		assertThat(gba, is(0.30947846f));
		assertTrue(dqGrowthLimitApplied.isPresent());
		assertThat(dqGrowthLimitApplied.get(), is(false));
	}

	@Test
	void testMixedModelWithInterpolation() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_3_LAYER_DQDELTA.predecessor());

		ForwardDebugSettings debugSettings = fpe.fps.fcm.getDebugSettings();
		debugSettings.setValue(Vars.DQ_GROWTH_MODEL_6, 2);

		float yabh = 104.0f; /* this value will force interpolation. */
		float hd = 35.2999992f;
		float ba = 45.3864441f;
		float dq = 30.9988747f;
		float growthInHd = 0.173380271f;
		Optional<Float> v_ba_start = Optional.empty();
		Optional<Float> v_ba_end = Optional.empty();
		Reference<Boolean> dqGrowthLimitApplied = new Reference<>();

		float gba = fpe.calculateQuadMeanDiameterDelta(
				yabh, ba, hd, dq, v_ba_start, v_ba_end, growthInHd, dqGrowthLimitApplied
		);

		assertThat(gba, is(0.2830987f));
		assertTrue(dqGrowthLimitApplied.isPresent());
		assertThat(dqGrowthLimitApplied.get(), is(false));
	}

	@Test
	void testMinimumApplied() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_3_LAYER_DQDELTA.predecessor());

		ForwardDebugSettings debugSettings = fpe.fps.fcm.getDebugSettings();
		debugSettings.setValue(Vars.DQ_GROWTH_MODEL_6, 0);

		float yabh = 54.0f;
		float hd = 35.3f;
		float ba = 45.3864441f;
		float dq = 3.0f; /* this value will force the minimum test to kick in. */
		float growthInHd = 0.173380271f;
		Optional<Float> v_ba_start = Optional.empty();
		Optional<Float> v_ba_end = Optional.empty();
		Reference<Boolean> dqGrowthLimitApplied = new Reference<>();

		float gba = fpe.calculateQuadMeanDiameterDelta(
				yabh, ba, hd, dq, v_ba_start, v_ba_end, growthInHd, dqGrowthLimitApplied
		);

		assertThat(gba, is(4.6f));
		assertTrue(dqGrowthLimitApplied.isPresent());
		assertThat(dqGrowthLimitApplied.get(), is(false));
	}

	@Test
	void testLimitApplied() throws ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		// Select the first polygon - 01002 S000001 00(1970)
		VdypPolygon polygon = forwardDataStreamReader.readNextPolygon().orElseThrow();

		fpe.processPolygon(polygon, ExecutionStep.GROW_3_LAYER_DQDELTA.predecessor());

		ForwardDebugSettings debugSettings = fpe.fps.fcm.getDebugSettings();
		debugSettings.setValue(Vars.DQ_GROWTH_MODEL_6, 0);

		float yabh = 54.0f;
		float hd = 35.3f;
		float ba = 45.3864441f;
		float dq = 50.0f; /* this value will force the limit test to kick in. */
		float growthInHd = 0.173380271f;
		Optional<Float> v_ba_start = Optional.empty();
		Optional<Float> v_ba_end = Optional.empty();
		Reference<Boolean> dqGrowthLimitApplied = new Reference<>();

		float gba = fpe.calculateQuadMeanDiameterDelta(
				yabh, ba, hd, dq, v_ba_start, v_ba_end, growthInHd, dqGrowthLimitApplied
		);

		assertThat(gba, is(0.0f));
		assertTrue(dqGrowthLimitApplied.isPresent());
		assertThat(dqGrowthLimitApplied.get(), is(true));
	}
}
