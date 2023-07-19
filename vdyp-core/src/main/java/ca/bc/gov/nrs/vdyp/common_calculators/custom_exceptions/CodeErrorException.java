package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;
/**
 * The CodeErrorException exist to replace the SI_ERR_CODE(-8) error code found in the orginal C code
 * This exception is generally thrown when the species code is unknown, not valid or a similar issue
 */
public class CodeErrorException extends RuntimeException{
    public CodeErrorException(){}

    public CodeErrorException(String message){
        super(message);
    }

    public CodeErrorException(Throwable cause){
        super(cause);
    }

    public CodeErrorException(String message, Throwable cause){
        super(message, cause);
    }  
}
