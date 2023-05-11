package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parser for a BEC Definition data file
 * 
 * @author Kevin Smith, Vivid Solutions
 *
 */
public class BecDefinitionParser implements ResourceParser<Map<String, BecDefinition>>{
	
	LineParser lineParser = new LineParser() {

		@Override
		public boolean isStopSegment(List<String> segments) {
			return "Z".equalsIgnoreCase(segments.get(2));
		}
		
	}
		.strippedString(4, "alias")
		.space(1)
		.value(1, "region", (s)->{
			return Region.fromAlias(Character.toUpperCase(s.charAt(0)))
					.orElseThrow(()->new ValueParseException(s, s+" is not a valid region identifier"));
		})
		.space(1)
		.strippedString("name");
	
	@Override
	public Map<String, BecDefinition> parse(InputStream is) throws IOException, ResourceParseException {
		Map<String, BecDefinition> result = new HashMap<>();
		result = lineParser.parse(is, result, (v, r)->{
			String alias = (String) v.get("alias");
			Region region = (Region) v.get("region");
			String name = (String) v.get("name");
			var defn = new BecDefinition(alias, region, name);
			r.put(alias, defn);
			return r;
		});
		
		return Collections.unmodifiableMap(result);
	}
	
}
