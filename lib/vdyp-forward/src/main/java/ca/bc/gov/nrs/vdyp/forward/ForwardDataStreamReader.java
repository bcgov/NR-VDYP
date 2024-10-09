package ca.bc.gov.nrs.vdyp.forward;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.controlmap.ResolvedControlMap;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParser;
import ca.bc.gov.nrs.vdyp.io.parse.streaming.StreamingParserFactory;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilization;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

public class ForwardDataStreamReader {

	private static final Logger logger = LoggerFactory.getLogger(ForwardDataStreamReader.class);

	private final ResolvedControlMap resolvedControlMap;

	private final StreamingParser<VdypPolygon> polygonStream;
	private final StreamingParser<Collection<VdypSpecies>> layerSpeciesStream;
	private final StreamingParser<Collection<VdypUtilization>> speciesUtilizationStream;
	Optional<StreamingParser<PolygonIdentifier>> polygonDescriptionStream;

	@SuppressWarnings("unchecked")
	public ForwardDataStreamReader(ResolvedControlMap resolvedControlMap) throws ProcessingException {

		try {
			this.resolvedControlMap = resolvedControlMap;
			Map<String, Object> controlMap = resolvedControlMap.getControlMap();

			var polygonStreamFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_POLY.name());
			polygonStream = ((StreamingParserFactory<VdypPolygon>) polygonStreamFactory).get();

			var layerSpeciesStreamFactory = controlMap.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SPECIES.name());
			layerSpeciesStream = ((StreamingParserFactory<Collection<VdypSpecies>>) layerSpeciesStreamFactory).get();

			var speciesUtilizationStreamFactory = controlMap
					.get(ControlKey.FORWARD_INPUT_VDYP_LAYER_BY_SP0_BY_UTIL.name());
			speciesUtilizationStream = ((StreamingParserFactory<Collection<VdypUtilization>>) speciesUtilizationStreamFactory)
					.get();

