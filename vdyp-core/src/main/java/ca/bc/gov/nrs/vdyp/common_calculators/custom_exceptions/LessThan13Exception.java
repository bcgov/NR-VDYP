package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;

/**
 * The LessThan13Exception exist to replace the SI_ERR_LT13(-1) error code found
 * in the orginal C code This exception is generally thrown when there a site
 * index is less than or equal to 1.3
 */
public class LessThan13Exception extends RuntimeException {
	public LessThan13Exception() {
	}

	public LessThan13Exception(String message) {
		super(message);
	}

	public LessThan13Exception(Throwable cause) {
		super(cause);
	}

	public LessThan13Exception(String message, Throwable cause) {
		super(message, cause);
	}
}
