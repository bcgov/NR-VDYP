package ca.bc.gov.nrs.vdyp.forward;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

class Bank {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(Bank.class);

	private static final int N_UTILIZATION_CLASSES = UtilizationClass.values().length;

	private final VdypLayer layer;
	private final BecDefinition becZone;

	/**
	 * The number of species in the state. Note that all arrays have this value plus one elements in them; the element
	 * at index 0 is unused for the species values* and contains the default utilization in the Utilization values.
	 *
	 * (*) except: siteCurveNumbers[0] is used to store the site curve of the primary species.
	 */
	private int nSpecies; // BANK1 NSPB
	private int[] indices;

	// Species information

	public final String[/* nSpecies + 1 */] speciesNames; // BANK2 SP0B
	public final Sp64DistributionSet[/* nSpecies + 1 */] sp64Distributions; // BANK2 SP64DISTB
	public final float[/* nSpecies + 1 */] siteIndices; // BANK3 SIB
	public final float[/* nSpecies + 1 */] dominantHeights; // BANK3 HDB
	public final float[/* nSpecies + 1 */] ageTotals; // BANK3 AGETOTB
	public final float[/* nSpecies + 1 */] yearsAtBreastHeight; // BANK3 AGEBHB
	public final float[/* nSpecies + 1 */] yearsToBreastHeight; // BANK3 YTBHB
	public final int[/* nSpecies + 1 */] siteCurveNumbers; // BANK3 SCNB
	public final int[/* nSpecies + 1 */] speciesIndices; // BANK1 ISPB
	public final float[/* nSpecies + 1 */] percentagesOfForestedLand; // BANK1 PCTB

