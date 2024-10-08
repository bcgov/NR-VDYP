package ca.bc.gov.nrs.vdyp.io.parse.coe.base;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapSubResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ControlledValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;

/**
 * Base class for parsers of configuration files whose lines contain a BEC Zone Alias, an integer index, an indicator
 * flag and a list of 16 coefficients (one per species) and generates a MatrixMap2 of coefficients indexed by first BEC
 * Zone Alias and then sp0 Alias, one per index.
 *
 * Indices range from 0 to a parameterized value (7, for example).
 *
 * If, for a given input line, indicator is "one", coe i, i = 1 - 15, is set to coe 0 plus coe i. Otherwise (indicator
 * is "zero"), coe i is set to the ith coe supplied in the input file.
 *
 * @author Michael Junkin, Vivid Solutions
 * @see ControlMapSubResourceParser
 */
public abstract class BecZoneBySpeciesCoefficientParser
		implements ControlMapSubResourceParser<MatrixMap2<String, String, Coefficients>> {

	public static final String BEC_ZONE_ID_KEY = "BecId";
	public static final String INDEX_KEY = "index";
	public static final String INDICATOR_KEY = "indicator";
	public static final String COEFFICIENTS_KEY = "coefficients";

	private final int nCoefficients;
	private static final int NUM_SPECIES = 16;

	protected BecZoneBySpeciesCoefficientParser(int nCoefficients) {

		this.nCoefficients = nCoefficients;

		this.lineParser = new LineParser() {
			@Override
			public boolean isIgnoredLine(String line) {
				return Utils.nullOrPrefixBlank(line, 4);
			}

		} //
				.value(4, BEC_ZONE_ID_KEY, ControlledValueParser.BEC) //
				.space(2) //
				.value(
						1, INDEX_KEY,
						ValueParser.range(ValueParser.INTEGER, 0, true, nCoefficients, false, "Index value")
				) //
				.value(2, INDICATOR_KEY, ValueParser.range(ValueParser.INTEGER, 0, true, 1, true, "Indicator value")) //
				.multiValue(NUM_SPECIES, 8, COEFFICIENTS_KEY, ValueParser.FLOAT);
	}

	private LineParser lineParser;

	@Override
	public MatrixMap2<String, String, Coefficients> parse(InputStream is, Map<String, Object> control)
			throws IOException, ResourceParseException {

		var becAliases = BecDefinitionParser.getBecs(control).getBecAliases();
		var sp0Aliases = GenusDefinitionParser.getSpeciesAliases(control);
		MatrixMap2<String, String, Coefficients> result = new MatrixMap2Impl<>(
				becAliases, sp0Aliases, (k1, k2) -> Coefficients.empty(nCoefficients, 0)
		);

		lineParser.parse(is, result, (value, r, lineNumber) -> {
			var becZoneId = (String) value.get(BEC_ZONE_ID_KEY);
			var index = (int) value.get(INDEX_KEY);
			var indicator = (int) value.get(INDICATOR_KEY);

			@SuppressWarnings("unchecked")
			var specCoefficients = (List<Float>) value.get(COEFFICIENTS_KEY);

			var specIt = sp0Aliases.iterator();

			Float first = null;
			int coefficientIndex = 0;
			while (specIt.hasNext()) {
				var spec = specIt.next();

				Float coe = specCoefficients.get(coefficientIndex);
				if (coefficientIndex == 0) {
					first = coe;
				}

				Coefficients coefficients = r.get(becZoneId, spec);
				if (indicator == 0 || coefficientIndex == 0) {
					coefficients.setCoe(index, first);
				} else {
					coefficients.setCoe(index, first + coe);
				}

				coefficientIndex += 1;
			}

			return r;
		}, control);

		return result;
	}
}
