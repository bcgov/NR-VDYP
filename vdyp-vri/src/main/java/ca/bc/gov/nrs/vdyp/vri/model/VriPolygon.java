package ca.bc.gov.nrs.vdyp.vri.model;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import ca.bc.gov.nrs.vdyp.model.BaseVdypPolygon;
import ca.bc.gov.nrs.vdyp.model.FipMode;

public class VriPolygon extends BaseVdypPolygon<VriLayer, Optional<Float>> {

	private Optional<String> nonproductiveDescription; // FIP_P3/NPDESC
	private float yieldFactor; // FIP_P4/YLDFACT

	public VriPolygon(
			String polygonIdentifier, String fiz, String becIdentifier, Optional<Float> percentAvailable,
			Optional<FipMode> modeFip, Optional<String> nonproductiveDescription, float yieldFactor
	) {
		super(polygonIdentifier, percentAvailable, fiz, becIdentifier, modeFip);
		this.nonproductiveDescription = nonproductiveDescription;
		this.yieldFactor = yieldFactor;
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

	/**
	 * Accepts a configuration function that accepts a builder to configure.
	 *
	 * <pre>
	 * VdypPolygon myPolygon = VdypPolygon.build(builder-&gt; {
			builder.polygonIdentifier(polygonId);
			builder.percentAvailable(percentAvailable);
			builder.forestInventoryZone
			builder.biogeoclimaticZone
			builder.
	 * })
	 * </pre>
	 *
	 * @param config The configuration function
	 * @return The object built by the configured builder.
	 * @throws IllegalStateException if any required properties have not been set by
	 *                               the configuration function.
	 */
	public static VriPolygon build(Consumer<Builder> config) {
		var builder = new Builder();
		config.accept(builder);
		return builder.build();
	}

	public static class Builder extends BaseVdypPolygon.Builder<VriPolygon, VriLayer, Optional<Float>> {
		protected Optional<String> nonproductiveDescription = Optional.empty();
		protected Optional<Float> yieldFactor = Optional.empty();

		public Builder() {
			this.percentAvailable(Optional.empty());
		}

		public Builder nonproductiveDescription(Optional<String> nonproductiveDescription) {
			this.nonproductiveDescription = nonproductiveDescription;
			return this;
		}

		public Builder nonproductiveDescription(String nonproductiveDescription) {
			nonproductiveDescription(Optional.of(nonproductiveDescription));
			return this;
		}

		public Builder yieldFactor(Float yieldFactor) {
			this.yieldFactor = Optional.of(yieldFactor);
			return this;
		}

		public Builder percentAvailable(Float percentAvailable) {
			percentAvailable(Optional.of(percentAvailable));
			return this;
		}

		@Override
		protected void check(Collection<String> errors) {
			super.check(errors);
			requirePresent(forestInventoryZone, "forestInventoryZone", errors);
			requirePresent(biogeoclimaticZone, "biogeoclimaticZone", errors);
			requirePresent(yieldFactor, "yieldFactor", errors);
		}

		@Override
		protected VriPolygon doBuild() {
			return (new VriPolygon(
					polygonIdentifier.get(), //
					forestInventoryZone.get(), //
					biogeoclimaticZone.get(), //
					percentAvailable.get(), //
					modeFip, //
					nonproductiveDescription, //
					yieldFactor.get() //
			));
		}

		@Override
		protected VriLayer.Builder getLayerBuilder() {
			return new VriLayer.Builder();
		}

	}
}
