package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;

class ProcessMultiplePolygonsBasicTest extends AbstractForwardProcessingEngineTest {

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		assertThat(fpe.fps.fcm.getBecLookup(), notNullValue());
		assertThat(fpe.fps.fcm.getGenusDefinitionMap(), notNullValue());
		assertThat(fpe.fps.fcm.getSiteCurveMap(), notNullValue());

		// Fetch the next polygon to process.
		int nPolygonsProcessed = 0;
		while (polygonDescriptionStream.hasNext()) {

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
}
