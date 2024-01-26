package ca.bc.gov.nrs.vdyp.model;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.notPresent;
import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.present;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class FipModeTest {

	@ParameterizedTest()
	@EnumSource(FipMode.class) 
	void testGetByCodeExpected(FipMode mode){
		var result = FipMode.getByCode(mode.getCode());
		assertThat(result, present(is(mode)));
	}
	
	@ParameterizedTest()
	@ValueSource(ints= {-2, Integer.MIN_VALUE, Integer.MAX_VALUE, 42}) 
	void testGetByCodeUnexpected(int code){
		var result = FipMode.getByCode(code);
		assertThat(result, present(is(FipMode.DONT_PROCESS)));
	}
	
	@ParameterizedTest()
	@ValueSource(ints= {0}) 
	void testGetByCodeMissing(int code){
		var result = FipMode.getByCode(code);
		assertThat(result, notPresent());
	}

}
