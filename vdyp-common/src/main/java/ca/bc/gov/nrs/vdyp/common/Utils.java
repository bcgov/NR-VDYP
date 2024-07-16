package ca.bc.gov.nrs.vdyp.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.math3.util.Pair;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

public class Utils {

	private Utils() {
	}

	/**
	 * Returns a singleton set containing the value if it's not null, otherwise an empty set
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
	 * Normalize a nullable value that may or may not be an Optional to a non-null Optional.
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

	/**
	 * Passes both operands to the consumer only if both are present.
	 *
	 * @return
	 */
	public static <T, U> void ifBothPresent(Optional<T> opt1, Optional<U> opt2, BiConsumer<T, U> consumer) {
		opt1.ifPresent(v1 -> {
			opt2.ifPresent(v2 -> consumer.accept(v1, v2));
		});
	}

	/**
	 * Returns the result of the function applied to the operands if both are present, otherwise empty.
	 *
	 * @return
	 */
	public static <T, U, V> Optional<V> mapBoth(Optional<T> opt1, Optional<U> opt2, BiFunction<T, U, V> function) {
		return opt1.flatMap(v1 -> opt2.map(v2 -> function.apply(v1, v2)));
	}

	/**
	 * Returns the result of the function applied to the operands if both are present, otherwise empty.
	 *
	 * @return
	 */
	public static <T, U, V> Optional<V>
			flatMapBoth(Optional<T> opt1, Optional<U> opt2, BiFunction<T, U, Optional<V>> function) {
		return opt1.flatMap(v1 -> opt2.flatMap(v2 -> function.apply(v1, v2)));
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
	 * @throws IllegalStateException if the control map does not have the requested entry or it is the wrong type.
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
	 * @throws IllegalStateException if the control map does not have the requested entry or it is the wrong type.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <U> U expectParsedControl(Map<String, Object> control, ControlKey key, Class<? super U> clazz) {
		return (U) expectParsedControl(control, key.name(), clazz);
	}

	/**
	 * Creates a Comparator that compares two objects by applying the given accessor function to get comparable values
	 * that are then compared.
	 *
	 * @param <T>      type to be compared with the Comparator
	 * @param <V>      Comparable type
	 * @param accessor Function getting a V from a T
	 */
	public static <T, V extends Comparable<V>> Comparator<T> compareUsing(Function<T, V> accessor) {
		return (x, y) -> accessor.apply(x).compareTo(accessor.apply(y));
	}

	/**
	 * Compares two Optionals of comparables of the same type, treating "empty" as equal to "empty" and before
	 * "present".
	 *
	 * @param <T> the base type of the Optional
	 * @param t1  the lhs of the comparison
	 * @param t2  the rhs of the comparison
	 * @return as described.
	 */
	public static <T extends Comparable<T>> int compareOptionals(Optional<T> t1, Optional<T> t2) {
		if (t1.isEmpty() && t2.isEmpty()) {
			return 0;
		} else if (t1.isEmpty()) {
			return -1;
		} else if (t2.isEmpty()) {
			return 1;
		} else {
			return t1.get().compareTo(t2.get());
		}
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

	public static Coefficients heightVector() {
		return heightVector(0, 0);
	}

	public static Coefficients utilizationVector(float small, float all, float u1, float u2, float u3, float u4) {
		return new Coefficients(new float[] { small, all, u1, u2, u3, u4 }, -1);
	}

	public static Coefficients utilizationVector(float small, float u1, float u2, float u3, float u4) {
		return utilizationVector(small, u1 + u2 + u3 + u4, u1, u2, u3, u4);
	}

	public static Coefficients utilizationVector(float singleValue) {
		return utilizationVector(0f, singleValue, 0f, 0f, 0f, singleValue);
	}

	public static Coefficients utilizationVector() {
		return utilizationVector(0f);
	}

	/**
	 * Takes two iterables and returns an iterable of pairs of their entries. If they have different lengths, it stops
	 * when the first one does.
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
	 * @return true iff <code>string</code> is null or when the result of passing the first
	 *         {@code Math.min(string.length(), length)} characters of {@code string} to
	 *         {@code java.lang.String.isBlank()} would return true.
	 *
	 * @param string the String against which the check is being made
	 * @param length the length of the prefix of {@code string} that's being checked.
	 */
	public static boolean nullOrPrefixBlank(@Nullable String string, int length) {
		return string == null || (string.length() <= length && string.isBlank())
				|| (string.length() > length && string.substring(0, length).isBlank());
	}

	/**
	 * @return true iff <code>string</code> is null or {@code java.lang.String.isBlank()} would return true.
	 *
	 * @param string the String against which the check is being made
	 */
	public static boolean nullOrBlank(@Nullable String string) {
		return string == null || string.isBlank();
	}

	/**
	 * @return true iff <code>string</code> is null or {@code java.lang.String.isEmpty()} would return true.
	 *
	 * @param string the String against which the check is being made
	 */
	public static boolean nullOrEmpty(@Nullable String string) {
		return string == null || string.isEmpty();
	}

	public static boolean parsesBlankOrNonPositive(String string) {
		if (string == null || string.isBlank())
			return true;
		var value = Float.valueOf(string);
		return value != null && value <= 0;
	}

	public static <T> Optional<T> getIfPresent(List<T> list, int index) {
		if (list.size() > index)
			return Optional.of(list.get(index));
		return Optional.empty();
	}

	public static BecDefinition getBec(String biogeoclimaticZone, Map<String, Object> controlMap)
			throws ProcessingException {
		return expectParsedControl(controlMap, ControlKey.BEC_DEF, BecLookup.class).get(biogeoclimaticZone)
				.orElseThrow(() -> new ProcessingException("Reference to unexpected BEC " + biogeoclimaticZone));
	}

	/**
	 * Returns the value of the optional if it's present, otherwise the string "N/A"
	 *
	 * @param <T>
	 * @param value
	 * @param stringify
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object optNa(Optional<?> value) {
		return ((Optional) value).orElse("N/A");
	}

	/**
	 * If the value is present, returns the default string representation otherwise returns "N/A"
	 *
	 * @param <T>
	 * @param value
	 * @param stringify
	 * @return
	 */
	public static String optPretty(Optional<?> value) {
		return optPretty(value, Object::toString);
	}

	/**
	 * If the value is present, returns the result of the stringify function otherwise returns "N/A"
	 *
	 * @param <T>
	 * @param value
	 * @param stringify
	 * @return
	 */
	public static <T> String optPretty(Optional<T> value, Function<T, String> stringify) {
		return (String) optNa(value.map(stringify));
	}

	/**
	 * Iterates over all but the last entry, passing them to the first consumer then passes the last entry to the second
	 * consumer
	 */
	public static <T> void eachButLast(Collection<T> items, Consumer<T> body, Consumer<T> lastBody) {
		var it = items.iterator();
		while (it.hasNext()) {
			var value = it.next();
			if (it.hasNext()) {
				body.accept(value);
			} else {
				lastBody.accept(value);
			}
		}
	}

	// The following methods replicate the structure of the arrays used by VDYP7 to aid in debugging

	public static float[] utilizationArray(
			VdypLayer layer, Function<VdypUtilizationHolder, Coefficients> accessor, UtilizationClass uc
	) {
		var result = new float[layer.getSpecies().size() + 1];
		utilizationArray(result, layer, accessor, uc);
		return result;
	}

	private static void utilizationArray(
			float[] result, VdypLayer layer, Function<VdypUtilizationHolder, Coefficients> accessor, UtilizationClass uc
	) {
		int i = 0;
		result[i++] = accessor.apply(layer).getCoe(uc.index);
		for (var spec : layer.getSpecies().values()) {
			result[i++] = accessor.apply(spec).getCoe(uc.index);
		}
	}

	public static float[][] utilizationArray(VdypLayer layer, Function<VdypUtilizationHolder, Coefficients> accessor) {
		return utilizationArrayLimited(layer, accessor, UtilizationClass.OVER225);
	}

	private static float[][] utilizationArrayLimited(
			VdypLayer layer, Function<VdypUtilizationHolder, Coefficients> accessor, UtilizationClass maxUc
	) {
		var result = new float[maxUc.index + 2][layer.getSpecies().size() + 1];

		int i = 0;

		for (var uc : UtilizationClass.values()) {
			utilizationArray(result[i++], layer, accessor, uc);
			if (uc == maxUc)
				break;
		}
		return result;
	}

	public static float[][] hlArray(VdypLayer layer) {
		return utilizationArrayLimited(layer, VdypUtilizationHolder::getLoreyHeightByUtilization, UtilizationClass.ALL);
	}

	public static float[][] dqArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getQuadraticMeanDiameterByUtilization);
	}

	public static float[][] baArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getBaseAreaByUtilization);
	}

	public static float[][] tphArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getTreesPerHectareByUtilization);
	}

	public static float[][] wsvArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getWholeStemVolumeByUtilization);
	}

	public static float[][] cuvArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getCloseUtilizationVolumeByUtilization);
	}

	public static float[][] cuvdArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayByUtilization);
	}

	public static float[][] cuvdwArray(VdypLayer layer) {
		return utilizationArray(layer, VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization);
	}

	public static float[][] cuvdwbArray(VdypLayer layer) {
		return utilizationArray(
				layer, VdypUtilizationHolder::getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization
		);
	}

	public static float[] specValueArray(VdypLayer layer, Function<VdypSpecies, Object> accessor) {
		var result = new float[layer.getSpecies().size()];
		int i = 0;

		for (var spec : layer.getSpecies().values()) {
			result[i++] = Utils.optSafe(accessor.apply(spec)).map(Float.class::cast).orElse(-9f);
		}
		return result;
	}

	public static float[] specFraction(VdypLayer layer) {
		return specValueArray(layer, VdypSpecies::getFractionGenus);
	}

}
