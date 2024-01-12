package ca.bc.gov.nrs.vdyp.fip;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.fip.model.FipLayer;
import ca.bc.gov.nrs.vdyp.fip.model.FipLayerPrimary;
import ca.bc.gov.nrs.vdyp.fip.model.FipMode;
import ca.bc.gov.nrs.vdyp.fip.model.FipPolygon;
import ca.bc.gov.nrs.vdyp.fip.model.FipSpecies;
import ca.bc.gov.nrs.vdyp.io.FileResolver;
import ca.bc.gov.nrs.vdyp.io.FileSystemFileResolver;
import ca.bc.gov.nrs.vdyp.io.parse.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.LayerType;

public class FipStart {

	static private final Logger log = LoggerFactory.getLogger(FipStart.class);

	public static final int CONFIG_LOAD_ERROR = 1; // TODO check what Fortran FIPStart would exit with.
	public static final int PROCESSING_ERROR = 2; // TODO check what Fortran FIPStart would exit with.

	private Map<String, Object> controlMap = Collections.emptyMap();

	/**
	 * Initialize FipStart
	 *
	 * @param resolver
	 * @param controlFilePath
	 * @throws IOException
	 * @throws ResourceParseException
	 */
	void init(FileResolver resolver, String controlFilePath) throws IOException, ResourceParseException {

		// Load the control map

		var parser = new FipControlParser();
		try (var is = resolver.resolve(controlFilePath)) {
			setControlMap(parser.parse(is, resolver));
		}

	}

	void setControlMap(Map<String, Object> controlMap) {
		this.controlMap = controlMap;
	}

