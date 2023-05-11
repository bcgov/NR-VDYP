package ca.bc.gov.nrs.vdyp.io.parse;

import static ca.bc.gov.nrs.vdyp.test.VydpMatchers.parseAs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class ControlFileParserTest {

	@Test
	void testParsesEntriesSimple() throws Exception {
		var parser = makeParser();
		
		String file = "001 Control";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), contains(controlEntry(equalTo(1), equalTo(" "), equalTo("Control"))));
		}
	}
	
	@Test
	void testParsesEntriesSpacePadding() throws Exception {
		var parser = makeParser();

		String file = "  1 Control";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), contains(controlEntry(equalTo(1), equalTo(" "), equalTo("Control"))));
		}
	}

	@Test
	void testParsesEntriesExtended() throws Exception {
		var parser = makeParser();

		String file = "001XControl that is longer than 50 characters. Blah Blah Blah Blah.";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(
					result.entrySet(),
					contains(
							controlEntry(
									equalTo(1), equalTo("X"), equalTo("Control that is longer than 50 characters. Blah Blah Blah Blah.")
							)
					)
			);
		}
	}

	@Test
	void testParsesEntriesExtendedAlternate() throws Exception {
		var parser = makeParser();

		String file = "001>Control that is longer than 50 characters. Blah Blah Blah Blah.";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(
					result.entrySet(),
					contains(
							controlEntry(
									equalTo(1), equalTo(">"), equalTo("Control that is longer than 50 characters. Blah Blah Blah Blah.")
							)
					)
			);
		}
	}

	@Test
	void testParsesEntriesWithDistantComment() throws Exception {
		var parser = makeParser();

		String file = "001 Control                                           Comment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(
					result.entrySet(),
					contains(
							controlEntry(equalTo(1), equalTo(" "), equalTo("Control"))
					)
			);
		}
	}

	@Test
	void testParsesEntriesExtendedWithDistantComment() throws Exception {
		var parser = makeParser();

		String file = "001XControl                                                                                                                 Comment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(
					result.entrySet(),
					contains(
							controlEntry(
									equalTo(1), equalTo("X"),
									equalTo(
											"Control"
									)
							)
					)
			);
		}
	}

	@Test
	void testParsesEntriesWithMarkedComment() throws Exception {
		var parser = makeParser();

		String file = "001 Control!Comment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), contains(controlEntry(equalTo(1), equalTo(" "), equalTo("Control"))));
		}
	}

	@Test
	void testParsesEntriesExtendedWithMarkedComment() throws Exception {
		var parser = makeParser();

		String file = "001XControl!Comment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), contains(controlEntry(equalTo(1), equalTo("X"), equalTo("Control"))));
		}
	}

	@Test
	void testParsesEntriesIgnoreCommentLinesByExtendedMarker() throws Exception {
		var parser = makeParser();

		String file = "001CComment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), empty());
		}
	}

	@Test
	void testParsesEntriesIgnoreCommentLinesByZeroIndex() throws Exception {
		var parser = makeParser();

		String file = "000 Comment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), empty());
		}
	}

	@Test
	void testParsesEntriesIgnoreCommentLinesByNullIndex() throws Exception {
		var parser = makeParser();

		String file = "    Comment";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), empty());
		}
	}

	@Test
	void testParsesEntriesIgnoreEmptyLines() throws Exception {
		var parser = makeParser();

		String file = "\n \n  \n   \n    ";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(result.entrySet(), empty());
		}
	}

	@Test
	void testParsesMultipleEntries() throws Exception {
		var parser = makeParser();

		String file = "001 Control 1\n002 Control 2";
		try (InputStream is = new ByteArrayInputStream(file.getBytes())) {
			var result = parser.parse(is);

			assertThat(
					result.entrySet(),
					contains(
							controlEntry(equalTo(1), equalTo(" "), equalTo("Control 1")),
							controlEntry(equalTo(2), equalTo(" "), equalTo("Control 2"))
					)
			);
		}
	}

	@Test
	void testParseToMap() throws Exception {
		var parser = makeParser();
		String file = 
				  "097 coe\\vetdq2.dat                                    DQ for Vet layer           RD_YDQV\n"
				+ "098 coe\\REGBAV01.coe                                  VET BA, IPSJF168.doc       RD_E098\n"
				+ "\n"
				+ "197    5.0   0.0   2.0                                Minimum Height, Minimum BA, Min BA fully stocked.\n"
				+ "\n"
				+ "198 coe\\MOD19813.prm                                  Modifier file (IPSJF155, XII) RD_E198\n"
				+ "199  0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 Debug switches (0 by default) See IPSJF155  App IX                              Debug switches (25) 0=default See IPSJF155, App IX\n"
				+ "                                                      1st:  1: Do NOT apply BA limits from SEQ043\n"
				+ "                                                      2nd:  1: Do NOT apply DQ limits from SEQ043\n";
		try(
			var is = new ByteArrayInputStream(file.getBytes());
		) {
			var result = parser.parse(is);

			
			assertThat(result, hasEntry(equalTo("097"), equalTo("coe\\vetdq2.dat")));
			
			assertThat(result, hasEntry(equalTo("098"), equalTo("coe\\REGBAV01.coe")));
			assertThat(result, hasEntry(equalTo("197"), equalTo("5.0   0.0   2.0")));
			assertThat(result, hasEntry(equalTo("198"), equalTo("coe\\MOD19813.prm")));
			assertThat(result, hasEntry(equalTo("199"), equalTo("0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0")));
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void testParseToMapWithConfiguration() throws Exception {
		var parser = makeConfiguredParser();
		String file = 
				  "097 coe\\vetdq2.dat                                    DQ for Vet layer           RD_YDQV\n"
				+ "098 coe\\REGBAV01.coe                                  VET BA, IPSJF168.doc       RD_E098\n"
				+ "\n"
				+ "197    5.0   0.0   2.0                                Minimum Height, Minimum BA, Min BA fully stocked.\n"
				+ "\n"
				+ "198 coe\\MOD19813.prm                                  Modifier file (IPSJF155, XII) RD_E198\n"
				+ "199  0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 Debug switches (0 by default) See IPSJF155  App IX                              Debug switches (25) 0=default See IPSJF155, App IX\n"
				+ "                                                      1st:  1: Do NOT apply BA limits from SEQ043\n"
				+ "                                                      2nd:  1: Do NOT apply DQ limits from SEQ043\n";
		try(
			var is = new ByteArrayInputStream(file.getBytes());
		) {
			var result = parser.parse(is);

			
			assertThat(result, hasEntry(equalTo("097"), equalTo("coe\\vetdq2.dat")));
			
			assertThat(result, hasEntry(equalTo("098"), equalTo("coe\\REGBAV01.coe")));
			assertThat(result, hasEntry(equalTo("minimums"), (Matcher)contains(5.0f, 0.0f, 2.0f)));
			assertThat(result, hasEntry(equalTo("modifier_file"), equalTo("coe\\MOD19813.prm")));
			assertThat(result, hasEntry(equalTo("debugSwitches"), (Matcher)contains(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0)));
		}
	}
	
	@Test
	void testParseToMapLastOfDuplicates() throws Exception {
		var parser = makeParser();
		String file = 
				  "097 value1\n"
				+ "097 value2\n";
		try(
			var is = new ByteArrayInputStream(file.getBytes());
		) {
			var result = parser.parse(is);

			
			assertThat(result, hasEntry(equalTo("097"), equalTo("value2")));
		}
	}

	
	private static Matcher<Map.Entry<String, Object>> controlEntry(Matcher<Integer> index, Matcher<String> extend, Matcher<String> control) {
		
		return allOf(hasProperty("key", parseAs(index, Integer::valueOf)), hasProperty("value", control));
	}

	private ControlFileParser makeParser() {
		return new ControlFileParser();
	}
	
	private ControlFileParser makeConfiguredParser() {
		var parser = makeParser();
		
		var identifiers = new HashMap<Integer, String>();
		var parsers = new HashMap<Integer, Function<String, ?>>();

		identifiers.put(197, "minimums");
		parsers.put(197, (String s) -> Arrays.stream(s.strip().split("\s+")).map(Float::valueOf).collect(Collectors.toList()));

		identifiers.put(198, "modifier_file");

		identifiers.put(199, "debugSwitches");
		parsers.put(199, (String s) -> Arrays.stream(s.strip().split("\s+")).map(Integer::valueOf).collect(Collectors.toList()));

		parser.setIdentifiers(identifiers);
		parser.setValueParsers(parsers);
		
		return parser;
	}

}
