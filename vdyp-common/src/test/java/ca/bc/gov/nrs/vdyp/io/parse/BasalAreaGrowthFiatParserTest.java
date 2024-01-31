package ca.bc.gov.nrs.vdyp.io.parse;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.model.GrowthFiatDetails;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

public class BasalAreaGrowthFiatParserTest {

	@Test
	void testParseSimple() throws Exception {

		var parser = new BasalAreaGrowthFiatParser();

		Map<String, Object> controlMap = new HashMap<>();
		
		TestUtils.populateControlMapFromResource(controlMap, parser, "EMP111A1.PRM");
		
		@SuppressWarnings("unchecked")
		Map<Region, GrowthFiatDetails> m = (Map<Region, GrowthFiatDetails>)controlMap.get(BasalAreaGrowthFiatParser.CONTROL_KEY);
		
		assertThat(m, Matchers.aMapWithSize(2));
		assertThat(m.get(Region.COASTAL),
				Matchers.allOf(
						Matchers.hasProperty("ages", Matchers.arrayContaining(1.0f, 0.02f, 100.0f, 0.01f)),
						Matchers.hasProperty("coefficients", Matchers.arrayContaining(200f, 0.0f, 0.0f, 0.0f)),
						Matchers.hasProperty("mixedCoefficients", Matchers.arrayContaining(100.0f, 150.0f, 1.0f))));
		assertThat(m.get(Region.INTERIOR),
				Matchers.allOf(
						Matchers.hasProperty("ages", Matchers.arrayContaining(1.0f, 0.02f, 100.0f, 0.01f)),
						Matchers.hasProperty("coefficients", Matchers.arrayContaining(200f, 0.0f, 0.0f, 0.0f)),
						Matchers.hasProperty("mixedCoefficients", Matchers.arrayContaining(100.0f, 150.0f, 1.0f))));
	}
	
	@Test
	void testParseMissingInteriorRegion() throws Exception {

		var parser = new BasalAreaGrowthFiatParser();

		var is = TestUtils.makeInputStream(
				"  1     1   .02   100   .01   200     0     0     0   100   150   1.0"
		);
		
		Map<String, Object> controlMap = new HashMap<>();

		try {
			parser.parse(is, controlMap);
			Assertions.fail();
		}
		catch (ResourceParseException e) {
			assertThat(e, Matchers.hasProperty("message", Matchers.is("Details for Interior region missing")));
		}
	}
	
	@Test
	void testParseMissingCoastalRegion() throws Exception {

		var parser = new BasalAreaGrowthFiatParser();

		var is = TestUtils.makeInputStream(
				"  2     1   .02   100   .01   200     0     0     0   100   150   1.0"
		);
		
		Map<String, Object> controlMap = new HashMap<>();

		try {
			parser.parse(is, controlMap);
			Assertions.fail();
		}
		catch (ResourceParseException e) {
			assertThat(e, Matchers.hasProperty("message", Matchers.is("Details for Coastal region missing")));
		}
	}
	
	@Test
	void testParseMissingRegions() throws Exception {

		var parser = new BasalAreaGrowthFiatParser();

		var is = TestUtils.makeInputStream(
		);
		
		Map<String, Object> controlMap = new HashMap<>();

		try {
			parser.parse(is, controlMap);
			Assertions.fail();
		}
		catch (ResourceParseException e) {
			assertThat(e, Matchers.hasProperty("message", Matchers.is("Details for Interior and Coastal regions missing")));
		}
	}
	
	@Test
	void testParseMissingAges() throws Exception {

		var parser = new BasalAreaGrowthFiatParser();

		var is = TestUtils.makeInputStream(
				"  1   0.0   0.0   0.0   0.0     0     0     0     0   100   150   1.0",
				"  2     1   .02   100   .01   200     0     0     0   100   150   1.0"
		);
		
		Map<String, Object> controlMap = new HashMap<>();

		try {
			parser.parse(is, controlMap);
			Assertions.fail();
		}
		catch (ResourceParseLineException e) {
			assertThat(e, Matchers.hasProperty("message", Matchers.is("Error at line 1: Region Id 1 contains no age ranges")));
		}
	}
	
	@Test
	void testDuplicatedLines() throws Exception {

		var parser = new BasalAreaGrowthFiatParser();

		var is = TestUtils.makeInputStream(
				"  1     1   .02   100   .01   200     0     0     0   100   150   1.0",
				"  2     1   .02   100   .01   200     0     0     0   100   150   1.0",
				"  2     1   .02   100   .01   200     0     0     0   100   150   1.0"
		);
		
		Map<String, Object> controlMap = new HashMap<>();

		try {
			parser.parse(is, controlMap);
			Assertions.fail();
		}
		catch (ResourceParseLineException e) {
			assertThat(e, Matchers.hasProperty("message", Matchers.is("Error at line 3: Region Id INTERIOR is present multiple times in the file")));
		}
	}
}
