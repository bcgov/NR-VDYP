package ca.bc.gov.nrs.vdyp.forward.model;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.GenusDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;

public class VdypPolygonLayer {

	// See IPSJF155.doc

	private final LayerType layerType; // LAYERG

	private final VdypPolygon parent;
	private final Map<GenusDefinition, VdypLayerSpecies> genus;
	private final Map<UtilizationClass, VdypSpeciesUtilization> defaultUtilizationMap;

	public VdypPolygonLayer(LayerType layerType, VdypPolygon parent, Map<GenusDefinition, VdypLayerSpecies> genus,
			Map<UtilizationClass, VdypSpeciesUtilization> defaultUtilizationMap) {
		this.layerType = layerType;
		this.parent = parent;
		this.genus = genus;
		this.defaultUtilizationMap = defaultUtilizationMap;
	}

	public LayerType getLayerType() {
		return layerType;
	}

	public VdypPolygon getParent() {
		return parent;
	}

	public Map<GenusDefinition, VdypLayerSpecies> getGenus() {
		return genus;
	}

	public Map<UtilizationClass, VdypSpeciesUtilization> getDefaultUtilizationMap() {
		return defaultUtilizationMap;
	}
}
