package ca.bc.gov.nrs.vdyp.fip.model;

import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.Computed;
import ca.bc.gov.nrs.vdyp.model.LayerType;

public class FipLayerPrimary extends FipLayer {

	static final String SITE_CURVE_NUMBER = "SITE_CURVE_NUMBER"; // SCN
	static final String STOCKING_CLASS = "STOCKING_CLASS"; // STK

	// TODO Confirm if these should be required instead of optional if we know it's
	// a Primary layer.
	private Optional<Integer> siteCurveNumber = Optional.empty(); // FIPL_1/SCN_L1

	private Optional<Character> stockingClass = Optional.empty(); // FIPL_1ST/STK_L1

	private String primaryGenus; // FIPL_1C/JPRIME

	private int inventoryTypeGroup = 0;

	public FipLayerPrimary(String polygonIdentifier, float ageTotal) {
		super(polygonIdentifier, LayerType.PRIMARY, ageTotal);
	}

	public Optional<Integer> getSiteCurveNumber() {
		return siteCurveNumber;
	}

	public void setSiteCurveNumber(Optional<Integer> siteCurveNumber) {
		this.siteCurveNumber = siteCurveNumber;
	}

	public Optional<Character> getStockingClass() {
		return stockingClass;
	}

	public void setStockingClass(Optional<Character> stockingClass) {
		this.stockingClass = stockingClass;
	}

	public String getPrimaryGenus() {
		return primaryGenus;
	}

	public void setPrimaryGenus(String primaryGenus) {
		this.primaryGenus = primaryGenus;
	}

	@Computed
	public FipSpecies getPrimarySpeciesRecord() {
		return getSpecies().get(primaryGenus);
	}

	public int getInventoryTypeGroup() {
		return inventoryTypeGroup;
	}

	public void setInventoryTypeGroup(int inventoryTypeGroup) {
		this.inventoryTypeGroup = inventoryTypeGroup;
	}
}
