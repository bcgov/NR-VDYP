package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.forward.ForwardProcessingEngine.ExecutionStep;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;

class ForwardGrowStepTests extends AbstractForwardProcessingEngineTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ForwardGrowStepTests.class);

	@Test
	void testOnePolygon() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		assertThat(fpe.fps.becLookup, notNullValue());
		assertThat(fpe.fps.genusDefinitionMap, notNullValue());
		assertThat(fpe.fps.siteCurveMap, notNullValue());

		int nPolygonsProcessed = 0;
		var polygon = forwardDataStreamReader.readNextPolygon();
		
		if (polygon.isPresent()) {
			fpe.processPolygon(polygon.get(), ExecutionStep.GROW);
			nPolygonsProcessed += 1;
		}

		assertEquals(1, nPolygonsProcessed);
	}
}
