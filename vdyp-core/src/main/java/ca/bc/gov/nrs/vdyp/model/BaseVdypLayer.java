package ca.bc.gov.nrs.vdyp.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseVdypLayer<S extends BaseVdypSpecies> {

	// TODO I think polygonidentifier and layer might be FIP specific so possibly
	// push them down to FipLayer?
	protected final String polygonIdentifier;
	protected final LayerType layer;
	protected float ageTotal; // LVCOM3/AGETOTLV, L1COM3/AGETOTL1
	protected float height; // LVCOM3/HDLV, L1COM3/HDL1
	protected float yearsToBreastHeight; // LVCOM3/YTBHLV, L1COM3/YTBHL1
	LinkedHashMap<String, S> species = new LinkedHashMap<>();

	public BaseVdypLayer(String polygonIdentifier, LayerType layer) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.layer = layer;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public LayerType getLayer() {
		return layer;
	}

	public float getAgeTotal() {
		return ageTotal;
	}

	public float getHeight() {
		return height;
	}

	public float getYearsToBreastHeight() {
		return yearsToBreastHeight;
	}

	public void setAgeTotal(float ageTotal) {
		this.ageTotal = ageTotal;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public void setYearsToBreastHeight(float yearsToBreastHeight) {
		this.yearsToBreastHeight = yearsToBreastHeight;
	}

	public LinkedHashMap<String, S> getSpecies() {
		return species;
	}

	public void setSpecies(Map<String, S> species) {
		this.species.clear();
		this.species.putAll(species);
	}

	public void setSpecies(Collection<S> species) {
		this.species.clear();
		species.forEach(spec -> this.species.put(spec.getGenus(), spec));
	}

}