			polygonDescriptionStream = Optional.empty();
			if (controlMap.containsKey(ControlKey.FORWARD_INPUT_GROWTO.name())) {
				var polygonDescriptionStreamFactory = Utils
						.<StreamingParserFactory<PolygonIdentifier>>expectParsedControl(
								controlMap, ControlKey.FORWARD_INPUT_GROWTO, StreamingParserFactory.class
						);

				polygonDescriptionStream = Optional.of(polygonDescriptionStreamFactory.get());
			} else {
				polygonDescriptionStream = Optional.empty();
			}
		} catch (IOException e) {
			throw new ProcessingException(e);
		}
	}

	/**
	 * Constructor that takes a raw control map. This should only be used from unit tests.
	 *
	 * @param controlMap a raw (i.e., unresolved) control map
	 * @throws ProcessingException
	 */
	ForwardDataStreamReader(Map<String, Object> controlMap) throws ProcessingException {

		this(new ForwardResolvedControlMapImpl(controlMap));
	}

	public Optional<VdypPolygon> readNextPolygon() throws ProcessingException {

		// Advance all the streams until the definition for the polygon is found.

		try {
			if (polygonStream.hasNext()) {
				var polygon = polygonStream.next();

				logger.debug("Reading polygon {}", polygon);

				var utilizationCollection = speciesUtilizationStream.next();
				var utilizationsBySpeciesMap = new HashMap<UtilizationBySpeciesKey, Map<UtilizationClass, VdypUtilization>>();
				for (var utilization : utilizationCollection) {
					logger.trace("Saw utilization {}", utilization);

					var key = new UtilizationBySpeciesKey(utilization.getLayerType(), utilization.getGenusIndex());
					utilizationsBySpeciesMap.putIfAbsent(key, new EnumMap<>(UtilizationClass.class));
					utilizationsBySpeciesMap.get(key).put(utilization.getUcIndex(), utilization);
				}

				if (polygonDescriptionStream.isPresent()) {
					var pdStream = polygonDescriptionStream.get();
					if (!pdStream.hasNext()) {
						throw new ProcessingException(
								MessageFormat.format(
										"Grow-to-year file at {0} in the control file does"
												+ " not contain a record for {1} as expected, but instead the end-of-file was reached",
										ControlKey.FORWARD_INPUT_GROWTO.name(), polygon.getPolygonIdentifier().getName()
								)
						);
					}
					var polygonDescription = pdStream.next();
					if (!polygonDescription.getName().equals(polygon.getPolygonIdentifier().getName())) {
						throw new ProcessingException(
								MessageFormat.format(
										"Grow-to-year file at {0} in the control file does"
												+ " not contain a record for {1} as expected, but instead contains a record for {2}",
										ControlKey.FORWARD_INPUT_GROWTO.name(),
										polygon.getPolygonIdentifier().getName(), polygonDescription.getName()
								)
						);
					}

					polygon.setTargetYear(polygonDescription.getYear());
				}

				var layerSpeciesSet = layerSpeciesStream.next();
				var primaryLayerSpecies = new HashMap<Integer, VdypSpecies>();
				var veteranLayerSpecies = new HashMap<Integer, VdypSpecies>();
				for (var s : layerSpeciesSet) {
					logger.trace("Saw species {}", s);

					applyGroups(polygon.getBiogeoclimaticZone(), s.getGenus(), s);

					var perSpeciesKey = new UtilizationBySpeciesKey(s.getLayerType(), s.getGenusIndex());
					var speciesUtilizations = utilizationsBySpeciesMap.get(perSpeciesKey);

					if (speciesUtilizations != null) {
						setUtilizations(s, speciesUtilizations);

						var defaultKey = new UtilizationBySpeciesKey(LayerType.PRIMARY, 0);
						Map<UtilizationClass, VdypUtilization> defaultUtilization = utilizationsBySpeciesMap
								.get(defaultKey);

						calculateSpeciesCoverage(s, defaultUtilization);
					}

					if (LayerType.PRIMARY.equals(s.getLayerType())) {
						primaryLayerSpecies.put(s.getGenusIndex(), s);
					} else if (LayerType.VETERAN.equals(s.getLayerType())) {
						veteranLayerSpecies.put(s.getGenusIndex(), s);
					} else {
						throw new IllegalStateException(
								MessageFormat.format(
										"Unrecognized layer type {} for species {} of polygon {}", s.getLayerType(),
										s.getGenusIndex(), polygon.getPolygonIdentifier()
								)
						);
					}
				}

				Map<LayerType, VdypLayer> layerMap = new HashMap<>();

				VdypLayer primaryLayer = null;
				if (primaryLayerSpecies.size() > 0) {

					var key = new UtilizationBySpeciesKey(LayerType.PRIMARY, 0);
					Map<UtilizationClass, VdypUtilization> defaultSpeciesUtilization = utilizationsBySpeciesMap
							.get(key);

					String primarySp0 = getPrimarySpecies(polygon, primaryLayerSpecies.values()).getGenus();

					primaryLayer = VdypLayer.build(builder -> {
						builder.layerType(LayerType.PRIMARY);
						builder.polygonIdentifier(polygon.getPolygonIdentifier());
						builder.inventoryTypeGroup(polygon.getInventoryTypeGroup());
						builder.addSpecies(primaryLayerSpecies.values());
						builder.primaryGenus(primarySp0);
					});

					setUtilizations(primaryLayer, defaultSpeciesUtilization);

					layerMap.put(LayerType.PRIMARY, primaryLayer);
				}

				VdypLayer veteranLayer = null;
				if (veteranLayerSpecies.size() > 0) {

					var key = new UtilizationBySpeciesKey(LayerType.VETERAN, 0);
					Map<UtilizationClass, VdypUtilization> defaultUtilization = utilizationsBySpeciesMap.get(key);

					String primarySp0 = getPrimarySpecies(polygon, veteranLayerSpecies.values()).getGenus();

					veteranLayer = VdypLayer.build(builder -> {
						builder.layerType(LayerType.VETERAN);
						builder.polygonIdentifier(polygon.getPolygonIdentifier());
						builder.inventoryTypeGroup(polygon.getInventoryTypeGroup());
						builder.addSpecies(veteranLayerSpecies.values());
						builder.primaryGenus(primarySp0);
					});

					setUtilizations(veteranLayer, defaultUtilization);

					layerMap.put(LayerType.VETERAN, veteranLayer);
				}

				polygon.setLayers(layerMap);

				UtilizationOperations.doPostCreateAdjustments(polygon);

				return Optional.of(polygon);
			} else {
				return Optional.empty();
			}
		} catch (ResourceParseException | IOException e) {
			throw new ProcessingException(e);
		}
	}

	private static VdypSpecies getPrimarySpecies(VdypPolygon polygon, Collection<VdypSpecies> speciesList)
			throws ProcessingException {

		var primarySpecies = speciesList.stream().filter(s -> s.getSite().isPresent()).toList();
		if (primarySpecies.size() == 0) {
			throw new ProcessingException(
					MessageFormat.format(
							"Primary layer of {0} does not contain a primary species",
							polygon.getPolygonIdentifier().toStringCompact()
					)
			);
		} else if (primarySpecies.size() > 1) {
			throw new ProcessingException(
					MessageFormat.format(
							"Primary layer of {0} contains multiple primary species: {1}",
							polygon.getPolygonIdentifier().toStringCompact(),
							String.join(", ", primarySpecies.stream().map(s -> s.getGenus()).toList())
					)
			);
		}

		return primarySpecies.get(0);
	}

	private void calculateSpeciesCoverage(VdypSpecies s, Map<UtilizationClass, VdypUtilization> defaultUtilization) {

		float speciesCoverage = s.getBaseAreaByUtilization().get(UtilizationClass.ALL)
				/ defaultUtilization.get(UtilizationClass.ALL).getBasalArea();

		s.setPercentGenus(speciesCoverage);
	}

	protected void applyGroups(BecDefinition bec, String genus, VdypSpecies species) {
		// Look up Volume group, Decay Group, and Breakage group for each species.

		try {
			// VGRPFIND
			var volumeGroupMap = resolvedControlMap.getVolumeEquationGroups();
			var volumeGroup = volumeGroupMap.get(genus, bec.getVolumeBec().getAlias());
			species.setVolumeGroup(volumeGroup);
		} catch (NoSuchElementException e) {
			// group will remain undefined
		}

		try {
			// DGRPFIND
			var decayGroupMap = resolvedControlMap.getDecayEquationGroups();
			var decayGroup = decayGroupMap.get(genus, bec.getDecayBec().getAlias());
			species.setDecayGroup(decayGroup);
		} catch (NoSuchElementException e) {
			// group will remain undefined
		}

		try {
			// BGRPFIND (Breakage uses decay BEC)
			var breakageGroupMap = resolvedControlMap.getBreakageEquationGroups();
			var breakageGroup = breakageGroupMap.get(genus, bec.getDecayBec().getAlias());
			species.setBreakageGroup(breakageGroup);
		} catch (NoSuchElementException e) {
			// group will remain undefined
		}
	}

	private void setUtilizations(VdypUtilizationHolder u, Map<UtilizationClass, VdypUtilization> speciesUtilizations) {

		for (var e : speciesUtilizations.entrySet()) {
			var uc = e.getKey();
			var ucUtilizations = e.getValue();

			u.getBaseAreaByUtilization().set(uc, ucUtilizations.getBasalArea());
			u.getTreesPerHectareByUtilization().set(uc, ucUtilizations.getLiveTreesPerHectare());
			if (uc == UtilizationClass.SMALL || uc == UtilizationClass.ALL) {
				u.getLoreyHeightByUtilization().set(uc, ucUtilizations.getLoreyHeight());
			}
			u.getWholeStemVolumeByUtilization().set(uc, ucUtilizations.getWholeStemVolume());
			u.getCloseUtilizationVolumeByUtilization().set(uc, ucUtilizations.getCloseUtilizationVolume());
			u.getCloseUtilizationVolumeNetOfDecayByUtilization().set(uc, ucUtilizations.getCuVolumeMinusDecay());
			u.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.set(uc, ucUtilizations.getCuVolumeMinusDecayWastage());
			u.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization()
					.set(uc, ucUtilizations.getCuVolumeMinusDecayWastageBreakage());
			u.getQuadraticMeanDiameterByUtilization().set(uc, ucUtilizations.getQuadraticMeanDiameterAtBH());
		}
	}

	private class UtilizationBySpeciesKey {
		private final LayerType layerType;
		private final Integer speciesIndex;

		public UtilizationBySpeciesKey(LayerType layerType, Integer speciesIndex) {
			this.layerType = layerType;
			this.speciesIndex = speciesIndex;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof UtilizationBySpeciesKey that) {
				return layerType.equals(that.layerType) && speciesIndex.equals(that.speciesIndex);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return layerType.hashCode() * 17 + speciesIndex.hashCode();
		}
	}
}
