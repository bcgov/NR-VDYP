package ca.bc.gov.nrs.vdyp.io.parse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class LineParserTest {
	
	@Test
	public void testBasic() throws Exception {
		var parser = new LineParser();
		parser
			.string(3, "part1")
			.space(1)
			.string(4, "part2");
		
		var result1 = parser.parse("042 Blah");
		
		assertThat(result1, hasEntry("part1", "042"));
		assertThat(result1, hasEntry("part2", "Blah"));
		
	}
	
	@Test
	public void testNumbers() throws Exception {
		var parser = new LineParser();
		parser
			.integer(4, "part1")
			.space(1)
			.floating(5, "part2");
		
		var result1 = parser.parse(" 4   0.5  ");
		
		assertThat(result1, hasEntry("part1", 4));
		assertThat(result1, hasEntry("part2", 0.5f));
		
	}
	
	@Test
	public void testIncomplete() throws Exception {
		var parser = new LineParser();
		parser
			.integer(4, "part1")
			.space(1)
			.floating(5, "part2");
		
		var result1 = parser.parse(" 4  ");
		
		assertThat(result1, hasEntry("part1", 4));
		assertThat(result1, not(hasKey("part2")));
		
	}
	
	@Test
	public void testIncompleteSegment() throws Exception {
		var parser = new LineParser();
		parser
			.integer(4, "part1")
			.space(1)
			.floating(5, "part2");
		
		var result1 = parser.parse(" 4   5.0");
		
		assertThat(result1, hasEntry("part1", 4));
		assertThat(result1, hasEntry("part2", 5.0f));
		
	}
	
	@Test
	public void testNumberParseErrors() throws Exception {
		var parser = new LineParser();
		parser
			.integer(4, "part1")
			.space(1)
			.floating(5, "part2");
		
		var ex1 = assertThrows(ValueParseException.class, ()->parser.parse(" X   0.5  "));
		
		assertThat(ex1, hasProperty("value", is("X")));
		assertThat(ex1, hasProperty("cause", isA(NumberFormatException.class)));
		
		var ex2 = assertThrows(ValueParseException.class, ()->parser.parse(" 4   0.x  "));
		
		assertThat(ex2, hasProperty("value", is("0.x")));
		assertThat(ex2, hasProperty("cause", isA(NumberFormatException.class)));

		
	}
	
	@Test
	public void testValueParser() throws Exception {
		var parser = new LineParser();
		parser
			.parse(4, "part1", (s)->Integer.valueOf(s.strip())+1)
			.space(1)
			.parse("part2", (s)->Float.valueOf(s.strip())+1);
		
		var result1 = parser.parse(" 4   0.5  ");
		
		assertThat(result1, hasEntry("part1", 5));
		assertThat(result1, hasEntry("part2", 1.5f));
		
	}
	
	@Test
	public void testValueParserError() throws Exception {
		var parser = new LineParser();
		parser
			.parse(4, "part1", (s)->{throw new ValueParseException(s, "Testing");})
			.space(1)
			.parse(4, "part2", (s)->Float.valueOf(s.strip())+1);
		
		var ex1 = assertThrows(ValueParseException.class, ()->parser.parse(" X   0.5  "));
		assertThat(ex1, hasProperty("value", is(" X  ")));
		assertThat(ex1, hasProperty("message", is("Testing")));
		
	}

	@Test
	public void testUnbounded() throws Exception {
		var parser = new LineParser();
		parser
			.string(4, "part1")
			.string("part2");
		
		var result1 = parser.parse("123  67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890 ");
		
		assertThat(result1, hasEntry("part1", "123 "));
		assertThat(result1, hasEntry("part2", " 67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890 "));
		
	}
	
	@Test
	public void testStripped() throws Exception {
		var parser = new LineParser();
		parser
			.strippedString(4, "part1")
			.strippedString("part2");
		
		var result1 = parser.parse("123  67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890 ");
		
		assertThat(result1, hasEntry("part1", "123"));
		assertThat(result1, hasEntry("part2", "67890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"));
		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLine() throws Exception {
		var parser = new LineParser();
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				),
				allOf(
						(Matcher)hasEntry("part1", 43),
						(Matcher)hasEntry("part2", "Value2")
				)
			));
	}
	
	@Test
	public void testMultiLineException() throws Exception {
		var parser = new LineParser();
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n004x Value2".getBytes());
		) {
			
			var ex1 = assertThrows(ResourceParseException.class, ()-> parser.parse(is));
			
			assertThat(ex1, hasProperty("line", is(2))); // Line numbers indexed from 1 so the error is line 2
			assertThat(ex1, hasProperty("cause", isA(ValueParseException.class)));
			assertThat(ex1, hasProperty("cause", hasProperty("value", is("004x"))));
			assertThat(ex1, hasProperty("cause", hasProperty("cause", isA(NumberFormatException.class))));

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLineWithStopEntry() throws Exception {
		var parser = new LineParser() {
			
			@Override
			public boolean isStopEntry(Map<String, Object> entry) {
				return 0 == (int) entry.get("part1");
			}
			
		};
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0000\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				)
			));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLineWithStopLine() throws Exception {
		var parser = new LineParser() {
			
			@Override
			public boolean isStopLine(String line) {
				return line.length()>4 && 'X' == Character.toUpperCase(line.charAt(4));
			}
			
		};
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0000X\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				)
			));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLineWithStopSegment() throws Exception {
		var parser = new LineParser() {
			
			@Override
			public boolean isStopSegment(List<String> segments) {
				return 'X' == Character.toUpperCase(segments.get(1).charAt(0));
			}
			
		};
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0000X\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				)
			));
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLineWithIgnoredEntry() throws Exception {
		var parser = new LineParser() {
			
			@Override
			public boolean isIgnoredEntry(Map<String, Object> entry) {
				return 0 == (int) entry.get("part1");
			}
			
		};
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0000\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				),
				allOf(
						(Matcher)hasEntry("part1", 43),
						(Matcher)hasEntry("part2", "Value2")
				)
			));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLineWithIgnoredLine() throws Exception {
		var parser = new LineParser() {
			
			@Override
			public boolean isIgnoredLine(String line) {
				return line.length()>4 && 'X' == Character.toUpperCase(line.charAt(4));
			}
			
		};
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0000X\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				),
				allOf(
						(Matcher)hasEntry("part1", 43),
						(Matcher)hasEntry("part2", "Value2")
				)
			));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testMultiLineWithIgnoredSegment() throws Exception {
		var parser = new LineParser() {
			
			@Override
			public boolean isIgnoredSegment(List<String> segments) {
				return 'X' == Character.toUpperCase(segments.get(1).charAt(0));
			}
			
		};
		parser
			.integer(4, "part1")
			.space(1)
			.string("part2");
		
		List<Map<String, Object>> result = new ArrayList<>();
		try(
			var is = new ByteArrayInputStream("0042 Value1\r\n0000X\r\n0043 Value2".getBytes());
		) {
			result = parser.parse(is);
		}
		
		assertThat(result, contains(
				allOf(
						(Matcher)hasEntry("part1", 42),
						(Matcher)hasEntry("part2", "Value1")
				),
				allOf(
						(Matcher)hasEntry("part1", 43),
						(Matcher)hasEntry("part2", "Value2")
				)
			));
	}

}
