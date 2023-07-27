package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;

public class VdypSpecies extends BaseVdypSpecies {

	Coefficients baseAreaByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/BA
	Coefficients loreyHeightByUtilization = new Coefficients(Arrays.asList(0f, 0f), -1); // LVCOM/HL
	Coefficients quadraticMeanDiameterByUtilization = new Coefficients(Arrays.asList(0f, 0f, 0f, 0f, 0f, 0f), -1); // LVCOM/DQ

	int volumeGroup;
	int decayGroup;
	int breakageGroup;

	public VdypSpecies(String polygonIdentifier, Layer layer, String genus) {
		super(polygonIdentifier, layer, genus);
	}

	public VdypSpecies(BaseVdypSpecies toCopy) {
		super(toCopy);
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	public Coefficients getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	public void setBaseAreaByUtilization(Coefficients baseAreaByUtilization) {
		assert baseAreaByUtilization.indexFrom == -1 : "baseAreaByUtilization must be indexed from -1";
		assert baseAreaByUtilization.size() == 6 : "baseAreaByUtilization must have index -1 - 4";
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	public Coefficients getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	public void setLoreyHeightByUtilization(Coefficients loreyHeightByUtilization) {
		assert loreyHeightByUtilization.indexFrom == -1 : "loreyHeightByUtilization must be indexed from -1";
		assert loreyHeightByUtilization.size() == 2 : "loreyHeightByUtilization must have index -1 - 0";
		this.loreyHeightByUtilization = loreyHeightByUtilization;
	}

	public int getVolumeGroup() {
		return volumeGroup;
	}

	public void setVolumeGroup(int volumeGroup) {
		this.volumeGroup = volumeGroup;
	}

	public int getDecayGroup() {
		return decayGroup;
	}

	public void setDecayGroup(int decayGroup) {
		this.decayGroup = decayGroup;
	}

	public int getBreakageGroup() {
		return breakageGroup;
	}

	public void setBreakageGroup(int breakageGroup) {
		this.breakageGroup = breakageGroup;
	}

	public Coefficients getQuadraticMeanDiameterByUtilization() {
		return quadraticMeanDiameterByUtilization;
	}

	public void setQuadraticMeanDiameterByUtilization(Coefficients quadraticMeanDiameterByUtilization) {
		assert loreyHeightByUtilization.indexFrom == -1 : "quadraticMeanDiameterByUtilization must be indexed from -1";
		assert loreyHeightByUtilization.size() == 6 : "quadraticMeanDiameterByUtilization must have index -1 - 4";
		this.quadraticMeanDiameterByUtilization = quadraticMeanDiameterByUtilization;
	}

}
