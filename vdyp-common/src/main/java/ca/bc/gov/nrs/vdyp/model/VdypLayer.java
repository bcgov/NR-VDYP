package ca.bc.gov.nrs.vdyp.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.common.Computed;

public class VdypLayer extends BaseVdypLayer<VdypSpecies> implements VdypUtilizationHolder {

	Coefficients baseAreaByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/BA species 0
	Coefficients loreyHeightByUtilization = //
			VdypUtilizationHolder.emptyLoreyHeightUtilization(); // LVCOM/HL species 0
	Coefficients quadraticMeanDiameterByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/DQ species 0
	Coefficients treesPerHectareByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/TPH species 0

	Coefficients wholeStemVolumeByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOLWS species 0
	Coefficients closeUtilizationVolumeByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOLCU species/ 0
	Coefficients closeUtilizationVolumeNetOfDecayByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOL_D species 0
	Coefficients closeUtilizationVolumeNetOfDecayAndWasteByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOL_DW species 0
	Coefficients closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = //
			VdypUtilizationHolder.emptyUtilization(); // LVCOM/VOL_DWB species 0

	private Optional<String> dominantSpecies;

	public VdypLayer(
			String polygonIdentifier, LayerType layer, Optional<Float> ageTotal, Optional<Float> height,
			Optional<Float> yearsToBreastHeight, Optional<Float> siteIndex, Optional<Integer> siteCurveNumber,
			Optional<Integer> inventoryTypeGroup, Optional<String> siteGenus
	) {
		super(
				polygonIdentifier, layer, ageTotal, height, yearsToBreastHeight, siteIndex, siteCurveNumber,
				inventoryTypeGroup, siteGenus
		);
	}

	@Computed
	public Optional<Float> getBreastHeightAge() {
		return this.getAgeTotal().flatMap(at -> this.getYearsToBreastHeight().map(bha -> at - bha));
	}

	@Override
	public Coefficients getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	@Override
	public void setBaseAreaByUtilization(Coefficients baseAreaByUtilization) {
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	@Override
	public Coefficients getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	@Override
	public void setLoreyHeightByUtilization(Coefficients loreyHeightByUtilization) {
		this.loreyHeightByUtilization = loreyHeightByUtilization;
	}

	@Override
	public Coefficients getQuadraticMeanDiameterByUtilization() {
		return quadraticMeanDiameterByUtilization;
	}

	@Override
	public void setQuadraticMeanDiameterByUtilization(Coefficients quadraticMeanDiameterByUtilization) {
		this.quadraticMeanDiameterByUtilization = quadraticMeanDiameterByUtilization;
	}

	@Override
	public Coefficients getTreesPerHectareByUtilization() {
		return treesPerHectareByUtilization;
	}

	@Override
	public void setTreesPerHectareByUtilization(Coefficients treesPerHectareByUtilization) {
		this.treesPerHectareByUtilization = treesPerHectareByUtilization;
	}

	@Override
	public Coefficients getWholeStemVolumeByUtilization() {
		return wholeStemVolumeByUtilization;
	}

	@Override
	public void setWholeStemVolumeByUtilization(Coefficients wholeStemVolumeByUtilization) {
		this.wholeStemVolumeByUtilization = wholeStemVolumeByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeByUtilization() {
		return closeUtilizationVolumeByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeByUtilization(Coefficients closeUtilizationVolumeByUtilization) {
		this.closeUtilizationVolumeByUtilization = closeUtilizationVolumeByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeNetOfDecayByUtilization() {
		return closeUtilizationVolumeNetOfDecayByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayByUtilization(
			Coefficients closeUtilizationNetVolumeOfDecayByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayByUtilization = closeUtilizationNetVolumeOfDecayByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization() {
		return closeUtilizationVolumeNetOfDecayAndWasteByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayAndWasteByUtilization(
			Coefficients closeUtilizationVolumeNetOfDecayAndWasteByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayAndWasteByUtilization = closeUtilizationVolumeNetOfDecayAndWasteByUtilization;
	}

	@Override
	public Coefficients getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization() {
		return closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization(
			Coefficients closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
	) {
		this.closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization;
	}

	public Optional<String> getDominantSpecies() {
		return dominantSpecies;
	}

	public void setDominantSpecies(Optional<String> dominantSpecies) {
		this.dominantSpecies = dominantSpecies;
	}

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * <pre>
	 * VdypLayer myLayer = VdypLayer.build(builder-&gt; {
			builder.polygonIdentifier(polygonId);
			builder.layerType(LayerType.VETERAN);
			builder.ageTotal(8f);
			builder.yearsToBreastHeight(7f);
			builder.height(6f);
	
			builder.siteIndex(5f);
			builder.crownClosure(0.9f);
			builder.siteGenus("B");
			builder.siteSpecies("B");
	 * })
	 * </pre>
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by
	 *                               the configuration function.
	 */
	public static VdypLayer build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	/**
	 * Builds a layer and adds it to the polygon.
	 *
	 * @param layer  Layer to create the species for.
	 * @param config Configuration function for the builder.
	 * @return the new species.
	 */
	public static VdypLayer build(VdypPolygon polygon, Consumer<Builder> config) {
		var result = build(builder -> {
			builder.polygonIdentifier(polygon.getPolygonIdentifier());

			config.accept(builder);
		});
		polygon.getLayers().put(result.getLayer(), result);
		return result;
	}

	public static class Builder extends BaseVdypLayer.Builder<VdypLayer, VdypSpecies> {

		@Override
		protected VdypLayer doBuild() {
			return (new VdypLayer(
					polygonIdentifier.get(), //
					layer.get(), //
					ageTotal, //
					height, //
					yearsToBreastHeight, //
					siteIndex, //
					siteCurveNumber, //
					inventoryTypeGroup, //
					siteGenus
			));
		}

	}
}
