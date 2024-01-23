package ca.bc.gov.nrs.vdyp.io.parse;

import static ca.bc.gov.nrs.vdyp.common.Utils.constMap;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.isMarker;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.isValue;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.model.LayerType;

class ValueParserTest {

	@Test
	void testRangeParser() throws Exception {
		var exclusiveParser = ValueParser.range(ValueParser.INTEGER, 10, false, 20, false, "Test");

		var result = exclusiveParser.parse("11");
		assertThat(result, is(11));

		result = exclusiveParser.parse("19");
		assertThat(result, is(19));

		var ex = assertThrows(ValueParseException.class, () -> exclusiveParser.parse("10"));
		assertThat(ex, hasProperty("message", is("Test must be greater than 10.")));

		ex = assertThrows(ValueParseException.class, () -> exclusiveParser.parse("9"));
		assertThat(ex, hasProperty("message", is("Test must be greater than 10.")));

		ex = assertThrows(ValueParseException.class, () -> exclusiveParser.parse("20"));
		assertThat(ex, hasProperty("message", is("Test must be less than 20.")));

		ex = assertThrows(ValueParseException.class, () -> exclusiveParser.parse("21"));
		assertThat(ex, hasProperty("message", is("Test must be less than 20.")));

		var inclusiveParser = ValueParser.range(ValueParser.INTEGER, 10, true, 20, true, "Test");

		result = inclusiveParser.parse("11");
		assertThat(result, is(11));

		result = inclusiveParser.parse("19");
		assertThat(result, is(19));

		result = inclusiveParser.parse("10");
		assertThat(result, is(10));

		result = inclusiveParser.parse("20");
		assertThat(result, is(20));

		ex = assertThrows(ValueParseException.class, () -> inclusiveParser.parse("9"));
		assertThat(ex, hasProperty("message", is("Test must be greater than or equal to 10.")));

		ex = assertThrows(ValueParseException.class, () -> inclusiveParser.parse("21"));
		assertThat(ex, hasProperty("message", is("Test must be less than or equal to 20.")));

	}

	static enum TestEnum {
		VALUE1, VALUE2
	}

	@Test
	void testEnumParser() throws Exception {
		var parser = ValueParser.enumParser(TestEnum.class);

		assertThat(parser.parse("VALUE1"), is(TestEnum.VALUE1));
		assertThat(parser.parse("VALUE2"), is(TestEnum.VALUE2));

		var ex = assertThrows(ValueParseException.class, () -> parser.parse("FAKE"));
		assertThat(ex.getMessage(), is("\"FAKE\" is not a valid TestEnum"));

		ex = assertThrows(ValueParseException.class, () -> parser.parse(""));
		assertThat(ex.getMessage(), is("\"\" is not a valid TestEnum"));

		ex = assertThrows(ValueParseException.class, () -> parser.parse(" "));
		assertThat(ex.getMessage(), is("\"\" is not a valid TestEnum"));
	}

	@Test
	void testValidateRangeInclusive() throws Exception {
		var unit = ValueParser.validateRangeInclusive(10f, 20f, "Test");

		assertThat(unit.apply(10f), notPresent());
		assertThat(unit.apply(20f), notPresent());
		assertThat(unit.apply(11f), notPresent());
		assertThat(unit.apply(19f), notPresent());
		assertThat(unit.apply(9f), present(is("Test is expected to be between 10.0 and 20.0 but was 9.0")));
		assertThat(unit.apply(21f), present(is("Test is expected to be between 10.0 and 20.0 but was 21.0")));

	}

