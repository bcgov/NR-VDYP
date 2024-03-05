package ca.bc.gov.nrs.vdyp.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.math3.util.Pair;

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
	 * Normalize a nullable value that may or may not be an Optional to a non-null
	 * Optional.
	 *
	 * Mostly useful for Optionalizing values from maps.
	 *
	 * @param <U>
	 * @param value
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <U> Optional<U> optSafe(@Nullable Object value) {
		return Optional.ofNullable(value).flatMap(x -> x instanceof Optional o ? o : (Optional) Optional.of(x));
	}

	public static <T, U> void ifBothPresent(Optional<T> opt1, Optional<U> opt2, BiConsumer<T, U> consumer) {
		opt1.ifPresent(v1 -> {
			opt2.ifPresent(v2 -> consumer.accept(v1, v2));
		});
	}

	@SuppressWarnings("unchecked")
	public static <U> Optional<U> parsedControl(Map<String, Object> control, String key, Class<? super U> clazz) {
		var opt = optSafe(control.get(key));

		opt.ifPresent(value -> {
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
		});

		return (Optional<U>) opt.map(clazz::cast);
	}

	@SuppressWarnings("unchecked")
	public static <U> Optional<U> parsedControl(Map<String, Object> control, ControlKey key, Class<? super U> clazz) {
		return (Optional<U>) parsedControl(control, key.name(), clazz);
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
		return (U) parsedControl(control, key, clazz)
				.orElseThrow(() -> new IllegalStateException("Expected control map to have " + key));
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
	public static <U> U expectParsedControl(Map<String, Object> control, ControlKey key, Class<? super U> clazz) {
		return (U) expectParsedControl(control, key.name(), clazz);
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

	/**
	 * Takes two iterables and returns an iterable of pairs of their entries. If
	 * they have different lengths, it stops when the first one does.
	 *
	 * @param <T>
	 * @param <U>
	 * @param iterable1
	 * @param iterable2
	 * @return
	 */
	public static <T, U> Iterable<Pair<T, U>> parallelIterate(Iterable<T> iterable1, Iterable<U> iterable2) {
		return () -> {
			var iterator1 = iterable1.iterator();
			var iterator2 = iterable2.iterator();

			return new Iterator<Pair<T, U>>() {

				@Override
				public boolean hasNext() {
					return iterator1.hasNext() && iterator2.hasNext();
				}

				@Override
				public Pair<T, U> next() {
					return new Pair<>(iterator1.next(), iterator2.next());
				}

			};
		};
	}

    /**
     * @return true iff <code>string</code> is null or when the result of 
     * passing the first {@code Math.min(string.length(), length)} characters of 
     * {@code string} to {@code java.lang.String.isBlank()} would return true.
     * 
     * @param string the String against which the check is being made
     * @param length the length of the prefix of {@code string} that's being checked.
     */
	public static boolean nullOrPrefixBlank(@Nullable String string, int length) {
		return string == null || (string.length() <= length && string.isBlank())
				|| (string.length() > length && string.substring(0, length).isBlank());
	}

	/**
	 * @return true iff <code>string</code> is null or {@code java.lang.String.isBlank()} would
	 * return true.
     * 
     * @param string the String against which the check is being made
	 */
	public static boolean nullOrBlank(@Nullable String string) {
		return string == null || string.isBlank();
	}

    /**
     * @return true iff <code>string</code> is null or {@code java.lang.String.isEmpty()} would
     * return true.
     * 
     * @param string the String against which the check is being made
     */
	public static boolean nullOrEmpty(@Nullable String string) {
		return string == null || string.isEmpty();
	}
}
