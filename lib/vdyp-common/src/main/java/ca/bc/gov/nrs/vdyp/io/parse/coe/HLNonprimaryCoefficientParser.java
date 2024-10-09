package ca.bc.gov.nrs.vdyp.io.parse.coe;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.io.parse.common.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapSubResourceParser;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parses an HL Nonprimary Coefficient data file.
 *
 * This file contains a (possibly incomplete) mapping of species aliases <b>x</b> species aliases <b>x</b> regions to a
 * (one-based) list of two coefficients. Each row contains:
 * <ol>
 * <li>(cols 0-1) non-primary species alias</li>
 * <li>(cols 3-4) primary species alias</li>
 * <li>(col 6) region character ('I' or 'C')</li>
 * <li>(col 8) an equation number</li>
 * <li>(cols 9-18, 19-28) two coefficients</li>
 * </ol>
 * Lines where both Species aliases are empty are considered blank lines and are skipped. All lines are read.
 * <p>
 * The result of the parse is a {@link MatrixMap3} of (Coefficients, equation number) pairs indexed by species 1,
 * species 2 and then region.
 * <p>
 * FIP Control index: 053
 * <p>
 * Example: coe/REGHL.COE
 *
 * @author Kevin Smith, Vivid Solutions
 * @see ControlMapSubResourceParser
 */
public class HLNonprimaryCoefficientParser
		implements ControlMapSubResourceParser<MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>> {

	public static final int NUM_COEFFICIENTS = 2;

	public static final String NON_PRIMARY_SPECIES_KEY = "nonPrimarySpecies";
	public static final String PRIMARY_SPECIES_KEY = "primarySpecies";
	public static final String REGION_KEY = "region";
	public static final String EQUATION_KEY = "ieqn";
	public static final String COEFFICIENT_KEY = "coefficient";

	public HLNonprimaryCoefficientParser() {
		super();
		this.lineParser = new LineParser() {

			@Override
			public boolean isIgnoredLine(String line) {
				return (line.length() < 6 && line.isBlank()) || line.substring(0, 6).isBlank();
			}

		}.value(2, NON_PRIMARY_SPECIES_KEY, ValueParser.STRING).space(1) //
				.value(2, PRIMARY_SPECIES_KEY, ValueParser.STRING).space(1) //
				.value(1, REGION_KEY, ValueParser.REGION).space(1) //
				.integer(1, EQUATION_KEY) //
				.multiValue(NUM_COEFFICIENTS, 10, COEFFICIENT_KEY, ValueParser.FLOAT);
	}

	LineParser lineParser;

	@Override
	public MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>
			parse(InputStream is, Map<String, Object> control) throws IOException, ResourceParseException {
		final var regionIndicies = Arrays.asList(Region.values());
		final var speciesIndicies = GenusDefinitionParser.getSpeciesAliases(control);

		MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> result = new MatrixMap3Impl<>(
				speciesIndicies, speciesIndicies, regionIndicies, MatrixMap3Impl.emptyDefault()
		);
		lineParser.parse(is, result, (value, r, line) -> {
			var sp0_1 = (String) value.get(NON_PRIMARY_SPECIES_KEY);
			var sp0_2 = (String) value.get(PRIMARY_SPECIES_KEY);
			var ieqn = (Integer) value.get(EQUATION_KEY);
			var region = (Region) value.get(REGION_KEY);
			@SuppressWarnings("unchecked")
			var coefficients = (List<Float>) value.get(COEFFICIENT_KEY);
			GenusDefinitionParser.checkSpecies(speciesIndicies, sp0_1);
			GenusDefinitionParser.checkSpecies(speciesIndicies, sp0_2);

			if (coefficients.size() < NUM_COEFFICIENTS) {
				throw new ValueParseException(
						MessageFormat.format("{0}, {1}, {2}", sp0_1, sp0_2, region),
						"Expected 2 coefficients but saw " + coefficients.size()
				);
			}

			if (ieqn != 1 && ieqn != 2) {
				throw new ValueParseException(
						MessageFormat.format("{0}, {1}, {2}", sp0_1, sp0_2, region),
						"Equation number " + ieqn + " is not 1 or 2"
				);
			}

			r.put(sp0_1, sp0_2, region, Optional.of(new NonprimaryHLCoefficients(coefficients, ieqn)));

			return r;
		}, control);
		return result;
	}

	@Override
	public ControlKey getControlKey() {
		return ControlKey.HL_NONPRIMARY;
	}

}
