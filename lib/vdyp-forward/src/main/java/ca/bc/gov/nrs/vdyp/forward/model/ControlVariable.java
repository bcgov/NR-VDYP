package ca.bc.gov.nrs.vdyp.forward.model;

public enum ControlVariable {
	/**
	 * 1st 4 col: Growth target. Set IYR_CNTR = i
	 * <ul>
	 * <li>i = 0 => no growth
	 * <li>i = -1 => use SEQ014 (specifies end year, each polygon)
	 * <li>i = 1 to 400 => grow given number of years
	 * <li>i = 1920 - 2400 => grow to given year (A.D.)
	 * </ul>
	 */
	GROW_TARGET_1(1),
	/**
	 * 2nd 4 col: Compatibility Variable output
	 * <ul>
	 * <li>0 => None
	 * <li>1 => First year only
	 * <li>2 => All years
	 * </ul>
	 * Note: Output will occur only in years also selected by 4th option
	 */
	COMPAT_VAR_OUTPUT_2(2),
	/**
	 * 3rd 4 col: Control Variable application
	 * <ul>
	 * <li>0 => Do not apply
	 * <li>1 => All variables except Vol
	 * <li>2 => All variables (standard behaviour)
	 * </ul>
	 */
	COMPAT_VAR_APPLICATION_3(3),
	/**
	 * 4th 4 col: when to generate output files
	 * <ul>
	 * <li>0 => none
	 * <li>1 => first year only
	 * <li>2 => first and last year only
	 * <li>3 => all years
	 * <li>4 => first, 1st + 10, 1st + 20, ..., last
	 * </ul>
	 */
	OUTPUT_FILES_4(4),
	/**
	 * 5th 4 col: allow Compatibility Variable computations
	 * <ul>
	 * <li>0 => always
	 * <li>1 => only when basis exceeds limits (.1 for vol, .01 BA for DQ)
	 * </ul>
	 */
	ALLOW_COMPAT_VAR_CALCS_5(5),
	/**
	 * 6th 4 col: Update site species and Inventory Type Group during growth
	 * <ul>
	 * <li>0 => no
	 * <li>1 => yes (standard behaviour)
	 * </ul>
	 */
	UPDATE_DURING_GROWTH_6(6),
	/**
	 * 7th 4 col: Checkpoint polygon growth after each growth step
	 * <ul>
	 * <li>0 => no (standard behaviour)
	 * <li>1 => yes
	 * </ul>
	 */
	CHECKPOINT_7(7);

	public final int variableNumber;

	ControlVariable(int variableNumber) {
		this.variableNumber = variableNumber;
	}
}