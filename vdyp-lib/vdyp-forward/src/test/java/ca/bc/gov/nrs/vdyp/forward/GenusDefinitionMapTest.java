package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.controlMapHasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import org.hamcrest.Matchers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.forward.test.VdypForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;

class GenusDefinitionMapTest {

	private ForwardControlParser parser;
	private Map<String, Object> controlMap;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@BeforeEach
	void before() throws IOException, ResourceParseException {

		parser = new ForwardControlParser();
		controlMap = VdypForwardTestUtils.parse(parser, "VDYP.CTR");
		assertThat(
				controlMap, (Matcher) controlMapHasEntry(
						ControlKey.SP0_DEF, Matchers.instanceOf(GenusDefinitionMap.class)
				)
		);
	}

	@Test
	void test() {
		var gdMap = (GenusDefinitionMap) controlMap.get(ControlKey.SP0_DEF.name());

		assertThat(gdMap.contains("AC"), is(true));
		assertThat(gdMap.getByAlias("AC"), hasProperty("alias", is("AC")));
		assertThat(gdMap.getByIndex(3), hasProperty("alias", is("B")));
		assertThat(gdMap.getIndexByAlias("B"), is(3));
		assertThat(gdMap.getNGenera(), is(16));
	}
}