	// Utilization information, per Species

	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] basalAreas; // BANK1 BAB. Units: m^2/hectare
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] closeUtilizationVolumes; // BANK1 VOLCUB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] cuVolumesMinusDecay; // BANK1 VOL_DB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] cuVolumesMinusDecayAndWastage; // BANK1 VOL_DW_B
	public final float[/* nSpecies + 1, including 0 */][/* uc -1 and 0 only */] loreyHeights; // BANK1 HLB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] quadMeanDiameters; // BANK1 DQB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] treesPerHectare; // BANK1 TPHB
	public final float[/* nSpecies + 1, including 0 */][/* all ucs */] wholeStemVolumes; // BANK1 VOLWSB

	public Bank(VdypLayer layer, BecDefinition becZone, Predicate<VdypSpecies> retainCriteria) {

		this.layer = layer;
		this.becZone = becZone;

		List<VdypSpecies> speciesToRetain = layer.getSpecies().values().stream().filter(s -> retainCriteria.test(s))
				.sorted((s1, s2) -> s1.getGenusIndex() - s2.getGenusIndex()).toList();

		this.nSpecies = speciesToRetain.size();
		this.indices = IntStream.range(1, nSpecies + 1).toArray();

		// In the following, index 0 is unused
		speciesNames = new String[nSpecies + 1];
		sp64Distributions = new Sp64DistributionSet[getNSpecies() + 1];
		siteIndices = new float[nSpecies + 1];
		dominantHeights = new float[nSpecies + 1];
		ageTotals = new float[nSpecies + 1];
		yearsAtBreastHeight = new float[nSpecies + 1];
		yearsToBreastHeight = new float[nSpecies + 1];
		siteCurveNumbers = new int[nSpecies + 1];
		speciesIndices = new int[nSpecies + 1];
		percentagesOfForestedLand = new float[nSpecies + 1];

		// In the following, index 0 is used for the default species utilization
		basalAreas = new float[nSpecies + 1][N_UTILIZATION_CLASSES];
		closeUtilizationVolumes = new float[nSpecies + 1][N_UTILIZATION_CLASSES];
		cuVolumesMinusDecay = new float[nSpecies + 1][N_UTILIZATION_CLASSES];
		cuVolumesMinusDecayAndWastage = new float[nSpecies + 1][N_UTILIZATION_CLASSES];
		loreyHeights = new float[nSpecies + 1][2];
		quadMeanDiameters = new float[nSpecies + 1][N_UTILIZATION_CLASSES];
		treesPerHectare = new float[nSpecies + 1][N_UTILIZATION_CLASSES];
		wholeStemVolumes = new float[nSpecies + 1][N_UTILIZATION_CLASSES];

		int nextSlot = 1;
		for (VdypSpecies s : speciesToRetain) {
			transferSpeciesIntoBank(nextSlot++, s);
		}

		transferUtilizationSetIntoBank(0, layer);

		// BANKCHK1 - calculate UC All values from components (rather than rely on the values
		// provided in the input.)

		setCalculateUtilizationClassAllValues();
	}

	public Bank(Bank source) {

		this.becZone = source.becZone;
		this.layer = source.layer;

		this.nSpecies = source.nSpecies;
		this.indices = copy(source.indices);
		this.speciesNames = copy(source.speciesNames);
		this.speciesIndices = copy(source.speciesIndices);

		this.siteCurveNumbers = copy(source.siteCurveNumbers);
		this.sp64Distributions = copy(source.sp64Distributions);

		this.ageTotals = copy(source.ageTotals);
		this.dominantHeights = copy(source.dominantHeights);
		this.percentagesOfForestedLand = copy(source.percentagesOfForestedLand);
		this.siteIndices = copy(source.siteIndices);
		this.yearsAtBreastHeight = copy(source.yearsAtBreastHeight);
		this.yearsToBreastHeight = copy(source.yearsToBreastHeight);

		this.basalAreas = copy(source.basalAreas);
		this.closeUtilizationVolumes = copy(source.closeUtilizationVolumes);
		this.cuVolumesMinusDecay = copy(source.cuVolumesMinusDecay);
		this.cuVolumesMinusDecayAndWastage = copy(source.cuVolumesMinusDecayAndWastage);
		this.loreyHeights = copy(source.loreyHeights);
		this.quadMeanDiameters = copy(source.quadMeanDiameters);
		this.treesPerHectare = copy(source.treesPerHectare);
		this.wholeStemVolumes = copy(source.wholeStemVolumes);
	}

	public int getNSpecies() {
		return nSpecies;
	}

	int[] getIndices() {
		return indices;
	}

	BecDefinition getBecZone() {
		return becZone;
	}

	/**
	 * Refresh the values in the bank with an updated version (given) of the layer used to create the bank. The
	 * modifications cannot include any changes to the set of species, although the details of those species may of
	 * course change.
	 *
	 * @param layer a (presumably modified) version of the layer.
	 * @throws ProcessingException
	 */
	void refreshBank(VdypLayer layer) throws ProcessingException {

		if (!this.layer.equals(layer)) {
			throw new IllegalArgumentException(
					MessageFormat.format(
							"One cannot refresh a bank from a"
									+ " layer ({0}) different from the one used to create the bank ({1})",
							this.layer, layer
					)
			);
		}

		List<VdypSpecies> species = layer.getSpecies().values().stream()
				.sorted((s1, s2) -> s1.getGenusIndex() - s2.getGenusIndex()).toList();

		transferUtilizationSetIntoBank(0, layer);

		int nextSlot = 1;
		for (VdypSpecies s : species) {
			transferSpeciesIntoBank(nextSlot++, s);
		}
	}

	private void transferSpeciesIntoBank(int index, VdypSpecies species) {

		speciesNames[index] = species.getGenus();
		sp64Distributions[index] = species.getSp64DistributionSet();
		speciesIndices[index] = species.getGenusIndex();

		species.getSite().ifPresentOrElse(s -> {
			siteIndices[index] = s.getSiteIndex().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			dominantHeights[index] = s.getHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			ageTotals[index] = s.getAgeTotal().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			yearsToBreastHeight[index] = s.getYearsToBreastHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE);
			if (ageTotals[index] != VdypEntity.MISSING_FLOAT_VALUE
					&& yearsToBreastHeight[index] != VdypEntity.MISSING_FLOAT_VALUE) {
				yearsAtBreastHeight[index] = ageTotals[index] - yearsToBreastHeight[index];
			} else {
				yearsAtBreastHeight[index] = VdypEntity.MISSING_FLOAT_VALUE;
			}
			siteCurveNumbers[index] = s.getSiteCurveNumber().orElse(VdypEntity.MISSING_INTEGER_VALUE);
			// percentForestedLand is output-only and so not assigned here.
		}, () -> {
			siteIndices[index] = VdypEntity.MISSING_FLOAT_VALUE;
			dominantHeights[index] = VdypEntity.MISSING_FLOAT_VALUE;
			ageTotals[index] = VdypEntity.MISSING_FLOAT_VALUE;
			yearsToBreastHeight[index] = VdypEntity.MISSING_FLOAT_VALUE;
			yearsAtBreastHeight[index] = VdypEntity.MISSING_FLOAT_VALUE;
			siteCurveNumbers[index] = VdypEntity.MISSING_INTEGER_VALUE;
		});

		transferUtilizationSetIntoBank(index, species);
	}

	private void transferUtilizationSetIntoBank(int index, VdypUtilizationHolder uh) {

		for (UtilizationClass uc : UtilizationClass.values()) {
			int ucIndex = uc.ordinal();
			basalAreas[index][ucIndex] = uh.getBaseAreaByUtilization().get(uc);
			closeUtilizationVolumes[index][ucIndex] = uh.getCloseUtilizationVolumeByUtilization().get(uc);
			cuVolumesMinusDecay[index][ucIndex] = uh.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc);
			cuVolumesMinusDecayAndWastage[index][ucIndex] = uh
					.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc);
			if (ucIndex < 2 /* only uc 0 and 1 have a lorey height */) {
				loreyHeights[index][ucIndex] = uh.getLoreyHeightByUtilization().get(uc);
			}
			quadMeanDiameters[index][ucIndex] = uh.getQuadraticMeanDiameterByUtilization().get(uc);
			treesPerHectare[index][ucIndex] = uh.getTreesPerHectareByUtilization().get(uc);
			wholeStemVolumes[index][ucIndex] = uh.getWholeStemVolumeByUtilization().get(uc);
		}
	}

	/**
	 * For each species, set uc All to the sum of the UC values, UC 7.5 and above only, for the summable values, and
	 * calculate quad-mean-diameter from these values.
	 * <p>
	 * For the layer, set uc All values (for summable types) to the sum of those of the individual species and set the
	 * other uc values to the sum of those of the individual species. Calculate the uc All value for quad-mean-diameter,
	 * and the uc All and Small value for lorey-height.
	 */
	private void setCalculateUtilizationClassAllValues() {

		int layerIndex = 0;
		int ucAllIndex = UtilizationClass.ALL.ordinal();
		int ucSmallIndex = UtilizationClass.SMALL.ordinal();

		// Each species

		for (int sp0Index : indices) {

			basalAreas[sp0Index][ucAllIndex] = sumUtilizationClassValues(
					basalAreas[sp0Index], UtilizationClass.UTIL_CLASSES
			);
			treesPerHectare[sp0Index][ucAllIndex] = sumUtilizationClassValues(
					treesPerHectare[sp0Index], UtilizationClass.UTIL_CLASSES
			);
			wholeStemVolumes[sp0Index][ucAllIndex] = sumUtilizationClassValues(
					wholeStemVolumes[sp0Index], UtilizationClass.UTIL_CLASSES
			);
			closeUtilizationVolumes[sp0Index][ucAllIndex] = sumUtilizationClassValues(
					closeUtilizationVolumes[sp0Index], UtilizationClass.UTIL_CLASSES
			);
			cuVolumesMinusDecay[sp0Index][ucAllIndex] = sumUtilizationClassValues(
					cuVolumesMinusDecay[sp0Index], UtilizationClass.UTIL_CLASSES
			);
			cuVolumesMinusDecayAndWastage[sp0Index][ucAllIndex] = sumUtilizationClassValues(
					cuVolumesMinusDecayAndWastage[sp0Index], UtilizationClass.UTIL_CLASSES
			);

			if (basalAreas[sp0Index][ucAllIndex] > 0.0f) {
				quadMeanDiameters[sp0Index][ucAllIndex] = BaseAreaTreeDensityDiameter
						.quadMeanDiameter(basalAreas[sp0Index][ucAllIndex], treesPerHectare[sp0Index][ucAllIndex]);
			}
		}

		// Layer

		basalAreas[layerIndex][ucAllIndex] = sumSpeciesUtilizationClassValues(basalAreas, UtilizationClass.ALL);
		treesPerHectare[layerIndex][ucAllIndex] = sumSpeciesUtilizationClassValues(
				treesPerHectare, UtilizationClass.ALL
		);
		wholeStemVolumes[layerIndex][ucAllIndex] = sumSpeciesUtilizationClassValues(
				wholeStemVolumes, UtilizationClass.ALL
		);
		closeUtilizationVolumes[layerIndex][ucAllIndex] = sumSpeciesUtilizationClassValues(
				closeUtilizationVolumes, UtilizationClass.ALL
		);
		cuVolumesMinusDecay[layerIndex][ucAllIndex] = sumSpeciesUtilizationClassValues(
				cuVolumesMinusDecay, UtilizationClass.ALL
		);
		cuVolumesMinusDecayAndWastage[layerIndex][ucAllIndex] = sumSpeciesUtilizationClassValues(
				cuVolumesMinusDecayAndWastage, UtilizationClass.ALL
		);

		// Calculate the layer's uc All values for quad-mean-diameter and lorey height

		float sumLoreyHeightByBasalAreaSmall = 0.0f;
		float sumBasalAreaSmall = 0.0f;
		float sumLoreyHeightByBasalAreaAll = 0.0f;

		for (int sp0Index : indices) {
			sumLoreyHeightByBasalAreaSmall += loreyHeights[sp0Index][ucSmallIndex] * basalAreas[sp0Index][ucSmallIndex];
			sumBasalAreaSmall += basalAreas[sp0Index][ucSmallIndex];
			sumLoreyHeightByBasalAreaAll += loreyHeights[sp0Index][ucAllIndex] * basalAreas[sp0Index][ucAllIndex];
		}

		if (basalAreas[layerIndex][ucAllIndex] > 0.0f) {
			quadMeanDiameters[layerIndex][ucAllIndex] = BaseAreaTreeDensityDiameter
					.quadMeanDiameter(basalAreas[layerIndex][ucAllIndex], treesPerHectare[layerIndex][ucAllIndex]);
			loreyHeights[layerIndex][ucAllIndex] = sumLoreyHeightByBasalAreaAll / basalAreas[layerIndex][ucAllIndex];
		}

		// Calculate the layer's lorey height uc Small value

		if (sumBasalAreaSmall > 0.0f) {
			loreyHeights[layerIndex][ucSmallIndex] = sumLoreyHeightByBasalAreaSmall / sumBasalAreaSmall;
		}

		// Finally, set the layer's summable UC values (other than All, which was computed above) to
		// the sums of those of each of the species.

		for (UtilizationClass uc : UtilizationClass.ALL_CLASSES) {
			basalAreas[layerIndex][uc.ordinal()] = sumSpeciesUtilizationClassValues(basalAreas, uc);
			treesPerHectare[layerIndex][uc.ordinal()] = sumSpeciesUtilizationClassValues(treesPerHectare, uc);
			wholeStemVolumes[layerIndex][uc.ordinal()] = sumSpeciesUtilizationClassValues(wholeStemVolumes, uc);
			closeUtilizationVolumes[layerIndex][uc.ordinal()] = sumSpeciesUtilizationClassValues(
					closeUtilizationVolumes, uc
			);
			cuVolumesMinusDecay[layerIndex][uc.ordinal()] = sumSpeciesUtilizationClassValues(cuVolumesMinusDecay, uc);
			cuVolumesMinusDecayAndWastage[layerIndex][uc.ordinal()] = sumSpeciesUtilizationClassValues(
					cuVolumesMinusDecayAndWastage, uc
			);
		}
	}

	private float sumUtilizationClassValues(float[] ucValues, List<UtilizationClass> subjects) {
		float sum = 0.0f;

		for (UtilizationClass uc : UtilizationClass.values()) {
			if (subjects.contains(uc)) {
				sum += ucValues[uc.ordinal()];
			}
		}

		return sum;
	}

	private float sumSpeciesUtilizationClassValues(float[][] ucValues, UtilizationClass uc) {
		float sum = 0.0f;

		for (int sp0Index : this.indices) {
			sum += ucValues[sp0Index][uc.ordinal()];
		}

		return sum;
	}

	/**
	 * This method copies the Bank contents out to the VdypLayer instance used to create it and returns that. It is a
	 * relatively expensive operation and should not be called without due consideration.
	 *
	 * @return as described
	 */
	VdypLayer buildLayerFromBank() {

		transferUtilizationsFromBank(0, layer);

		Collection<VdypSpecies> newSpecies = new ArrayList<>();
		for (int i : indices) {
			newSpecies.add(transferSpeciesFromBank(i, layer.getSpecies().get(speciesNames[i])));
		}
		layer.setSpecies(newSpecies);

		return layer;
	}

	private VdypSpecies transferSpeciesFromBank(int index, VdypSpecies species) {

		VdypSpecies newSpecies = VdypSpecies.build(speciesBuilder -> {
			speciesBuilder.copy(species);
			speciesBuilder.percentGenus(this.percentagesOfForestedLand[index]);
			species.getSite().ifPresentOrElse(site -> speciesBuilder.addSite(VdypSite.build(siteBuilder -> {
				siteBuilder.copy(site);
				siteBuilder.siteGenus(this.speciesNames[index]);
				siteBuilder.ageTotal(Utils.optFloat(ageTotals[index]));
				siteBuilder.height(Utils.optFloat(this.dominantHeights[index]));
				siteBuilder.siteCurveNumber(Utils.optInt(this.siteCurveNumbers[index]));
				siteBuilder.siteIndex(Utils.optFloat(this.siteIndices[index]));
				siteBuilder.yearsToBreastHeight(Utils.optFloat(this.yearsToBreastHeight[index]));
			})), () -> {
				VdypSite site = VdypSite.build(siteBuilder -> {
					siteBuilder.polygonIdentifier(species.getPolygonIdentifier());
					siteBuilder.layerType(species.getLayerType());
					siteBuilder.siteGenus(this.speciesNames[index]);
					siteBuilder.ageTotal(Utils.optFloat(this.ageTotals[index]));
					siteBuilder.height(Utils.optFloat(this.dominantHeights[index]));
					siteBuilder.siteCurveNumber(Utils.optInt(this.siteCurveNumbers[index]));
					siteBuilder.siteIndex(Utils.optFloat(this.siteIndices[index]));
					siteBuilder.yearsToBreastHeight(Utils.optFloat(this.yearsToBreastHeight[index]));
				});

				speciesBuilder.addSite(site);
			});
		});

		transferUtilizationsFromBank(index, newSpecies);

		return newSpecies;
	}

	private void transferUtilizationsFromBank(int index, VdypUtilizationHolder uh) {

		for (UtilizationClass uc : UtilizationClass.values()) {
			int ucIndex = uc.ordinal();
			uh.getBaseAreaByUtilization().set(uc, basalAreas[index][ucIndex]);
			uh.getCloseUtilizationVolumeByUtilization().set(uc, closeUtilizationVolumes[index][ucIndex]);
			uh.getCloseUtilizationVolumeNetOfDecayByUtilization().set(uc, cuVolumesMinusDecay[index][ucIndex]);
			uh.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization()
					.set(uc, cuVolumesMinusDecayAndWastage[index][ucIndex]);
			if (ucIndex < 2 /* only uc 0 and 1 have a lorey height */) {
				uh.getLoreyHeightByUtilization().set(uc, loreyHeights[index][ucIndex]);
			}
			uh.getQuadraticMeanDiameterByUtilization().set(uc, quadMeanDiameters[index][ucIndex]);
			uh.getTreesPerHectareByUtilization().set(uc, treesPerHectare[index][ucIndex]);
			uh.getWholeStemVolumeByUtilization().set(uc, wholeStemVolumes[index][ucIndex]);
		}
	}

	public Bank copy() {
		return new Bank(this);
	}

	private String[] copy(String[] a) {
		return Arrays.stream(a).toArray(String[]::new);
	}

	private int[] copy(int[] a) {
		int[] t = new int[a.length];

		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	private float[] copy(float[] a) {
		float[] t = new float[a.length];

		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	private float[][] copy(float[][] a) {
		return Arrays.stream(a).map(float[]::clone).toArray(float[][]::new);
	}

	private Sp64DistributionSet[] copy(Sp64DistributionSet[] sp64Distributions) {
		return Arrays.stream(sp64Distributions).map(s -> s == null ? null : s.copy())
				.toArray(Sp64DistributionSet[]::new);
	}
}