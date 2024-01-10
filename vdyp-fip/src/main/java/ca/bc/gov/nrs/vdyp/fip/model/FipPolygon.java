package ca.bc.gov.nrs.vdyp.fip.model;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.model.LayerType;

public class FipPolygon {

	String polygonIdentifier; // FIP_P/POLYDESC
	String forestInventoryZone; // FIP_P/FIZ
	String biogeoclimaticZone; // FIP_P/BEC
	Optional<Float> percentAvailable; // FIP_P2/PCTFLAND
	Optional<FipMode> modeFip; // FIP_P2/MODE / MODEfip
	Optional<String> nonproductiveDescription; // FIP_P3/NPDESC
	float yieldFactor; // FIP_P4/YLDFACT

	Map<LayerType, FipLayer> layers = Collections.emptyMap();

	public FipPolygon(
			String polygonIdentifier, String fiz, String becIdentifier, Optional<Float> percentAvailable,
			Optional<FipMode> modeFip, Optional<String> nonproductiveDescription, float yieldFactor
	) {
		super();
		this.polygonIdentifier = polygonIdentifier;
		this.forestInventoryZone = fiz;
		this.biogeoclimaticZone = becIdentifier;
		this.percentAvailable = percentAvailable;
		this.modeFip = modeFip;
		this.nonproductiveDescription = nonproductiveDescription;
		this.yieldFactor = yieldFactor;
	}

	public String getPolygonIdentifier() {
		return polygonIdentifier;
	}

	public void setPolygonIdentifier(String polygonIdentifier) {
		this.polygonIdentifier = polygonIdentifier;
	}

	public String getForestInventoryZone() {
		return forestInventoryZone;
	}

	public void setForestInventoryZone(String forestInventoryZone) {
		this.forestInventoryZone = forestInventoryZone;
	}

	public String getBiogeoclimaticZone() {
		return biogeoclimaticZone;
	}

	public void setBiogeoclimaticZone(String biogeoclimaticZone) {
		this.biogeoclimaticZone = biogeoclimaticZone;
	}

	public Optional<Float> getPercentAvailable() {
		return percentAvailable;
	}

	public void setPercentAvailable(Optional<Float> percentAvailable) {
		this.percentAvailable = percentAvailable;
	}

	public Optional<FipMode> getModeFip() {
		return modeFip;
	}

	public void setModeFip(Optional<FipMode> modeFip) {
		this.modeFip = modeFip;
	}

	public Optional<String> getNonproductiveDescription() {
		return nonproductiveDescription;
	}

	public void setNonproductiveDescription(Optional<String> nonproductiveDescription) {
		this.nonproductiveDescription = nonproductiveDescription;
	}

	public float getYieldFactor() {
		return yieldFactor;
	}

	public void setYieldFactor(float yieldFactor) {
		this.yieldFactor = yieldFactor;
	}

	public Map<LayerType, FipLayer> getLayers() {
		return layers;
	}

	public void setLayers(Map<LayerType, FipLayer> layers) {
		this.layers = layers;
	}

}
