package ca.bc.gov.nrs.vdyp.fip.model;

import java.util.Map;

import ca.bc.gov.nrs.vdyp.model.Layer;

public class FipSpecies {

	static final String POLYGON_IDENTIFIER = "POLYGON_IDENTIFIER"; // POLYDESC
	static final String LAYER = "LAYER"; // LAYER

	final String polygonIdentifier; // POLYDESC
	final Layer layer; // LAYER
	final String genus; // SP0

	float percentGenus;

	// This is computed from percentGenus, but VDYP7 computes it (RFBASP0/FR) in a
	// way that might lead to a slight difference so it's stored separately and can
	// be modified.
	float fractionGenus;

	Map<String, Float> speciesPercent;

	public FipSpecies(
			String polygonIdentifier, Layer layer, String genus, float percentGenus, Map<String, Float> speciesPercent
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.layer = layer;
		this.genus = genus;

		this.setPercentGenus(percentGenus);

		this.speciesPercent = speciesPercent;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public Layer getLayer() {
		return layer;
	}

	public float getPercentGenus() {
		return percentGenus;
	}

	public float getFractionGenus() {
		return fractionGenus;
	}

	public void setPercentGenus(float percentGenus) {
		this.percentGenus = percentGenus;
		this.fractionGenus = percentGenus / 100f;
	}

	public void setFractionGenus(float fractionGenus) {
		this.fractionGenus = fractionGenus;
	}

	public Map<String, Float> getSpeciesPercent() {
		return speciesPercent;
	}

	public void setSpeciesPercent(Map<String, Float> speciesPercent) {
		this.speciesPercent = speciesPercent;
	}

	public String getGenus() {
		return genus;
	}

}
