package ca.bc.gov.nrs.vdyp.fip;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.io.parse.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.ControlFileParserTest;
import ca.bc.gov.nrs.vdyp.io.parse.SP0DefinitionParser;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SP0Definition;
import ca.bc.gov.nrs.vdyp.model.SiteCurve;
import ca.bc.gov.nrs.vdyp.model.StockingClassFactor;

@SuppressWarnings("unused")
public class FipControlParserTest {

	@Test
	public void testParse() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseBec() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) hasEntry(is(BecDefinitionParser.CONTROL_KEY), 
				allOf(
						instanceOf(Map.class),
						hasEntry(
								is("AT"), 
								instanceOf(BecDefinition.class)))));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseSP0() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) hasEntry(is(SP0DefinitionParser.CONTROL_KEY), 
				allOf(
						instanceOf(List.class),
						hasItem(
								instanceOf(SP0Definition.class)))));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseVGRP() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.VOLUME_EQN_GROUPS), 
				allOf(            // Map of SP0 Aliases
					isA(Map.class), 
					hasEntry(
						isA(String.class),
						allOf(            // Map of BEC aliases
							isA(Map.class), 
							hasEntry(
								isA(String.class),
								isA(Integer.class) // Equation Identifier
		))))));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseDGRP() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.DECAY_GROUPS), 
				allOf(            // Map of SP0 Aliases
					isA(Map.class), 
					hasEntry(
						isA(String.class),
						allOf(            // Map of BEC aliases
							isA(Map.class), 
							hasEntry(
								isA(String.class),
								isA(Integer.class) // Equation Identifier
		))))));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseBGRP() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.BREAKAGE_GROUPS), 
				allOf(            // Map of SP0 Aliases
					isA(Map.class), 
					hasEntry(
						isA(String.class),
						allOf(            // Map of BEC aliases
							isA(Map.class), 
							hasEntry(
								isA(String.class),
								isA(Integer.class) // Equation Identifier
		))))));
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseGRBA1() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.DEFAULT_EQ_NUM), 
				allOf(            // Map of SP0 Aliases
					isA(Map.class), 
					hasEntry(
						isA(String.class),
						allOf(            // Map of BEC aliases
							isA(Map.class), 
							hasEntry(
								isA(String.class),
								isA(Integer.class) // Equation Identifier
		))))));
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseGMBA1() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.EQN_MODIFIERS), 
				allOf(            // Default Equation
					isA(Map.class), 
					hasEntry(
						isA(Integer.class),
						allOf(            // ITG
							isA(Map.class), 
							hasEntry(
								isA(Integer.class),
								isA(Integer.class) // Reassigned Equation
		))))));
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseSTK33() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.STOCKING_CLASS_FACTORS), 
				allOf(            // STK
					isA(Map.class), 
					hasEntry(
						isA(Character.class),
						allOf(            // Region
							isA(Map.class), 
							hasEntry(
								isA(Region.class),
								isA(StockingClassFactor.class) // Factors
		))))));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	@Disabled
	public void testParseE025() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.SITE_CURVE_NUMBERS), 
				allOf(            // Species
					isA(Map.class), 
					hasEntry(
						isA(String.class),
						isA(SiteCurve.class)
		))));
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testParseE025Empty() throws Exception {
		var parser = new FipControlParser();
		var result = parser.parse(ControlFileParserTest.class, "FIPSTART.CTR");
		assertThat(result, (Matcher) 
			hasEntry(
				is(FipControlParser.SITE_CURVE_NUMBERS), 
				Matchers.anEmptyMap()
		));
	}
}