	@Test
	void testValidateRangeInclusiveBadRange() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> ValueParser.validateRangeInclusive(20f, 10f, "Test"));
	}

	@Test
	void testPretestOptional() throws Exception {
		var unit = ValueParser.pretestOptional(ValueParser.INTEGER, x -> !x.equals("IGNORE"));

		assertThat(unit.parse("1"), present(is(1)));
		assertThat(unit.parse("2"), present(is(2)));
		assertThat(unit.parse("IGNORE"), notPresent());
		assertThrows(ValueParseException.class, () -> unit.parse("DONT_IGNORE"));
	}

	@Test
	void testPosttestOptional() throws Exception {
		var unit = ValueParser.posttestOptional(ValueParser.INTEGER, x -> !x.equals(1));

		assertThat(unit.parse("0"), present(is(0)));
		assertThat(unit.parse("1"), notPresent());
		assertThat(unit.parse("2"), present(is(2)));
		assertThrows(ValueParseException.class, () -> unit.parse("DONT_IGNORE"));
	}

	@Test
	void testValueOrMarkerParser() throws Exception {
		var parser = ValueParser.valueOrMarker(ValueParser.INTEGER, (s) -> {
			if ("MARK".equals(s)) {
				return Optional.of("MARK");
			}
			return Optional.empty();
		});

		assertThat(parser.parse("MARK").getMarker(), present(is("MARK")));
		assertThat(parser.parse("MARK").getValue(), notPresent());

		assertThat(parser.parse("1").getValue(), present(is(1)));
		assertThat(parser.parse("1").getMarker(), notPresent());

		var ex = assertThrows(ValueParseException.class, () -> parser.parse("X"));
		assertThat(ex.getMessage(), is("\"X\" is not a valid Integer"));
	}

	@Test
	void testOptionalSingleton() throws Exception {
		var unit = ValueParser.optionalSingleton(x -> x.equals("PASS"), "X");

		assertThat(unit.parse("FAIL"), notPresent());
		assertThat(unit.parse("PASS"), present(is("X")));
	}

	@Test
	void testLayer() throws Exception {
		var unit = ValueParser.LAYER;

		assertThat(unit.parse("1"), present(is(LayerType.PRIMARY)));
		assertThat(unit.parse("P"), present(is(LayerType.PRIMARY)));

		assertThat(unit.parse("2"), present(is(LayerType.SECONDARY)));
		assertThat(unit.parse("S"), present(is(LayerType.SECONDARY)));

		assertThat(unit.parse("V"), present(is(LayerType.VETERAN)));

		assertThat(unit.parse(""), notPresent());
		assertThat(unit.parse(" "), notPresent());
		assertThat(unit.parse("X"), notPresent());
	}

	@Test
	void testMapParserSimple() throws Exception {
		var parser = ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), "A", "B", "C");

		var result = parser.parse("  1  2  3");

		assertThat(result, aMapWithSize(3));
		assertThat(result, hasEntry(is("A"), is(1)));
		assertThat(result, hasEntry(is("B"), is(2)));
		assertThat(result, hasEntry(is("C"), is(3)));

	}

	@Test
	void testMapParserMissingAValueError() throws Exception {
		var parser = ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), "A", "B", "C");

		var ex = assertThrows(ValueParseException.class, () -> parser.parse("  1  2"));
		assertThat(ex, hasProperty("message", is("Expected exactly 3 values but there were 2")));
	}

	@Test
	void testMapParserExtraAValueError() throws Exception {
		var parser = ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), "A", "B", "C");

		var ex = assertThrows(ValueParseException.class, () -> parser.parse("  1  2  3  4"));
		assertThat(ex, hasProperty("message", is("Expected exactly 3 values but there were 4")));
	}

	@Test
	void testMapParserWithDefaults() throws Exception {
		Map<String, Integer> defaults = new HashMap<>();
		defaults.put("C", 5);
		defaults.put("D", 6);

		var parser = ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), defaults, "A", "B", "C", "D");

		var result = parser.parse("  1  2  3");

		assertThat(result, aMapWithSize(4));
		assertThat(result, hasEntry(is("A"), is(1)));
		assertThat(result, hasEntry(is("B"), is(2)));
		assertThat(result, hasEntry(is("C"), is(3)));// 3 not 5
		assertThat(result, hasEntry(is("D"), is(6)));
	}

	@Test
	void testMapParserWithDefaultsToFew() throws Exception {
		Map<String, Integer> defaults = new HashMap<>();
		defaults.put("C", 5);
		defaults.put("D", 6);

		var parser = ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), defaults, "A", "B", "C", "D");

		var ex = assertThrows(ValueParseException.class, () -> parser.parse("  1"));
		assertThat(ex, hasProperty("message", is("Expected between 2 and 4 values but there were 1")));
	}

	@Test
	void testMapParserWithDefaultsToMany() throws Exception {
		Map<String, Integer> defaults = new HashMap<>();
		defaults.put("C", 5);
		defaults.put("D", 6);

		var parser = ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), defaults, "A", "B", "C", "D");

		var ex = assertThrows(ValueParseException.class, () -> parser.parse("  1  2  3  4  5"));
		assertThat(ex, hasProperty("message", is("Expected between 2 and 4 values but there were 5")));
	}

	@Test
	void testMapParserDefaultsMustComeAfterRequired() throws Exception {
		Map<String, Integer> defaults = new HashMap<>();
		defaults.put("C", 5);
		defaults.put("B", 6);

		var ex = assertThrows(
				IllegalArgumentException.class,
				() -> ValueParser.toMap(ValueParser.segmentList(3, ValueParser.INTEGER), defaults, "A", "B", "C", "D")
		);
		assertThat(ex, hasProperty("message", is("Keys with defaults must follow those without")));
	}

}
