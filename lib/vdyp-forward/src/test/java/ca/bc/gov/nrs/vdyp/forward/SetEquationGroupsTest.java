package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;

public class SetEquationGroupsTest extends AbstractForwardProcessingEngineTest {

	@Test
	void testSetEquationGroups() throws ResourceParseException, IOException, ProcessingException {

		var reader = new ForwardDataStreamReader(controlMap);
		var polygon = reader.readNextPolygon().get();

		ForwardProcessingState fps = new ForwardProcessingState(controlMap);
		fps.setPolygon(polygon);

		assertThat(
				fps.getPrimaryLayerProcessingState().getVolumeEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 12, 20, 25, 37, 66 })
		);
		assertThat(
				fps.getPrimaryLayerProcessingState().getDecayEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 7, 14, 19, 31, 54 })
		);
		assertThat(
				fps.getPrimaryLayerProcessingState().getBreakageEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 5, 6, 12, 17, 28 })
		);
	}

}
