package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse a file with records consisting of lines with fixed width fields.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class LineParser {

	private List<LineParserSegment> segments = new ArrayList<>();
	public static final Charset charset = StandardCharsets.US_ASCII;
	public static final String LINE_NUMBER_KEY = "_PARSER_LINE_NUMBER";

	static private abstract class LineParserSegment {
		int length;

		public int getLength() {
			return length;
		}

		public LineParserSegment(int length) {
			super();
			this.length = length;
		}

		public abstract void parseIntoMap(String toParse, Map<String, Object> map) throws ValueParseException;
	}

	static private class LineParserNullSegment extends LineParserSegment {

		public LineParserNullSegment(int length) {
			super(length);
		}

		@Override
		public void parseIntoMap(String toParse, Map<String, Object> map) {
			// do nothing
		}
	}

	static abstract class LineParserValueSegment<T> extends LineParserSegment {
		String name;

		abstract T parse(String value) throws ValueParseException;

		public String getName() {
			return name;
		}

		@Override
		public void parseIntoMap(String toParse, Map<String, Object> map) throws ValueParseException {
			var value = this.parse(toParse);
			map.put(this.getName(), value);
		}

		public LineParserValueSegment(int length, String name) {
			super(length);
			this.name = name;
		}
	}

	/**
	 * ignore a segment of characters
	 *
	 * @param length
	 * @return
	 */
	public LineParser space(int length) {
		segments.add(new LineParserNullSegment(length));
		return this;
	}

	/**
	 * A decimal integer segment
	 */
	public LineParser integer(int length, String name) {
		return this.value(length, name, ValueParser.INTEGER);
	}

	/**
	 * A floating point segment
	 */
	public LineParser floating(int length, String name) {
		return this.value(length, name, ValueParser.FLOAT);
	}

	/**
	 * A string segment
	 */
	public LineParser string(int length, String name) {
		return this.value(length, name, s -> s);
	}

	/**
	 * A string segment with no bounds. No further segments may be added.
	 */
	public LineParser string(String name) {
		return this.value(name, s -> s);
	}

	/**
	 * A string segment stripped of leading and trailing whitespace.
	 */
	public LineParser strippedString(int length, String name) {
		return this.value(length, name, String::strip);
	}

	/**
	 * A string segment stripped of leading and trailing whitespace with no bounds.
	 * No further segments may be added.
	 */
	public LineParser strippedString(String name) {
		return this.value(name, String::strip);
	}

	/**
	 * Add a multipart segment
	 *
	 * @param <T>    type the segment will be parsed too
	 * @param length length of the segment
	 * @param name   name of the segment
	 * @param parser parser to convert the string
	 */
	public <T> LineParser multiValue(int number, int length, String name, ValueParser<T> parser) {
		if (number < 0)
			throw new IllegalArgumentException("number can not be negative");
		if (length < 0)
			throw new IllegalArgumentException("length can not be negative");

		return doValue(length * number, name, ValueParser.segmentList(length, parser));
	}

	/**
	 * Add a segment
	 *
	 * @param <T>    type the segment will be parsed too
	 * @param length length of the segment
	 * @param name   name of the segment
	 * @param parser parser to convert the string
	 */
	public <T> LineParser value(int length, String name, ValueParser<T> parser) {
		if (length < 0)
			throw new IllegalArgumentException("length can not be negative");
		return doValue(length, name, parser);
	}

	/**
	 * Add an unbounded segment. No further segments may be added.
	 *
	 * @param <T>    type the segment will be parsed too
	 * @param length length of the segment
	 * @param name   name of the segment
	 * @param parser parser to convert the string
	 */
	public <T> LineParser value(String name, ValueParser<T> parser) {
		return doValue(-1, name, parser);
	}

	private <T> LineParser doValue(int length, String name, ValueParser<T> parser) {
		if (segments.size() > 0 && segments.get(segments.size() - 1).length < 0)
			throw new IllegalStateException("Can not add a segment after an unbounded segment");
		segments.add(new LineParserValueSegment<T>(length, name) {

			@Override
			T parse(String value) throws ValueParseException {
				return parser.parse(value);
			}

		});
		return this;
	}

	private List<String> segmentize(String line) {
		List<String> result = new ArrayList<>(segments.size());

		int i = 0;
		for (var segment : segments) {
			if (i >= line.length()) {
				result.add(null);
				continue;
			}
			;
			String segmentString;
			if (segment.getLength() >= 0 && i + segment.getLength() < line.length()) {
				segmentString = line.substring(i, i + segment.length);
			} else {
				segmentString = line.substring(i);
			}
			i += segmentString.length();
			result.add(segmentString);
		}

		return result;
	}

	/**
	 * Parse an individual line
	 *
	 * @param line
	 * @return
	 * @throws ValueParseException
	 */
	public Map<String, Object> parseLine(String line) throws ValueParseException {
		var segments = segmentize(line);
		return parse(segments);
	}

	private Map<String, Object> parse(List<String> segmentStrings) throws ValueParseException {
		if (segmentStrings.size() != segments.size()) {
			throw new IllegalStateException("segment strings and segment handlers must have the same size");
		}

		var result = new HashMap<String, Object>();

		for (int i = 0; i < segments.size(); i++) {
			var segmentHandler = segments.get(i);
			var segmentString = segmentStrings.get(i);
			if (segmentString != null) {
				segmentHandler.parseIntoMap(segmentString, result);
			}
		}

		return result;
	}

	/**
	 * Parse an input stream
	 *
	 * @param <T>         Type of the resulting object
	 * @param is          Input stream to parse
	 * @param result      Starting state for the resulting object
	 * @param addToResult Add a record from the file to the result object and return
	 *                    it
	 * @return The result object after parsing
	 * @throws IOException                if an error occurs while reading from the
	 *                                    stream
	 * @throws ResourceParseLineException if the content of the stream could not be
	 *                                    parsed
	 */
	public <T> T parse(InputStream is, T result, ParseEntryHandler<Map<String, Object>, T> addToResult)
			throws IOException, ResourceParseLineException {
		var reader = new BufferedReader(new InputStreamReader(is, charset));
		String line;
		int lineNumber = 0;
		while ( (line = reader.readLine()) != null) {
			lineNumber++;
			try {
				if (isStopLine(line)) {
					break;
				}
				if (isIgnoredLine(line)) {
					continue;
				}
				var segments = segmentize(line);
				if (isStopSegment(segments)) {
					break;
				}
				if (isIgnoredSegment(segments)) {
					continue;
				}
				var entry = parse(segments);
				entry.put(LINE_NUMBER_KEY, lineNumber);
				if (isStopEntry(entry)) {
					break;
				}
				if (isIgnoredEntry(entry)) {
					continue;
				}
				result = addToResult.addTo(entry, result);
			} catch (ValueParseException ex) {
				throw new ResourceParseLineException(lineNumber, ex);
			}
		}
		return result;
	}

	/**
	 * Parse an input stream into a list of maps
	 *
	 * @param is Input stream to parse
	 * @return A list of maps, one per line of the stream
	 * @throws IOException                if an error occurs while reading from the
	 *                                    stream
	 * @throws ResourceParseLineException if the content of the stream could not be
	 *                                    parsed
	 */
	public List<Map<String, Object>> parse(InputStream is) throws IOException, ResourceParseLineException {
		var result = new ArrayList<Map<String, Object>>();
		result = this.parse(is, result, (v, r) -> {
			r.add(v);
			return r;
		});
		return result;
	}

	/**
	 * If this returns true for a parsed line, parsing will stop and that line will
	 * not be included in the result.
	 */
	protected boolean isStopEntry(Map<String, Object> entry) {
		return false;
	}

	/**
	 * If this returns true for a segmented line, parsing will stop and that line
	 * will not be included in the result.
	 */
	public boolean isStopSegment(List<String> entry) {
		return false;
	}

	/**
	 * If this returns true for an unparsed line, parsing will stop and that line
	 * will not be included in the result.
	 */
	public boolean isStopLine(String line) {
		return false;
	}

	/**
	 * If this returns true for a parsed line, that line will not be included in the
	 * result.
	 */
	public boolean isIgnoredEntry(Map<String, Object> entry) {
		return false;
	}

	/**
	 * If this returns true for a segmented line, that line will not be included in
	 * the result.
	 */
	public boolean isIgnoredSegment(List<String> entry) {
		return false;
	}

	/**
	 * If this returns true for an unparsed line, that line will not be included in
	 * the result.
	 */
	public boolean isIgnoredLine(String line) {
		return false;
	}
}
