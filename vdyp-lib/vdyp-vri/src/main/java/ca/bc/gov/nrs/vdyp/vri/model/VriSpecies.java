package ca.bc.gov.nrs.vdyp.vri.model;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ca.bc.gov.nrs.vdyp.model.BaseVdypSpecies;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.PolygonIdentifier;
import ca.bc.gov.nrs.vdyp.model.Sp64DistributionSet;

public class VriSpecies extends BaseVdypSpecies<VriSite> {

	public VriSpecies(
			PolygonIdentifier polygonIdentifier, LayerType layer, String genus, int genusIndex, float percentGenus,
			Sp64DistributionSet sp64DistributionSet, Optional<VriSite> site
	) {
		super(polygonIdentifier, layer, genus, genusIndex, percentGenus, sp64DistributionSet, site);
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
	public static VriSpecies build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static class Builder extends BaseVdypSpecies.Builder<VriSpecies, VriSite, VriSite.Builder> {

		@Override
		protected VriSpecies doBuild() {
			return new VriSpecies(
					this.polygonIdentifier.get(), //
					this.layerType.get(), //
					this.genus.get(), //
					this.genusIndex.get(), //
					this.percentGenus.get(), //
					new Sp64DistributionSet(this.sp64DistributionList), //
					this.site
			);
		}

		@Override
		protected VriSite buildSite(Consumer<VriSite.Builder> config) {
			return VriSite.build(builder -> {
				config.accept(builder);
				builder.polygonIdentifier(this.polygonIdentifier.get());
				builder.layerType(this.layerType.get());
				builder.siteGenus(this.genus);
			});
		}
	}

	@Override
	public String toString() {
		return String.format(
				"VRISpecies[\"%s\", %s, %s, %s%%, {%s}]", //
				this.getPolygonIdentifier(), //
				this.getLayerType(), //
				this.getGenus(), //
				this.getPercentGenus(), //
				this.getSp64DistributionSet().getSp64DistributionList().stream()
						.map(e -> String.format("%s: %s%%", e.getGenusAlias(), e.getPercentage()))
						.collect(Collectors.joining(", "))
		);
	}

}
