package ca.bc.gov.nrs.vdyp.model;

import java.util.Objects;

public class GenusDefinition extends AliasedEntity implements Comparable<GenusDefinition> {

	private final int index;

	public GenusDefinition(String alias, int index, String name) {
		super(alias, name);

		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	@Override
	public int hashCode() {
		return ( (this.getAlias().hashCode() * 17) + this.getName().hashCode()) * 17 + index;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof GenusDefinition that) {
			return Objects.equals(this.index, that.index) && super.equals(that);
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(GenusDefinition that) {
		if (that != null) {
			int aliasCompareResult = this.getAlias().compareTo(that.getAlias());
			if (aliasCompareResult == 0) {
				int nameCompareResult = this.getName().compareTo(that.getName());
				if (nameCompareResult == 0) {
					return this.index - that.index;
				} else {
					return nameCompareResult;
				}
			} else {
				return aliasCompareResult;
			}
		} else {
			return 1 /* null is always less than not null */;
		}
	}
}
