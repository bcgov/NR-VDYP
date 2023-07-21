package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ca.bc.gov.nrs.vdyp.common.ValueOrMarker;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.EndOfRecord;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.AbstractStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.ControlledValueParser;
import ca.bc.gov.nrs.vdyp.io.parse.GroupingStreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.LineParser;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseValidException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.ValueParser;
import ca.bc.gov.nrs.vdyp.model.Layer;

public class FipSpeciesParser
		implements ControlMapValueReplacer<StreamingParserFactory<Collection<FipSpecies>>, String> {

	public static final String CONTROL_KEY = "FIP_SPECIES";

	static final String POLYGON_IDENTIFIER = "POLYGON_IDENTIFIER"; // POLYDESC
	static final String LAYER = "LAYER"; // LAYER

	static final String GENUS = "GENUS"; // SP0
	static final String PERCENT_GENUS = "PERCENT_GENUS"; // PTVSP0

	static final String SPECIES_1 = "SPECIES_1"; // SP641
	static final String PERCENT_SPECIES_1 = "PERCENT_SPECIES_1"; // PCT1
	static final String SPECIES_2 = "SPECIES_2"; // SP642
	static final String PERCENT_SPECIES_2 = "PERCENT_SPECIES_2"; // PCT2
	static final String SPECIES_3 = "SPECIES_3"; // SP643
	static final String PERCENT_SPECIES_3 = "PERCENT_SPECIES_3"; // PCT3
	static final String SPECIES_4 = "SPECIES_4"; // SP644
	static final String PERCENT_SPECIES_4 = "PERCENT_SPECIES_4"; // PCT4

	@Override
	public String getControlKey() {
		return CONTROL_KEY;
	}

	@Override
	public StreamingParserFactory<Collection<FipSpecies>>
			map(String fileName, FileResolver fileResolver, Map<String, Object> control)
					throws IOException, ResourceParseException {
		return () -> {
			var lineParser = new LineParser().strippedString(25, POLYGON_IDENTIFIER).space(1).value(
					1, LAYER,
					ValueParser.valueOrMarker(
							ValueParser.LAYER, ValueParser.optionalSingleton("Z"::equals, EndOfRecord.END_OF_RECORD)
					)
			).space(1).value(2, GENUS, ControlledValueParser.optional(ValueParser.GENUS))
					.value(6, PERCENT_GENUS, ValueParser.PERCENTAGE)
					.value(3, SPECIES_1, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_1, ValueParser.PERCENTAGE)
					.value(3, SPECIES_2, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_2, ValueParser.PERCENTAGE)
					.value(3, SPECIES_3, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_3, ValueParser.PERCENTAGE)
					.value(3, SPECIES_4, ControlledValueParser.optional(ValueParser.SPECIES))
					.value(5, PERCENT_SPECIES_4, ValueParser.PERCENTAGE);

			var is = fileResolver.resolve(fileName);

			var delegateStream = new AbstractStreamingParser<ValueOrMarker<Optional<FipSpecies>, EndOfRecord>>(
					is, lineParser, control
			) {

				@SuppressWarnings("unchecked")
				@Override
				protected ValueOrMarker<Optional<FipSpecies>, EndOfRecord> convert(Map<String, Object> entry)
						throws ResourceParseException {
					var polygonId = (String) entry.get(POLYGON_IDENTIFIER);
					var layer = (ValueOrMarker<Optional<Layer>, EndOfRecord>) entry.get(LAYER);
					String genus;
					if (layer.isValue()) {
						genus = ((Optional<String>) entry.get(GENUS)).orElseThrow(
								() -> new ResourceParseValidException(
										"Genus identifier can not be empty except in end of record entries"
								)
						);
					} else {
						genus = null;
					}
					var percentGenus = (Float) entry.get(PERCENT_GENUS);
					var species1 = (Optional<String>) entry.get(SPECIES_1);
					var percentSpecies1 = (Float) entry.get(PERCENT_SPECIES_1);
					var species2 = (Optional<String>) entry.get(SPECIES_2);
					var percentSpecies2 = (Float) entry.get(PERCENT_SPECIES_2);
					var species3 = (Optional<String>) entry.get(SPECIES_3);
					var percentSpecies3 = (Float) entry.get(PERCENT_SPECIES_3);
					var species4 = (Optional<String>) entry.get(SPECIES_4);
					var percentSpecies4 = (Float) entry.get(PERCENT_SPECIES_4);

					var builder = new ValueOrMarker.Builder<Optional<FipSpecies>, EndOfRecord>();
					var result = layer.handle(l -> {
						return builder.value(l.map(layerType -> {
							Map<String, Float> speciesPercent = new LinkedHashMap<>();
							species1.ifPresent((sp) -> speciesPercent.put(sp, percentSpecies1));
							species2.ifPresent((sp) -> speciesPercent.put(sp, percentSpecies2));
							species3.ifPresent((sp) -> speciesPercent.put(sp, percentSpecies3));
							species4.ifPresent((sp) -> speciesPercent.put(sp, percentSpecies4));
							var species = new FipSpecies(polygonId, layerType, genus, percentGenus, speciesPercent);
							return species;
						}));
					}, m -> {
						return builder.marker(m);
					});
					return result;
				}

			};

			return new GroupingStreamingParser<Collection<FipSpecies>, ValueOrMarker<Optional<FipSpecies>, EndOfRecord>>(
					delegateStream
			) {

				@Override
				protected boolean skip(ValueOrMarker<Optional<FipSpecies>, EndOfRecord> nextChild) {
					return nextChild.getValue().map(Optional::isEmpty).orElse(false);
				}

				@Override
				protected boolean stop(ValueOrMarker<Optional<FipSpecies>, EndOfRecord> nextChild) {
					return nextChild.isMarker();
				}

				@Override
				protected Collection<FipSpecies>
						convert(List<ValueOrMarker<Optional<FipSpecies>, EndOfRecord>> children) {
					return children.stream().map(ValueOrMarker::getValue).map(Optional::get) // Should never be empty as
																								// we've filtered out
																								// markers
							.flatMap(Optional::stream) // Skip if empty (and unknown layer type)
							.toList();
				}

			};
		};
	}
}
