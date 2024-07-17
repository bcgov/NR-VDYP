package ca.bc.gov.nrs.vdyp.model;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.common.Utils;

public class VdypSpecies extends BaseVdypSpecies implements VdypUtilizationHolder {

	private Coefficients baseAreaByUtilization = Utils.utilizationVector(); // LVCOM/BA
	private Coefficients loreyHeightByUtilization = Utils.heightVector(); // LVCOM/HL
	private Coefficients quadraticMeanDiameterByUtilization = Utils.utilizationVector(); // LVCOM/DQ
	private Coefficients treesPerHectareByUtilization = Utils.utilizationVector(); // LVCOM/TPH

	private Coefficients wholeStemVolumeByUtilization = Utils.utilizationVector(); // LVCOM/VOLWS
	private Coefficients closeUtilizationVolumeByUtilization = Utils.utilizationVector(); // LVCOM/VOLCU
	private Coefficients closeUtilizationNetVolumeOfDecayByUtilization = Utils.utilizationVector(); // LVCOM/VOL_D
	private Coefficients closeUtilizationVolumeNetOfDecayAndWasteByUtilization = Utils.utilizationVector(); // LVCOM/VOL_DW
	private Coefficients closeUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization = Utils.utilizationVector(); // LVCOM/VOL_DWB

	int volumeGroup;
	int decayGroup;
	int breakageGroup;

	public VdypSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layer, String genus, float percentGenus, //
			int volumeGroup, int decayGroup, int breakageGroup
	) {
		super(polygonIdentifier, layer, genus, percentGenus);
		this.volumeGroup = volumeGroup;
		this.decayGroup = decayGroup;
		this.breakageGroup = breakageGroup;

	}

	public VdypSpecies(BaseVdypSpecies toCopy) {
		super(toCopy);
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	@Override
	public Coefficients getBaseAreaByUtilization() {
		return baseAreaByUtilization;
	}

	/**
	 * Base area for utilization index -1 through 4
	 */
	@Override
	public void setBaseAreaByUtilization(Coefficients baseAreaByUtilization) {
		this.baseAreaByUtilization = baseAreaByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	@Override
	public Coefficients getLoreyHeightByUtilization() {
		return loreyHeightByUtilization;
	}

	/**
	 * Lorey height for utilization index -1 through 0
	 */
	@Override
	public void setLoreyHeightByUtilization(Coefficients loreyHeightByUtilization) {
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
		return closeUtilizationNetVolumeOfDecayByUtilization;
	}

	@Override
	public void setCloseUtilizationVolumeNetOfDecayByUtilization(
			Coefficients closeUtilizationNetVolumeOfDecayByUtilization
	) {
		this.closeUtilizationNetVolumeOfDecayByUtilization = closeUtilizationNetVolumeOfDecayByUtilization;
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

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * <pre>
	 * FipSpecies myLayer = FipSpecies.build(builder-&gt; {
			builder.polygonIdentifier(polygonId);
			builder.layerType(LayerType.VETERAN);
			builder.genus("B");
			builder.percentGenus(6f);
	 * })
	 * </pre>
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by the configuration function.
	 */
	public static VdypSpecies build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	/**
	 * Builds a species and adds it to the layer.
	 *
	 * @param layer  Layer to create the species for.
	 * @param config Configuration function for the builder.
	 * @return the new species.
	 */
	public static VdypSpecies build(VdypLayer layer, Consumer<Builder> config) {
		var result = build(builder -> {
			builder.polygonIdentifier(layer.getPolygonIdentifier());
			builder.layerType(layer.getLayerType());

			config.accept(builder);
		});
		layer.getSpecies().put(result.getGenus(), result);
		return result;
	}

	public static class Builder extends BaseVdypSpecies.Builder<VdypSpecies> {
		protected Optional<Integer> volumeGroup = Optional.empty();
		protected Optional<Integer> decayGroup = Optional.empty();
		protected Optional<Integer> breakageGroup = Optional.empty();

		protected Coefficients loreyHeight = VdypUtilizationHolder.emptyLoreyHeightUtilization();

		public void loreyHeight(float height) {
			this.loreyHeight = Utils.heightVector(0, height);
		}

		public void loreyHeight(float small, float height) {
			this.loreyHeight = Utils.heightVector(small, height);
		}

		protected Coefficients baseArea = VdypUtilizationHolder.emptyUtilization();

		public void baseArea(float small, float u1, float u2, float u3, float u4) {
			this.baseArea = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void baseArea(float height) {
			this.baseArea = Utils.utilizationVector(height);
		}

		protected Coefficients treesPerHectare = VdypUtilizationHolder.emptyUtilization();

		public void treesPerHectare(float small, float u1, float u2, float u3, float u4) {
			this.treesPerHectare = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void treesPerHectare(float height) {
			this.treesPerHectare = Utils.utilizationVector(height);
		}

		protected Coefficients quadMeanDiameter = VdypUtilizationHolder.emptyUtilization();

		public void quadMeanDiameter(float small, float u1, float u2, float u3, float u4) {
			this.quadMeanDiameter = Utils.utilizationVector(small, u1, u2, u3, u4);
		}

		public void quadMeanDiameter(float height) {
			this.quadMeanDiameter = Utils.utilizationVector(height);
		}

		@Override
		protected void check(Collection<String> errors) {
			super.check(errors);
			requirePresent(volumeGroup, "volumeGroup", errors);
			requirePresent(decayGroup, "decayGroup", errors);
			requirePresent(breakageGroup, "breakageGroup", errors);
		}

		@Override
		public Builder copy(VdypSpecies toCopy) {
			super.copy(toCopy);
			volumeGroup(toCopy.getVolumeGroup());
			decayGroup(toCopy.getDecayGroup());
			breakageGroup(toCopy.getBreakageGroup());
			return this;
		}

		@Override
		protected void postProcess(VdypSpecies spec) {
			super.postProcess(spec);
			spec.setLoreyHeightByUtilization(loreyHeight);
			spec.setBaseAreaByUtilization(baseArea);
			spec.setTreesPerHectareByUtilization(treesPerHectare);
			spec.setQuadraticMeanDiameterByUtilization(quadMeanDiameter);
		}

		@Override
		protected VdypSpecies doBuild() {
			return new VdypSpecies(
					polygonIdentifier.get(), //
					layerType.get(), //
					genus.get(), //
					percentGenus.get(), //
					volumeGroup.get(), //
					decayGroup.get(), //
					breakageGroup.get() //
			);
		}

		public Builder volumeGroup(int i) {
			this.volumeGroup = Optional.of(i);
			return this;
		}

		public Builder decayGroup(int i) {
			this.decayGroup = Optional.of(i);
			return this;
		}

		public Builder breakageGroup(int i) {
			this.breakageGroup = Optional.of(i);
			return this;
		}
	}
}
