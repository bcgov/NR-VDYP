package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_1;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_2;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_3;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_4;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_5;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypPolygonParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypSpeciesParser;
import ca.bc.gov.nrs.vdyp.forward.parsers.VdypUtilizationParser;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BecDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.BreakageEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.DecayEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.coe.VolumeEquationGroupParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardProcessorEndToEndTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessorEndToEndTest.class);

	private static Set<ForwardPass> vdypPassSet = new HashSet<>(Arrays.asList(PASS_1, PASS_2, PASS_3, PASS_4, PASS_5));

	private int nEquals = 0;
	private int nWithin1Percent = 0;
	private int nWithin2Percent = 0;
	private int nWithin5Percent = 0;
	private int nWithin10Percent = 0;
	private int nAbove10Percent = 0;

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessor fp = new ForwardProcessor();

		FileResolver inputFileResolver = TestUtils.fileResolver(TestUtils.class);

		Path vdyp8OutputPath = Path.of(System.getenv().get("HOME"), "tmp", "vdyp-deltas", "vdyp8");
		Files.createDirectories(vdyp8OutputPath);

		var vdyp8OutputResolver = new FileSystemFileResolver(vdyp8OutputPath);

		fp.run(inputFileResolver, vdyp8OutputResolver, List.of("VDYP.CTR"), vdypPassSet);

		var vdyp8InputResolver = new FileSystemFileResolver(vdyp8OutputPath);
		var polygonParser = new VdypPolygonParser();
		var speciesParser = new VdypSpeciesParser();
		var utilizationParser = new VdypUtilizationParser();
		var becDefinitionParser = new BecDefinitionParser();
		var genusDefinitionParser = new GenusDefinitionParser();
		var breakageGroupsParser = new BreakageEquationGroupParser();
		var decayGroupsParser = new DecayEquationGroupParser();
		var volumeGroupsParser = new VolumeEquationGroupParser();

		Map<String, Object> vdyp8ControlMap = new HashMap<>();
		vdyp8ControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
				polygonParser.map("vp_grow2.dat", vdyp8InputResolver, vdyp8ControlMap)
		);
		vdyp8ControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
				speciesParser.map("vs_grow2.dat", vdyp8InputResolver, vdyp8ControlMap)
		);
		vdyp8ControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				utilizationParser.map("vu_grow2.dat", vdyp8InputResolver, vdyp8ControlMap)
		);
		vdyp8ControlMap.put(
				ControlKey.BEC_DEF.name(),
				becDefinitionParser.parse(TestUtils.class, "coe/Becdef.dat", Collections.emptyMap())
		);
		vdyp8ControlMap.put(
				ControlKey.SP0_DEF.name(),
				genusDefinitionParser.parse(TestUtils.class, "coe/SP0DEF_v0.dat", Collections.emptyMap())
		);
		vdyp8ControlMap.put(
				ControlKey.BREAKAGE_GROUPS.name(),
				breakageGroupsParser.parse(TestUtils.class, "coe/BGRP.DAT", vdyp8ControlMap)
		);
		vdyp8ControlMap.put(
				ControlKey.DECAY_GROUPS.name(),
				decayGroupsParser.parse(TestUtils.class, "coe/DGRP.DAT", vdyp8ControlMap)
		);
		vdyp8ControlMap.put(
				ControlKey.VOLUME_EQN_GROUPS.name(),
				volumeGroupsParser.parse(TestUtils.class, "coe/VGRPDEF1.DAT", vdyp8ControlMap)
		);

		FileResolver vdyp7InputResolver = TestUtils.fileResolver(TestUtils.class);

		Map<String, Object> vdyp7ControlMap = new HashMap<>();
		vdyp7ControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
				polygonParser.map("../../vdyp7/vp_grow2.dat", vdyp7InputResolver, vdyp7ControlMap)
		);
		vdyp7ControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
				speciesParser.map("../../vdyp7/vs_grow2.dat", vdyp7InputResolver, vdyp7ControlMap)
		);
		vdyp7ControlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				utilizationParser.map("../../vdyp7/vu_grow2.dat", vdyp7InputResolver, vdyp7ControlMap)
		);
		vdyp7ControlMap.put(
				ControlKey.BEC_DEF.name(),
				becDefinitionParser.parse(TestUtils.class, "coe/Becdef.dat", Collections.emptyMap())
		);
		vdyp7ControlMap.put(
				ControlKey.SP0_DEF.name(),
				genusDefinitionParser.parse(TestUtils.class, "coe/SP0DEF_v0.dat", Collections.emptyMap())
		);
		vdyp7ControlMap.put(
				ControlKey.BREAKAGE_GROUPS.name(),
				breakageGroupsParser.parse(TestUtils.class, "coe/BGRP.DAT", vdyp7ControlMap)
		);
		vdyp7ControlMap.put(
				ControlKey.DECAY_GROUPS.name(),
				decayGroupsParser.parse(TestUtils.class, "coe/DGRP.DAT", vdyp7ControlMap)
		);
		vdyp7ControlMap.put(
				ControlKey.VOLUME_EQN_GROUPS.name(),
				volumeGroupsParser.parse(TestUtils.class, "coe/VGRPDEF1.DAT", vdyp7ControlMap)
		);

		var vdyp8Reader = new ForwardDataStreamReader(vdyp8ControlMap);
		var vdyp7Reader = new ForwardDataStreamReader(vdyp7ControlMap);

		Optional<VdypPolygon> optPolygon8 = vdyp8Reader.readNextPolygon();
		Optional<VdypPolygon> optPolygon7 = vdyp7Reader.readNextPolygon();

		while (optPolygon8.isPresent()) {
			var polygon8 = optPolygon8.orElseThrow();
			var polygon7 = optPolygon7.orElseThrow();
			assertEquals(polygon7.getPolygonIdentifier(), polygon8.getPolygonIdentifier());

			compare(polygon7, polygon8);

			optPolygon8 = vdyp8Reader.readNextPolygon();
			optPolygon7 = vdyp7Reader.readNextPolygon();
		}

		assertTrue(optPolygon7.isEmpty());

		assertTrue(nEquals >= 30012);
		assertTrue(nWithin1Percent >= 58695);
		assertTrue(nWithin2Percent >= 58720);
		assertTrue(nWithin5Percent >= 58753);
		assertTrue(nWithin10Percent >= 58755);
		assertTrue(nAbove10Percent <= 1032);
	}

	private void compare(VdypPolygon polygon7, VdypPolygon polygon8) {

		assertEquals(polygon7.getBiogeoclimaticZone(), polygon8.getBiogeoclimaticZone());
		assertEquals(polygon7.getForestInventoryZone(), polygon8.getForestInventoryZone());
		assertEquals(polygon7.getInventoryTypeGroup(), polygon8.getInventoryTypeGroup());
		assertEquals(polygon7.getPercentAvailable(), polygon8.getPercentAvailable());
		assertEquals(polygon7.getTargetYear(), polygon7.getTargetYear());

		compareLayers(polygon7.getLayers(), polygon8.getLayers());
	}

	private void compareLayers(Map<LayerType, VdypLayer> layerList7, Map<LayerType, VdypLayer> layerList8) {
		for (LayerType t : layerList7.keySet()) {
			var l7 = layerList7.get(t);
			var l8 = layerList8.get(t);

			assertNotNull(l8);

			assertEquals(l7.getEmpiricalRelationshipParameterIndex(), l8.getEmpiricalRelationshipParameterIndex());
			assertEquals(l7.getInventoryTypeGroup(), l8.getInventoryTypeGroup());
			assertEquals(l7.getPolygonIdentifier(), l8.getPolygonIdentifier());
			assertEquals(l7.getPrimaryGenus(), l8.getPrimaryGenus());
			assertEquals(l7.getPrimarySpeciesRecord(), l8.getPrimarySpeciesRecord());

			compareUtilizations(l7, l8);

			compareSites(l7.getSites(), l8.getSites());

			compareSpecies(l7.getSpecies(), l8.getSpecies());
		}
	}

	private void compareSpecies(HashMap<String, VdypSpecies> species7, HashMap<String, VdypSpecies> species8) {
		for (var k : species7.keySet()) {
			assertTrue(species8.containsKey(k));
			var s7 = species7.get(k);
			var s8 = species8.get(k);

			assertEquals(s7.getBreakageGroup(), s8.getBreakageGroup());
			assertEquals(s7.getDecayGroup(), s8.getDecayGroup());
			compareFloats(s7.getFractionGenus(), s8.getFractionGenus());
			compareFloats(s7.getPercentGenus(), s8.getPercentGenus());
			assertEquals(s7.getGenus(), s8.getGenus());
			assertEquals(s7.getGenusIndex(), s8.getGenusIndex());
			assertEquals(s7.getBreakageGroup(), s8.getBreakageGroup());
			assertEquals(s7.getDecayGroup(), s8.getDecayGroup());
			assertEquals(s7.getLayerType(), s8.getLayerType());
			assertEquals(s7.getPolygonIdentifier(), s8.getPolygonIdentifier());
			assertEquals(s7.getSp64DistributionSet(), s8.getSp64DistributionSet());
			assertEquals(s7.getVolumeGroup(), s8.getVolumeGroup());

			compareUtilizations(s7, s8);
		}
	}

	private void compareSites(HashMap<String, VdypSite> sites7, HashMap<String, VdypSite> sites8) {

		for (var k : sites7.keySet()) {
			assertTrue(sites8.containsKey(k));
			var s7 = sites7.get(k);
			var s8 = sites8.get(k);

			assertEquals(s7.getAgeTotal(), s8.getAgeTotal());
			assertEquals(s7.getYearsAtBreastHeight(), s8.getYearsAtBreastHeight());
			assertEquals(s7.getHeight(), s8.getHeight());
			assertEquals(s7.getYearsToBreastHeight(), s8.getYearsToBreastHeight());
			assertEquals(s7.getLayerType(), s8.getLayerType());
			assertEquals(s7.getSiteCurveNumber(), s8.getSiteCurveNumber());
			assertEquals(s7.getSiteGenus(), s8.getSiteGenus());
			compareFloats(s7.getSiteIndex(), s8.getSiteIndex());
		}
	}

	private void compareFloats(Optional<Float> of7, Optional<Float> of8) {
		assertEquals(of7.isPresent(), of8.isPresent());
		if (of7.isPresent()) {
			compareFloats(of7.get(), of8.get());
		}
	}

	private void compareFloats(float f7, float f8) {
		float ratio;
		if (f7 == f8) {
			nEquals += 1;
			ratio = 0.0f;
		} else {
			ratio = Math.abs(f7 / f8) - 1.0f;
		}
		if (ratio <= 0.01f) {
			nWithin1Percent += 1;
		}
		if (ratio <= 0.02f) {
			nWithin2Percent += 1;
		}
		if (ratio <= 0.05f) {
			nWithin5Percent += 1;
		}
		if (ratio <= 0.10f) {
			nWithin10Percent += 1;
		} else {
			nAbove10Percent += 1;
		}
	}

	private void compareUtilizations(VdypUtilizationHolder u7, VdypUtilizationHolder u8) {

		compareUtilizationVector(u7.getBaseAreaByUtilization(), u8.getBaseAreaByUtilization());
		compareUtilizationVector(
				u7.getCloseUtilizationVolumeByUtilization(), u8.getCloseUtilizationVolumeByUtilization()
		);
		compareUtilizationVector(
				u7.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(),
				u8.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
		);
		compareUtilizationVector(
				u7.getCloseUtilizationVolumeNetOfDecayByUtilization(),
				u8.getCloseUtilizationVolumeNetOfDecayByUtilization()
		);
		compareUtilizationVector(
				u7.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(),
				u8.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization()
		);
		compareUtilizationVector(u7.getLoreyHeightByUtilization(), u8.getLoreyHeightByUtilization());
		compareUtilizationVector(
				u7.getQuadraticMeanDiameterByUtilization(), u8.getQuadraticMeanDiameterByUtilization()
		);
		compareUtilizationVector(u7.getTreesPerHectareByUtilization(), u8.getTreesPerHectareByUtilization());
		compareUtilizationVector(u7.getWholeStemVolumeByUtilization(), u8.getWholeStemVolumeByUtilization());
	}

	private void compareUtilizationVector(UtilizationVector uv7, UtilizationVector uv8) {
		assertEquals(uv7.size(), uv8.size());
		var i8 = uv8.iterator();
		uv7.forEach(uc7 -> compareFloats(uc7, i8.next()));
	}
}
