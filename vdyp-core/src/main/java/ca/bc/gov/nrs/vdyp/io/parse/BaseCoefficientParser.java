package ca.bc.gov.nrs.vdyp.io.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.MatrixMap;
import ca.bc.gov.nrs.vdyp.model.Region;

/**
 * Parses a Coefficient data file.
 *
 * @author Kevin Smith, Vivid Solutions
 *
 */
public abstract class BaseCoefficientParser<T extends Coefficients, M extends MatrixMap<T>>
		implements ResourceParser<M> {

	public static final String SP0_KEY = "sp0";
	public static final String REGION_KEY = "region";
	public static final String COEFFICIENTS_KEY = "coefficients";

	int numCoefficients;

	List<String> metaKeys = new ArrayList<>();
	List<Collection<?>> keyRanges = new ArrayList<>();

	public BaseCoefficientParser() {
		super();
		this.lineParser = new LineParser() {

			@Override
			public boolean isStopLine(String line) {
				return line.startsWith("   ");
			}

		};
	}

	public <K> BaseCoefficientParser<T, M>
			key(int length, String name, ValueParser<K> parser, Collection<K> range, String errorTemplate) {
		var validParser = ValueParser.validate(
				parser, (v) -> range.contains(v) ? Optional.empty() : Optional.of(String.format(errorTemplate, v))
		);
		lineParser.value(length, name, validParser);
		metaKeys.add(name);
		keyRanges.add(range);
		return this;
	}

	public BaseCoefficientParser<T, M> regionKey() {
		var regions = Arrays.asList(Region.values());
		return key(1, REGION_KEY, ValueParser.REGION, regions, "%s is not a valid region");
	}

	public BaseCoefficientParser<T, M> speciesKey(String name, Map<String, Object> controlMap) {
		var range = SP0DefinitionParser.getSpeciesAliases(controlMap);
		return key(2, name, String::strip, range, "%s is not a valid species");
	}

	public BaseCoefficientParser<T, M> speciesKey(Map<String, Object> controlMap) {
		return speciesKey(SP0_KEY, controlMap);
	}

	public BaseCoefficientParser<T, M> space(int length) {
		lineParser.space(length);
		return this;
	}

	public <K> BaseCoefficientParser<T, M> coefficients(int number, int length) {
		lineParser.multiValue(number, length, COEFFICIENTS_KEY, ValueParser.FLOAT);
		numCoefficients = number;
		return this;
	}

	protected LineParser lineParser;

	@Override
	public M parse(InputStream is, Map<String, Object> control) throws IOException, ResourceParseException {

		M result = createMap(keyRanges);

		lineParser.parse(is, result, (v, r) -> {
			var key = metaKeys.stream().map(v::get).collect(Collectors.toList()).toArray(Object[]::new);

			@SuppressWarnings("unchecked")
			var coe = getCoefficients((List<Float>) v.get(COEFFICIENTS_KEY));

			r.putM(coe, key);
			return r;
		});
		return result;
	}

	protected abstract M createMap(List<Collection<?>> keyRanges);

	protected abstract T getCoefficients(List<Float> coefficients);

}
