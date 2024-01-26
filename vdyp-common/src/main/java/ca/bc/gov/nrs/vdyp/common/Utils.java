package ca.bc.gov.nrs.vdyp.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import ca.bc.gov.nrs.vdyp.model.Coefficients;

public class Utils {

	private Utils() {
	}

	/**
	 * Returns a singleton set containing the value if it's not null, otherwise an
	 * empty set
	 *
	 * @param <T>
	 * @param value
	 * @return
	 */
	public static <T> Set<T> singletonOrEmpty(@Nullable T value) {
		if (Objects.isNull(value)) {
			return Collections.emptySet();
		}
		return Collections.singleton(value);
	}

	/**
	 * Get an entry from a control map that is expected to exist.
	 *
	 * @param control The control map
	 * @param key     Key for the entry in the control map
	 * @param clazz   Expected type for the entry
	 * @throws IllegalStateException if the control map does not have the requested
	 *                               entry or it is the wrong type.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <U> U expectParsedControl(Map<String, Object> control, String key, Class<? super U> clazz) {
		var value = control.get(key);
		if (value == null) {
			throw new IllegalStateException("Expected control map to have " + key);
		}
		if (clazz != String.class && value instanceof String) {
			throw new IllegalStateException(
					"Expected control map entry " + key + " to be parsed but was still a String " + value
			);
		}
		if (!clazz.isInstance(value)) {
			throw new IllegalStateException(
					"Expected control map entry " + key + " to be a " + clazz.getSimpleName() + " but was a "
							+ value.getClass()
			);
		}
		return (U) value;
	}

	/**
	 * Creates a Comparator that compares two objects by applying the given accessor
	 * function to get comparable values that are then compared.
	 *
	 * @param <T>      type to be compared with the Comparator
	 * @param <V>      Comparable type
	 * @param accessor Function getting a V from a T
	 */
	public static <T, V extends Comparable<V>> Comparator<T> compareUsing(Function<T, V> accessor) {
		return (x, y) -> accessor.apply(x).compareTo(accessor.apply(y));
	}

	/**
	 * Create map, allow it to be modified, then return an unmodifiable view of it.
	 *
	 * @param <K>
	 * @param <V>
	 * @param body
	 * @return
	 */
	public static <K, V> Map<K, V> constMap(Consumer<Map<K, V>> body) {
		var map = new HashMap<K, V>();
		body.accept(map);
		return Collections.unmodifiableMap(map);
	}

	public static Coefficients heightVector(float small, float all) {
		return new Coefficients(new float[] { small, all }, -1);
	}

	public static Coefficients utilizationVector(float small, float all, float u1, float u2, float u3, float u4) {
		return new Coefficients(new float[] { small, all, u1, u2, u3, u4 }, -1);
	}

	public static Coefficients utilizationVector(float small, float u1, float u2, float u3, float u4) {
		return utilizationVector(small, u1 + u2 + u3 + u4, u1, u2, u3, u4);
	}

	public static Coefficients utilizationVector(float singleValue) {
		return utilizationVector(0f, singleValue, 0f, 0f, singleValue);
	}

	public static Coefficients utilizationVector() {
		return utilizationVector(0f);
	}

}
