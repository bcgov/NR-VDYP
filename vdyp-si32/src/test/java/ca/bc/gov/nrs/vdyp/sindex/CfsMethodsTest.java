package ca.bc.gov.nrs.vdyp.sindex;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsDead;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForGenus;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionCoefficientsForSpecies;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsDensity;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsMethods;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsSP0Densities;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsSpeciesMethods;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeClass;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeGenus;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeSpecies;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP0Name;

class CfsMethodsTest {

	@Test
	void test_CFS_CFSTreeClassToString() {
		assertThat(
				CfsMethods.cfsTreeClassToString(CfsTreeClass.LIVE_NO_PATH.getIndex()), equalTo(
						CfsTreeClass.LIVE_NO_PATH.getDescription()
				)
		);
		assertThat(CfsMethods.cfsTreeClassToString(100), equalTo(CfsTreeClass.UNKNOWN.getDescription()));
	}

	@Test
	void test_CFS_CFSGenusToString() {
		assertThat(
				CfsMethods.cfsGenusToString(CfsTreeGenus.BIRCH), equalTo(
						CfsTreeGenus.BIRCH.getGenusName()
				)
		);
		assertThat(CfsMethods.cfsGenusToString(null), equalTo(CfsTreeGenus.UNKNOWN.getGenusName()));
	}
	
	@Test
	void test_CFS_CFSSP0DensityFunctions() {
		assertThat(CfsMethods.cfsSP0DensityMax(null), equalTo(CfsSP0Densities.DEFAULT_VALUE));
		assertThat(CfsMethods.cfsSP0DensityMax(SP0Name.AC), equalTo(564.00F));
		assertThat(CfsMethods.cfsSP0DensityMean(SP0Name.AC), equalTo(295.00F));
		assertThat(CfsMethods.cfsSP0DensityMin(SP0Name.AC), equalTo(229.00F));
	}
	
	@Test
	void test_CFS_StringToCfsSpeciesTest() {
		assertThat(CfsMethods.stringToCfsSpecies("Black Spruce"), equalTo(CfsTreeSpecies.SpruceBlack));
		assertThat(CfsMethods.stringToCfsSpecies("Black spruce"), equalTo(CfsTreeSpecies.SpruceBlack));
		assertThat(CfsMethods.stringToCfsSpecies("something"), equalTo(CfsTreeSpecies.UNKNOWN));
		assertThat(CfsMethods.stringToCfsSpecies(null), equalTo(CfsTreeSpecies.UNKNOWN));
	}
	
	@Test
	void test_CFS_CFSSpcsNumToCFSGenus() {
		assertThat(CfsMethods.cfsSpcsNumToCFSGenus(CfsTreeSpecies.SpruceBlack), equalTo(CfsTreeGenus.SPRUCE));
		assertThat(CfsMethods.cfsSpcsNumToCFSGenus(null), equalTo(CfsTreeGenus.UNKNOWN));
	}
	
	@Test
	void test_CFS_CFSBiomassConversionCoefficientArrays() {
		assertThat(CfsBiomassConversionCoefficientsDead.get(1, 1).parms()[1], equalTo(0.29900000f));
		assertThat(CfsBiomassConversionCoefficientsForSpecies.get(1, 1).parms()[1], equalTo(0.84904177f));
		assertThat(CfsBiomassConversionCoefficientsForGenus.get(1, 1).parms()[1], equalTo(0.95456459f));
	}
	
	@Test
	public static void testGetSpeciesBySpeciesName() {
		String name = CfsTreeSpecies.AlderRed.getCfsSpeciesName();
		CfsTreeSpecies ts;
		
		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name);
		assertThat(ts, equalTo("Red alder"));
		
		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name.toLowerCase());
		assertThat(ts, equalTo("Red alder"));
		
		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(name.toUpperCase());
		assertThat(ts, equalTo("Red alder"));
		
		ts = CfsSpeciesMethods.getSpeciesBySpeciesName(null);
		assertThat(ts, equalTo("Unknown Species"));
	}

	@Test
	public static void testGetGenusBySpecies() {
		CfsTreeGenus g = CfsSpeciesMethods.getGenusBySpecies(CfsTreeSpecies.AlderRed);
		assertThat(g, equalTo(CfsTreeGenus.OTHER_BROAD_LEAVES));
		assertThat(CfsSpeciesMethods.getGenusBySpecies(null), equalTo(CfsTreeGenus.UNKNOWN));
	}
	
	@Test
	public static void testGetSpeciesIndexBySpecies() {
		int r = CfsSpeciesMethods.getSpeciesIndexBySpecies(CfsTreeSpecies.AlderRed);
		assertThat(r, equalTo(1802));
		assertThat(CfsSpeciesMethods.getSpeciesIndexBySpecies(null), equalTo(-1));
	}
	
	@Test
	public static void testCfsSP0Densities() {
		assertThat(CfsSP0Densities.getValue(SP0Name.B, CfsDensity.MEAN_DENSITY_INDEX), equalTo(379.25F));
		assertThat(CfsSP0Densities.getValue(null, CfsDensity.MEAN_DENSITY_INDEX), equalTo(CfsSP0Densities.DEFAULT_VALUE));
		assertThat(CfsSP0Densities.getValue(SP0Name.B, null), equalTo(CfsSP0Densities.DEFAULT_VALUE));
	}
}