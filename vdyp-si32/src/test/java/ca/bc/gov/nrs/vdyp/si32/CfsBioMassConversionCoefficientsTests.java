package ca.bc.gov.nrs.vdyp.si32;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDead;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForGenus;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForSpecies;

class CfsBioMassConversionCoefficientsTests {

	@Test
	void test_dead() {
		CfsBiomassConversionCoefficientsDead.Details d1 = CfsBiomassConversionCoefficientsDead.get(1, 1);
		CfsBiomassConversionCoefficientsDead.Details d2 = CfsBiomassConversionCoefficientsDead.get(1, 2);
		
		assertTrue(d1.equals(d1));
		assertFalse(d1.equals(d2));
		assertThat(d1.toString(), Matchers.notNullValue());
		assertTrue(d1.hashCode() == d1.hashCode());
	}

	@Test
	void test_forGenus() {
		CfsBiomassConversionCoefficientsForGenus.Details d1 = CfsBiomassConversionCoefficientsForGenus.get(1, 1);
		CfsBiomassConversionCoefficientsForGenus.Details d2 = CfsBiomassConversionCoefficientsForGenus.get(1, 2);
		
		assertTrue(d1.equals(d1));
		assertFalse(d1.equals(d2));
		assertThat(d1.toString(), Matchers.notNullValue());
		assertTrue(d1.hashCode() == d1.hashCode());
	}

	@Test
	void test_forSpecies() {
		CfsBiomassConversionCoefficientsForSpecies.Details d1 = CfsBiomassConversionCoefficientsForSpecies.get(1, 1);
		CfsBiomassConversionCoefficientsForSpecies.Details d2 = CfsBiomassConversionCoefficientsForSpecies.get(1, 2);
		
		assertTrue(d1.equals(d1));
		assertFalse(d1.equals(d2));
		assertThat(d1.toString(), Matchers.notNullValue());
		assertTrue(d1.hashCode() == d1.hashCode());
	}
}