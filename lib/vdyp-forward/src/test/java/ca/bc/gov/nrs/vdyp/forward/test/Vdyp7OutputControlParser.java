package ca.bc.gov.nrs.vdyp.forward.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.forward.parsers.ForwardControlVariableParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonDescriptionParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypUtilizationParser;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BreakageEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.VolumeEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.control.BaseControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.control.ControlMapValueReplacer;
import ca.bc.gov.nrs.vdyp.io.parse.control.ResourceControlMapModifier;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParser;

/**
 * Parser for VDYP control files
 *
 * @author Michael Junkin, Vivid Solutions
 */
public class Vdyp7OutputControlParser extends BaseControlParser {

	private static final Logger logger = LoggerFactory.getLogger(Vdyp7OutputControlParser.class);

	public Vdyp7OutputControlParser() {
		initialize();
	}

	@Override
	protected VdypApplicationIdentifier getProgramId() {
		return VdypApplicationIdentifier.VDYP_FORWARD;
	}

	@Override
	protected List<ControlMapValueReplacer<Object, String>> inputFileParsers() {
		return new ArrayList<>(vdypForwardInputParsers.values());
	}

	@Override
	protected List<ControlKey> outputFileParsers() {
		return List.of();
	}

	@Override
	protected ValueParser<Map<String, Float>> minimaParser() {
		return ValueParser.toMap(
				ValueParser.list(ValueParser.FLOAT), Collections.singletonMap(MINIMUM_VETERAN_HEIGHT, 10.0f),
				MINIMUM_HEIGHT, MINIMUM_BASE_AREA, MINIMUM_PREDICTED_BASE_AREA, MINIMUM_VETERAN_HEIGHT
		);
	}

	private final List<ControlKey> orderedControlKeys = new ArrayList<>();

	private final Map<ControlKey, ControlMapValueReplacer<Object, String>> vdypForwardInputParsers = new EnumMap<>(
			ControlKey.class
	);

	private void addInputParser(ControlMapValueReplacer<Object, String> parser) {
		vdypForwardInputParsers.put(parser.getControlKey(), parser);
		orderedControlKeys.add(parser.getControlKey());
	}

	private final Map<ControlKey, ResourceControlMapModifier> vdypForwardConfigurationParsers = new EnumMap<>(
			ControlKey.class
	);

	private void addConfigurationParser(ResourceControlMapModifier parser) {
		vdypForwardConfigurationParsers.put(parser.getControlKey(), parser);
		orderedControlKeys.add(parser.getControlKey());
	}

	@Override
	protected void initialize() {

		// RD_BECD - 09
		addConfigurationParser(new BecDefinitionParser());

		// DEF_BEC - superceded by BecLookup.getGrowthBecs

		// RD_SP0 - 10
		addConfigurationParser(new GenusDefinitionParser());

		// RD_VGRP - 20
		addConfigurationParser(new VolumeEquationGroupParser());
		// RD_DGRP - 21
		addConfigurationParser(new DecayEquationGroupParser());
		// RD_BGRP - 22
		addConfigurationParser(new BreakageEquationGroupParser());

		// V7O_VIP - 11
		addInputParser(new VdypPolygonParser());
		// V7O_VIS - 12
		addInputParser(new VdypSpeciesParser());
		// V7O_VIU - 13
		addInputParser(new VdypUtilizationParser());
		// V7O_VI7 - 14
		addInputParser(new VdypPolygonDescriptionParser());

		// 101 - a literal value of type VdypGrowthDetails

		controlParser.record(ControlKey.VTROL, new ForwardControlVariableParser());
		orderedControlKeys.add(ControlKey.VTROL);

		// 199 - debug switches
		orderedControlKeys.add(ControlKey.DEBUG_SWITCHES);

		// 1 - MAX_NUM_POLY
		orderedControlKeys.add(ControlKey.MAX_NUM_POLY);

		super.initialize();
	}

	@Override
	protected List<ResourceControlMapModifier> configurationFileParsers() {
		return new ArrayList<>(vdypForwardConfigurationParsers.values());
	}

	@Override
	protected void applyAllModifiers(Map<String, Object> map, FileResolver fileResolver)
			throws ResourceParseException, IOException {

		// FORWARD_INPUT_GROWTO is optional; if missing, the polygon list is read from the
		// polygon file itself.
		Optional<String> source = Utils.optSafe(map.get(ControlKey.FORWARD_INPUT_GROWTO.name()));
		if (source.isEmpty()) {
			String polyFileName = (String) map.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());
			map.put(ControlKey.FORWARD_INPUT_GROWTO.name(), polyFileName);
		}

		for (ControlKey key : orderedControlKeys) {

			ResourceControlMapModifier m = vdypForwardConfigurationParsers.get(key);
			if (m != null) {
				// m is a configuration file parser.
				logger.debug(
						"Parsing configuration file {}[{}] using {}", m.getControlKeyName(), key.sequence.get(),
						m.getClass().getName()
				);
				m.modify(map, fileResolver);
			}

			ControlMapValueReplacer<?, ?> r = vdypForwardInputParsers.get(key);
			if (r != null) {
				// r is an input file parser.
				logger.debug(
						"Parsing input file {}[{}] using {}", r.getControlKeyName(), key.sequence.get(),
						r.getClass().getName()
				);
				r.modify(map, fileResolver);
			}
		}
	}
}
