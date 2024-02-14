package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapSubResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.GrowthFiatDetails;
import ca.bc.gov.nrs.vdyp.model.Region;

public abstract class GrowthFiatParser implements ControlMapSubResourceParser<Map<Region, GrowthFiatDetails>> {
	
	public static final String REGION_ID_KEY = "RegionId";
	public static final String COEFFICIENTS_KEY = "Coefficients";

	public GrowthFiatParser() {
		
		this.lineParser = new LineParser() {
				@Override
				public boolean isStopLine(String line) {
					return line == null || line.substring(0, Math.min(3, line.length())).trim().length() == 0;
				}
			}
			.value(3, REGION_ID_KEY, ValueParser.INTEGER)
			.multiValue(11, 6, COEFFICIENTS_KEY, ValueParser.FLOAT);
	}

	private LineParser lineParser;

	@Override
	public Map<Region, GrowthFiatDetails> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		Map<Region, GrowthFiatDetails> result = new HashMap<>();
		
		lineParser.parse(is, result, (value, r, lineNumber) -> {
			var regionId = (Integer) value.get(REGION_ID_KEY);
			
			if (regionId != 1 && regionId != 2) {
				throw new ValueParseException(
						MessageFormat.format("Line {0}: region id is not recognized; the value must be 1 or 2"
								, lineNumber, regionId));
			}
			
			@SuppressWarnings("unchecked")
			var coefficients = (List<Float>) value.get(COEFFICIENTS_KEY);

			GrowthFiatDetails details = new GrowthFiatDetails(regionId, coefficients);

			if (details.getNAges() == 0) {
				throw new ValueParseException("0", "Region Id " + regionId + " contains no age ranges");
			}
			
			if (r.containsKey(details.getRegion())) {
				throw new ValueParseException(details.getRegion().name(), "Region Id " + details.getRegion().name() + " is present multiple times in the file");
			}
			
			r.put(details.getRegion(), details);

			return r;
		}, control);
		
		if (result.size() == 0) {
			throw new ResourceParseException("Details for Interior and Coastal regions missing");
		}
		else if (result.size() == 1 && result.containsKey(Region.COASTAL)) {
			throw new ResourceParseException("Details for Interior region missing");
		}
		else if (result.size() == 1) {
			throw new ResourceParseException("Details for Coastal region missing");
		}

		return result;
	}
}
