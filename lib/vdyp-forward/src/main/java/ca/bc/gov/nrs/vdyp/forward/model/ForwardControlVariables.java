package ca.bc.gov.nrs.vdyp.forward.model;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import ca.bc.gov.nrs.vdyp.io.parse.value.ValueParseException;

/**
 * Control Variable values
 * <p>
 * 1st: Grow target
 * <ul>
 * <li>0: NO GROWTH
 * <li>-1: SEQ014 (specifies end yr, each poly)
 * <li>1 to 400: grow i yrs
 * <li>1920 to 2400: grow to yr i (A.D.)
 * </ul>
 * 2nd: Compatibility Variable output
 * <ul>
 * <li>0 None
 * <li>1 First yr only
 * <li>2 All yrs
 * <li>Note: Output can occur ONLY in yrs also selected by 3rd option
 * </ul>
 * 3rd: Compatibility Variable application
 * <ul>
 * <li>0: Do not apply
 * <li>1: All variables except Volume
 * <li>2: All variables (standard usage)
 * </ul>
 * 4th: Output Files
 * <ul>
 * <li>0: None
 * <li>1: First year
 * <li>2: First and last year
 * <li>3: All years
 * <li>4: First, 1st + 10, 1st + 20, ..., last
 * </ul>
 * 5th: Allow Compatibility Variable computations
 * <ul>
 * <li>0: Always
 * <li>1: Only when basis exceeds limits (.1 for vol, .01 BA for DQ)
 * </ul>
 * 6th: Update Site species and ITG during growth
 * <ul>
 * <li>0: No
 * <li>1: Yes (normal)
 * </ul>
 * 7th: Checkpoint growth. Polygons are written to the output files after each step of the grow process. The final write
 * for a given year is the result of growth for that year; the others are all intermediate.
 * <ul>
 * <li>0: No
 * <li>1: Yes (normal)
 * </ul>
 */
public class ForwardControlVariables {

	private static final int MAX_CONTROL_VARIABLE_VALUES = 10;
	private static final int DEFAULT_CONTROL_VARIABLE_VALUE = 0;

	private final int[] controlVariables = new int[10];

	public ForwardControlVariables(Integer[] controlVariableValues) throws ValueParseException {
		int index = 0;

		if (controlVariableValues != null) {
			for (; index < Math.min(controlVariableValues.length, MAX_CONTROL_VARIABLE_VALUES); index++)
				this.controlVariables[index] = controlVariableValues[index];
		}

		for (; index < MAX_CONTROL_VARIABLE_VALUES; index++)
			this.controlVariables[index] = DEFAULT_CONTROL_VARIABLE_VALUE;

		validate();
	}

	private void validate() throws ValueParseException {

		// Validate the control variable values.

		var yearCounter = getControlVariable(ControlVariable.GROW_TARGET_1);
		if (yearCounter != -1 && (yearCounter < 0 || yearCounter > 400 && yearCounter < 1920 || yearCounter > 2400)) {
			throw new ValueParseException(
					Integer.toString(yearCounter),
					"VdypControlVariableParser: year counter (1) value \"" + yearCounter + "\" is out of range"
			);
		}

		var compatibilityVariableOutputVariableValue = getControlVariable(ControlVariable.COMPAT_VAR_OUTPUT_2);
		if (compatibilityVariableOutputVariableValue < 0 || compatibilityVariableOutputVariableValue > 2) {
			throw new ValueParseException(
					Integer.toString(compatibilityVariableOutputVariableValue),
					"VdypControlVariableParser: compatibility variable output value \""
							+ compatibilityVariableOutputVariableValue + "\" is out of range [0-2]"
			);
		}

		var compatibilityVariableApplicationVariableValue = getControlVariable(
				ControlVariable.COMPAT_VAR_APPLICATION_3
		);
		if (compatibilityVariableApplicationVariableValue < 0 || compatibilityVariableApplicationVariableValue > 2) {
			throw new ValueParseException(
					Integer.toString(compatibilityVariableApplicationVariableValue),
					"VdypControlVariableParser: compatibility variable application value \""
							+ compatibilityVariableApplicationVariableValue + "\" is out of range [0-2]"
			);
		}

		var outputFileDirectiveVariableValue = getControlVariable(ControlVariable.OUTPUT_FILES_4);
		if (outputFileDirectiveVariableValue < 0 || outputFileDirectiveVariableValue > 4) {
			throw new ValueParseException(
					Integer.toString(outputFileDirectiveVariableValue),
					"VdypControlVariableParser: output file directive value \"" + outputFileDirectiveVariableValue
							+ "\" is out of range [0-4]"
			);
		}

		var allowCompatibilityVariableCalculationsVariableValue = getControlVariable(
				ControlVariable.ALLOW_COMPAT_VAR_CALCS_5
		);
		if (allowCompatibilityVariableCalculationsVariableValue < 0
				|| allowCompatibilityVariableCalculationsVariableValue > 1) {
			throw new ValueParseException(
					Integer.toString(allowCompatibilityVariableCalculationsVariableValue),
					"VdypControlVariableParser: compatibility variable calculations allowed value \""
							+ allowCompatibilityVariableCalculationsVariableValue + "\" is out of range [0-1]"
			);
		}

		var updateDuringGrowthVariableValue = getControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6);
		if (updateDuringGrowthVariableValue < 0 || updateDuringGrowthVariableValue > 1) {
			throw new ValueParseException(
					Integer.toString(updateDuringGrowthVariableValue),
					"VdypControlVariableParser: update site species and ITG during grow value \""
							+ updateDuringGrowthVariableValue + "\" is out of range [0-1]"
			);
		}
	}

	public boolean allowCalculation(float value, float limit, BiFunction<Float, Float, Boolean> p) {
		int cvValue = controlVariables[ControlVariable.ALLOW_COMPAT_VAR_CALCS_5.ordinal()];
		return cvValue == 0 && value > 0 || cvValue > 0 && p.apply(value, limit);
	}

	public boolean allowCalculation(BooleanSupplier p) {
		int cvValue = controlVariables[ControlVariable.ALLOW_COMPAT_VAR_CALCS_5.ordinal()];
		return cvValue == 1 && p.getAsBoolean();
	}

	public int getControlVariable(ControlVariable controlVariable) {

		assert controlVariable.ordinal() == controlVariable.variableNumber - 1;
		return controlVariables[controlVariable.ordinal()];
	}

	/**
	 * Explicitly set a control variable value. To be used by unit tests only.
	 *
	 * @param controlVariable the variable to set
	 * @param value           its new value
	 * @throws ValueParseException
	 */
	public void setControlVariable(ControlVariable controlVariable, int value) throws ValueParseException {
		controlVariables[controlVariable.ordinal()] = value;
		validate();
	}

	int getControlVariable(int elementNumber) {

		if (elementNumber < 1 || elementNumber > MAX_CONTROL_VARIABLE_VALUES) {
			throw new IllegalArgumentException(
					"Element number (" + elementNumber + ") is out of range - must be from 1 to "
							+ MAX_CONTROL_VARIABLE_VALUES
			);
		}

		return controlVariables[elementNumber - 1];
	}
}
