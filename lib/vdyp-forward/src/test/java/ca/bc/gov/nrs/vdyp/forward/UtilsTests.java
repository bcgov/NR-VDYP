package ca.bc.gov.nrs.vdyp.forward;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class UtilsTests {

	@Test
	void testOptNumberMethods() {
		assertEquals(Optional.empty(), Utils.optFloat(null));
		assertEquals(Optional.of(34.5f), Utils.optFloat(34.5f));
		assertEquals(Optional.of(34), Utils.optInt(34));
		assertEquals(Optional.empty(), Utils.optInt(VdypEntity.MISSING_INTEGER_VALUE));
	}

	@Test
	void testOptionalMethods() {
		{
			class ResultHolder {
				float result = 0.0f;

				ResultHolder() {
				}

				void setResult(float f) {
					result = f;
				}

				float getResult() {
					return result;
				}
			}
			;
			ResultHolder result = new ResultHolder();
			Utils.ifBothPresent(Optional.of(2.0f), Optional.of(3.0f), (a, b) -> result.setResult(a + b));
			assertEquals(5.0f, result.getResult());
		}

		{
			Optional<Float> result1 = Utils
					.flatMapBoth(Optional.of(2.0f), Optional.of(3.0f), (a, b) -> Optional.of(a + b));
			assertEquals(5.0f, result1.get());
			Optional<Float> e = Optional.empty();
			Optional<Float> result2 = Utils.flatMapBoth(e, Optional.of(3.0f), (a, b) -> Optional.of(a));
			assertEquals(Optional.empty(), result2);
			Optional<Float> result3 = Utils.flatMapBoth(e, e, (a, b) -> Optional.of(b));
			assertEquals(Optional.empty(), result3);
		}

		{
			Optional<Integer> e = Optional.empty();
			assertEquals(0, Utils.compareOptionals(e, e));
			assertEquals(-1, Utils.compareOptionals(e, Optional.of(0)));
			assertEquals(1, Utils.compareOptionals(Optional.of(0), e));
			assertEquals(-1, Utils.compareOptionals(Optional.of(0), Optional.of(1)));
		}

		{
			Optional<Integer> e = Optional.empty();
			assertEquals(Optional.of(0), Utils.getIfPresent(List.of(0, 1, 2), 0));
			assertEquals(e, Utils.getIfPresent(List.of(0, 1, 2), 3));
		}
	}

	@Test
	void testGenusMethods() {
		Map<String, Object> controlMap = TestUtils.loadControlMap();
		assertEquals("AC", Utils.getGenusDefinition("AC", controlMap).getAlias());
		assertEquals(1, Utils.getGenusIndex("AC", controlMap));
		assertThrows(IllegalArgumentException.class, () -> Utils.getGenusDefinition("Z", controlMap).getAlias());
	}

	@Test
	void testListMethods() {
		var list = List.of(0, 1, 2);
		var allButLastList = new ArrayList<Integer>();
		var lastList = new ArrayList<Integer>();

		Utils.eachButLast(list, e -> allButLastList.add(e), e -> lastList.add(e));

		assertEquals(List.of(0, 1), allButLastList);
		assertEquals(List.of(2), lastList);
	}

	@Test
	void testStringMethods() {
		assertEquals(true, Utils.nullOrEmpty(null));
		assertEquals(true, Utils.nullOrEmpty(""));
		assertEquals(false, Utils.nullOrEmpty("a"));

		assertEquals(true, Utils.parsesBlankOrNonPositive(null));
		assertEquals(true, Utils.parsesBlankOrNonPositive(""));
		assertEquals(true, Utils.parsesBlankOrNonPositive("-4.3"));
		assertEquals(true, Utils.parsesBlankOrNonPositive("0.0"));
		assertEquals(false, Utils.parsesBlankOrNonPositive("4.3"));
	}
}
