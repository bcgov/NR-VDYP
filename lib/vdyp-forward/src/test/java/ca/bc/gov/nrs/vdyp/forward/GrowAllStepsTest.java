package ca.bc.gov.nrs.vdyp.forward;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.forward.test.Vdyp7OutputControlParser;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.test.VdypMatchers;

class GrowAllStepsTest {

	protected static final Logger logger = LoggerFactory.getLogger(GrowAllStepsTest.class);

	protected static ForwardControlParser parser;
	protected static Map<String, Object> controlMap;

	protected static StreamingParserFactory<PolygonIdentifier> polygonDescriptionStreamFactory;
	protected static StreamingParser<PolygonIdentifier> polygonDescriptionStream;

	protected static ForwardDataStreamReader forwardDataStreamReader;
	protected static ForwardDataStreamReader comparisonDataStreamReader;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void beforeTest() throws IOException, ResourceParseException, ProcessingException {

		parser = new ForwardControlParser();
		controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");

		polygonDescriptionStreamFactory = (StreamingParserFactory<PolygonIdentifier>) controlMap
				.get(ControlKey.FORWARD_INPUT_GROWTO.name());
		polygonDescriptionStream = polygonDescriptionStreamFactory.get();

		forwardDataStreamReader = new ForwardDataStreamReader(controlMap);

		var comparisonDataParser = new Vdyp7OutputControlParser();
		var vdyp7OutputControlMap = ForwardTestUtils.parse(comparisonDataParser, "vdyp7/vdyp7_output_data.CTR");

		ResolvedControlMap resolvedMap = new ResolvedControlMapImpl(vdyp7OutputControlMap);
		comparisonDataStreamReader = new ForwardDataStreamReader(resolvedMap);
	}

	private class WorkCompletedException extends Exception {
		private static final long serialVersionUID = 1232212709891370832L;
	};

	@Test
	void testStandardPath() throws ProcessingException, ValueParseException {

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);

		try {
			var nextComparisonPolygonRef = comparisonDataStreamReader.readNextPolygon();

			while (nextComparisonPolygonRef.isPresent()) {

				var polygon = forwardDataStreamReader.readNextPolygon().orElseThrow(() -> new WorkCompletedException());

				var comparisonPolygon = nextComparisonPolygonRef.get();
				nextComparisonPolygonRef = comparisonDataStreamReader.readNextPolygon();

				while (nextComparisonPolygonRef.isPresent() && nextComparisonPolygonRef.get().getPolygonIdentifier()
						.getName().equals(comparisonPolygon.getPolygonIdentifier().getName())) {
					comparisonPolygon = nextComparisonPolygonRef.get();
					nextComparisonPolygonRef = comparisonDataStreamReader.readNextPolygon();
				}

				assertThat(
						polygon.getPolygonIdentifier().forYear(polygon.getTargetYear().get()),
						is(comparisonPolygon.getPolygonIdentifier())
				);

				fpe.processPolygon(polygon);

// 				comparePolygons(polygon, comparisonPolygon, 0.02f);
			}
		} catch (WorkCompletedException e) {
			assertThat(comparisonDataStreamReader.readNextPolygon().isEmpty(), is(true));
		}

		assertThat(forwardDataStreamReader.readNextPolygon().isEmpty(), is(true));
	}

	private static void comparePolygons(VdypPolygon a, VdypPolygon b, float tolerance) {

		VdypMatchers.setEpsilon(tolerance);

		assertThat(a.getPolygonIdentifier().forYear(a.getTargetYear().get()), is(b.getPolygonIdentifier()));
		assertThat(a.getBiogeoclimaticZone(), is(b.getBiogeoclimaticZone()));
		assertThat(a.getForestInventoryZone(), is(b.getForestInventoryZone()));
		assertThat(a.getInventoryTypeGroup(), is(b.getInventoryTypeGroup()));
		// assertThat(a.getMode(), is(b.getMode())); -- comparison polys don't have modes
		assertThat(a.getPercentAvailable(), is(b.getPercentAvailable()));
		assertThat(a.getTargetYear().get(), is(b.getPolygonIdentifier().getYear()));

		assertThat(a.getLayers().size(), is(b.getLayers().size()));

		compareLayers(a.getLayers().get(LayerType.PRIMARY), b.getLayers().get(LayerType.PRIMARY));
		// compareLayers(a.getLayers().get(LayerType.VETERAN), b.getLayers().get(LayerType.VETERAN));
	}

	private static void compareLayers(VdypLayer aLayer, VdypLayer bLayer) {
		if (aLayer != null && bLayer != null) {

			assertThat(aLayer.getLayerType(), is(bLayer.getLayerType()));
			assertThat(aLayer.getPrimaryGenus(), is(bLayer.getPrimaryGenus()));
			assertThat(aLayer.getInventoryTypeGroup(), is(bLayer.getInventoryTypeGroup()));
			assertThat(aLayer.getPolygonIdentifier().getName(), is(bLayer.getPolygonIdentifier().getName()));
			assertThat(aLayer.getAgeTotal(), is(bLayer.getAgeTotal()));
			assertThat(
					aLayer.getBaseAreaByUtilization(),
					VdypMatchers.coe(-1, VdypMatchers::closeTo, bLayer.getBaseAreaByUtilization().toArray(Float[]::new))
			);

		} else if (aLayer != bLayer) {
			fail();
		}
	}
}
