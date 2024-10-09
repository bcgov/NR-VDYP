package ca.bc.gov.nrs.vdyp.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;

public class FindInventoryTypeGroupsTest {

	@Test
	void findInventoryTypeGroupsTest() throws ProcessingException {
		assertEquals(8, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.empty(), 75));
		assertEquals(2, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.of("Y"), 75));
		assertEquals(3, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.of("B"), 75));
		assertEquals(4, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.of("S"), 75));
		assertEquals(5, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.of("PL"), 75));
		assertEquals(6, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.of("PY"), 75));
		assertEquals(7, ForwardProcessingEngine.findInventoryTypeGroup("F", Optional.of("L"), 75));

		assertEquals(11, ForwardProcessingEngine.findInventoryTypeGroup("C", Optional.of("B"), 75));
		assertEquals(10, ForwardProcessingEngine.findInventoryTypeGroup("Y", Optional.empty(), 75));

		assertEquals(14, ForwardProcessingEngine.findInventoryTypeGroup("H", Optional.of("C"), 75));
		assertEquals(15, ForwardProcessingEngine.findInventoryTypeGroup("H", Optional.of("B"), 75));
		assertEquals(16, ForwardProcessingEngine.findInventoryTypeGroup("H", Optional.of("S"), 75));
		assertEquals(13, ForwardProcessingEngine.findInventoryTypeGroup("H", Optional.empty(), 75));

		assertEquals(19, ForwardProcessingEngine.findInventoryTypeGroup("B", Optional.of("C"), 75));
		assertEquals(20, ForwardProcessingEngine.findInventoryTypeGroup("B", Optional.empty(), 75));

		assertEquals(23, ForwardProcessingEngine.findInventoryTypeGroup("S", Optional.of("C"), 75));
		assertEquals(24, ForwardProcessingEngine.findInventoryTypeGroup("S", Optional.of("B"), 75));
		assertEquals(25, ForwardProcessingEngine.findInventoryTypeGroup("S", Optional.of("PL"), 75));
		assertEquals(26, ForwardProcessingEngine.findInventoryTypeGroup("S", Optional.of("AC"), 75));
		assertEquals(22, ForwardProcessingEngine.findInventoryTypeGroup("S", Optional.of("F"), 75));

		assertEquals(27, ForwardProcessingEngine.findInventoryTypeGroup("PW", Optional.of("C"), 75));
		assertEquals(27, ForwardProcessingEngine.findInventoryTypeGroup("PW", Optional.empty(), 75));

		assertEquals(28, ForwardProcessingEngine.findInventoryTypeGroup("PL", Optional.of("PL"), 75));
		assertEquals(29, ForwardProcessingEngine.findInventoryTypeGroup("PL", Optional.of("F"), 75));
		assertEquals(30, ForwardProcessingEngine.findInventoryTypeGroup("PL", Optional.empty(), 75));

		assertEquals(32, ForwardProcessingEngine.findInventoryTypeGroup("PY", Optional.of("C"), 75));
		assertEquals(32, ForwardProcessingEngine.findInventoryTypeGroup("PY", Optional.empty(), 75));

		assertEquals(33, ForwardProcessingEngine.findInventoryTypeGroup("L", Optional.of("F"), 75));
		assertEquals(34, ForwardProcessingEngine.findInventoryTypeGroup("L", Optional.empty(), 75));

		assertEquals(36, ForwardProcessingEngine.findInventoryTypeGroup("AC", Optional.of("E"), 75));
		assertEquals(35, ForwardProcessingEngine.findInventoryTypeGroup("AC", Optional.of("C"), 75));

		assertEquals(38, ForwardProcessingEngine.findInventoryTypeGroup("D", Optional.of("E"), 75));
		assertEquals(37, ForwardProcessingEngine.findInventoryTypeGroup("D", Optional.of("C"), 75));

		assertEquals(39, ForwardProcessingEngine.findInventoryTypeGroup("MB", Optional.of("F"), 75));
		assertEquals(39, ForwardProcessingEngine.findInventoryTypeGroup("MB", Optional.empty(), 75));

		assertEquals(40, ForwardProcessingEngine.findInventoryTypeGroup("E", Optional.of("F"), 75));
		assertEquals(40, ForwardProcessingEngine.findInventoryTypeGroup("E", Optional.empty(), 75));

		assertEquals(42, ForwardProcessingEngine.findInventoryTypeGroup("AT", Optional.of("E"), 75));
		assertEquals(41, ForwardProcessingEngine.findInventoryTypeGroup("AT", Optional.of("C"), 75));

		assertThrows(
				ProcessingException.class,
				() -> ForwardProcessingEngine.findInventoryTypeGroup("Z", Optional.empty(), 90)
		);
	}
}