	public static void main(final String... args) {

		var app = new FipStart();

		var resolver = new FileSystemFileResolver();

		var controlFileName = args[0];

		try {
			app.init(resolver, controlFileName);
		} catch (Exception ex) {
			log.error("Error during initialization", ex);
			System.exit(CONFIG_LOAD_ERROR);
		}

		try {
			app.process();
		} catch (Exception ex) {
			log.error("Error during processing", ex);
			System.exit(PROCESSING_ERROR);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> StreamingParser<T> getStreamingParser(String key) throws ProcessingException {
		try {
			var factory = (StreamingParserFactory<T>) controlMap.get(key);
			if (factory == null) {
				throw new ProcessingException(String.format("Data file %s not specified in control map.", key));
			}
			return factory.get();
		} catch (IOException ex) {
			throw new ProcessingException("Error while opening data file.", ex);
		}
	}

	void process() throws ProcessingException {
		try (
				var polyStream = this.<FipPolygon>getStreamingParser(FipPolygonParser.CONTROL_KEY);
				var layerStream = this.<Map<LayerType, FipLayer>>getStreamingParser(FipLayerParser.CONTROL_KEY);
				var speciesStream = this.<Collection<FipSpecies>>getStreamingParser(FipSpeciesParser.CONTROL_KEY);
		) {

			int polygonsRead = 0;

			while (polyStream.hasNext()) {

				// FIP_GET

				log.info("Getting polygon {}", polygonsRead + 1);
				var polygon = getPolygon(polyStream, layerStream, speciesStream);

				log.info("Read polygon {}, preparing to process", polygon.getPolygonIdentifier());

				// if (MODE .eq. -1) go to 100

				final var mode = polygon.getModeFip().orElse(FipMode.DONT_PROCESS);

				if (mode == FipMode.DONT_PROCESS) {
					log.info("Skipping polygon with mode {}", mode);
					continue;
				}

				// IP_IN = IP_IN+1
				// if (IP_IN .gt. MAXPOLY) go to 200

				polygonsRead++; // Don't count polygons we aren't processing due to mode. This was the behavior
				// in VDYP7

				// IPASS = 1
				// CALL FIP_CHK( IPASS, IER)
				// if (ier .gt. 0) go to 1000
				//
				// if (IPASS .le. 0) GO TO 120

				log.info("Checking validity of polygon {}:{}", polygonsRead, polygon.getPolygonIdentifier());
				checkPolygon(polygon);

				// CALL FIPCALCV( BAV, IER)
				// CALL FIPCALC1( BAV, BA_TOTL1, IER)

			}
		} catch (IOException | ResourceParseException ex) {
			throw new ProcessingException("Error while reading or writing data.", ex);
		}
	}

	private FipPolygon getPolygon(
			StreamingParser<FipPolygon> polyStream, StreamingParser<Map<LayerType, FipLayer>> layerStream,
			StreamingParser<Collection<FipSpecies>> speciesStream
	) throws ProcessingException, IOException, ResourceParseException {

		log.trace("Getting polygon");
		var polygon = polyStream.next();

		log.trace("Getting layers for polygon {}", polygon.getPolygonIdentifier());
		Map<LayerType, FipLayer> layers;
		try {
			layers = layerStream.next();
		} catch (NoSuchElementException ex) {
			throw validationError("Layers file has fewer records than polygon file.", ex);
		}

		log.trace("Getting species for polygon {}", polygon.getPolygonIdentifier());
		Collection<FipSpecies> species;
		try {
			species = speciesStream.next();
		} catch (NoSuchElementException ex) {
			throw validationError("Species file has fewer records than polygon file.", ex);
		}

		// Validate that layers belong to the correct polygon
		for (var layer : layers.values()) {
			if (!layer.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in layer file contains layer for polygon %s when expecting one for %s.",
						layer.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			layer.setSpecies(new HashMap<>());
		}

		for (var spec : species) {
			var layer = layers.get(spec.getLayer());
			// Validate that species belong to the correct polygon
			if (!spec.getPolygonIdentifier().equals(polygon.getPolygonIdentifier())) {
				throw validationError(
						"Record in species file contains species for polygon %s when expecting one for %s.",
						layer.getPolygonIdentifier(), polygon.getPolygonIdentifier()
				);
			}
			if (Objects.isNull(layer)) {
				throw validationError(
						"Species entry references layer %s of polygon %s but it is not present.", layer,
						polygon.getPolygonIdentifier()
				);
			}
			layer.getSpecies().put(spec.getGenus(), spec);
		}

		polygon.setLayers(layers);

		return polygon;
	}

	private Optional<Float> heightMinimum(LayerType layer, Map<String, Object> controlMap) {
		@SuppressWarnings("unchecked")
		var minima = (Map<String, Float>) controlMap.get(FipControlParser.MINIMA);
		switch (layer) {
		case PRIMARY:
			return Optional.of(minima.get(FipControlParser.MINIMUM_HEIGHT));
		case VETERAN:
			return Optional.of(minima.get(FipControlParser.MINIMUM_VETERAN_HEIGHT));
		default:
			return Optional.empty();
		}
	}

	private void checkPolygon(FipPolygon polygon) throws ProcessingException {

		if (!polygon.getLayers().containsKey(LayerType.PRIMARY)) {
			throw validationError(
					"Polygon %s has no %s layer, or that layer has non-positive height or crown closure.",
					polygon.getPolygonIdentifier(), LayerType.PRIMARY
			);
		}

		var primaryLayer = (FipLayerPrimary) polygon.getLayers().get(LayerType.PRIMARY);

		// FIXME VDYP7 actually tests if total age - YTBH is less than 0.5 but gives an
		// error that total age is "less than" YTBH. Replicating that for now but
		// consider changing it.

		if (primaryLayer.getAgeTotal() - primaryLayer.getYearsToBreastHeight() < 0.5f) {
			throw validationError(
					"Polygon %s has %s layer where total age is less than YTBH.", polygon.getPolygonIdentifier(),
					LayerType.PRIMARY
			);
		}

		// TODO This is the only validation step done to non-primary layers, VDYP7 had a
		// less well defined idea of a layer being present or not and so it may have
		// skipped validating other layers rather than validating them conditionally on
		// being present. Consider extending validation of other properties to other
		// layers.

		for (var layer : polygon.getLayers().values()) {
			var height = layer.getHeight();

			throwIfPresent(
					heightMinimum(layer.getLayer(), controlMap).filter(minimum -> height < minimum).map(
							minimum -> validationError(
									"Polygon %s has %s layer where height %.1f is less than minimum %.1f.",
									polygon.getPolygonIdentifier(), layer.getLayer(), layer.getHeight(), minimum
							)
					)
			);
		}

		if (polygon.getModeFip().map(x -> x == FipMode.FIPYOUNG).orElse(false)) {
			throw validationError(
					"Polygon %s is using unsupported mode %s.", polygon.getPolygonIdentifier(), FipMode.FIPYOUNG
			);
		}

		if (primaryLayer.getYearsToBreastHeight() < 0.5) {
			throw validationError(
					"Polygon %s has %s layer where years to breast height %.1f is less than minimum %.1f years.",
					polygon.getPolygonIdentifier(), LayerType.PRIMARY, primaryLayer.getYearsToBreastHeight(), 0.5f
			);
		}

		if (primaryLayer.getSiteIndex() < 0.5) {
			throw validationError(
					"Polygon %s has %s layer where site index %.1f is less than minimum %.1f years.",
					polygon.getPolygonIdentifier(), LayerType.PRIMARY, primaryLayer.getSiteIndex(), 0.5f
			);
		}

		for (var layer : polygon.getLayers().values()) {
			var percentTotal = layer.getSpecies().values().stream()//
					.map(FipSpecies::getPercentGenus)//
					.reduce(0.0f, (x, y) -> x + y);
			if (Math.abs(percentTotal - 100f) > 0.01f) {
				throw validationError(
						"Polygon %s has %s layer where species entries have a percentage total that does not sum to 100%%.",
						polygon.getPolygonIdentifier(), LayerType.PRIMARY
				);
			}
			// VDYP7 performs this step which should be negligible but might have a small
			// impact due to the 0.01 percent variation and floating point errors.
			if (layer.getLayer() == LayerType.PRIMARY) {
				layer.getSpecies().values().forEach(species -> {
					species.setFractionGenus(species.getPercentGenus() / percentTotal);
				});
			}
		}

	}

	private static <E extends Throwable> void throwIfPresent(Optional<E> opt) throws E {
		if (opt.isPresent()) {
			throw opt.get();
		}
	}

	private static ProcessingException validationError(String template, Object... values) {
		return new ProcessingException(String.format(template, values));
	};
}
