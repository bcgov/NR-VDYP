package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CombinePercentagesTests {

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

}
