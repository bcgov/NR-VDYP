package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_1;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_2;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_3;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_4;
import static ca.bc.gov.nrs.vdyp.forward.ForwardPass.PASS_5;

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
import ca.bc.gov.nrs.vdyp.io.parse.coe.GenusDefinitionParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.test.TestUtils;

class ForwardProcessorCheckpointGenerationTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessorCheckpointGenerationTest.class);

	private static Set<ForwardPass> vdypPassSet = new HashSet<>(Arrays.asList(PASS_1, PASS_2, PASS_3, PASS_4, PASS_5));

	@Test
	void test() throws IOException, ResourceParseException, ProcessingException {

		ForwardProcessor fp = new ForwardProcessor();

		FileResolver inputFileResolver = TestUtils.fileResolver(TestUtils.class);

		Path vdyp8OutputPath = Path.of(System.getenv().get("HOME"), "tmp", "vdyp-deltas", "vdyp8");
		Files.createDirectories(vdyp8OutputPath);

		var vdyp8OutputResolver = new FileSystemFileResolver(vdyp8OutputPath);

		fp.run(inputFileResolver, vdyp8OutputResolver, List.of("VDYP-Checkpoint.CTR"), vdypPassSet);

		var vdyp8InputResolver = new FileSystemFileResolver(vdyp8OutputPath);

		// Verify that polygons are output 14 times for each year of growth.

		Map<String, Object> controlMap = new HashMap<>();
		var polygonParser = new VdypPolygonParser();
		controlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_POLY.name(),
				polygonParser.map("vp_grow2.dat", vdyp8InputResolver, controlMap)
		);
		var speciesParser = new VdypSpeciesParser();
		controlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name(),
				speciesParser.map("vs_grow2.dat", vdyp8InputResolver, controlMap)
		);
		var utilizationParser = new VdypUtilizationParser();
		controlMap.put(
				ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name(),
				utilizationParser.map("vu_grow2.dat", vdyp8InputResolver, controlMap)
		);
		var becDefinitionParser = new BecDefinitionParser();
		controlMap.put(
				ControlKey.BEC_DEF.name(),
				becDefinitionParser.parse(TestUtils.class, "coe/Becdef.dat", Collections.emptyMap())
		);
		var genusDefinitionParser = new GenusDefinitionParser();
		controlMap.put(
				ControlKey.SP0_DEF.name(),
				genusDefinitionParser.parse(TestUtils.class, "coe/SP0DEF_v0.dat", Collections.emptyMap())
		);

		var reader = new ForwardDataStreamReader(controlMap);
		Optional<VdypPolygon> polygon = reader.readNextPolygon();
		var polygonName = polygon.orElseThrow().getPolygonIdentifier().getBase();

		int count = 0;
		var nextPolygonIdentifier = reader.readNextPolygon().get().getPolygonIdentifier();
		var year = nextPolygonIdentifier.getYear();
		while (nextPolygonIdentifier.getYear() == year && nextPolygonIdentifier.getBase().equals(polygonName)) {
			count += 1;
			nextPolygonIdentifier = reader.readNextPolygon().get().getPolygonIdentifier();
		}

		assert (count == 14);
	}
}
