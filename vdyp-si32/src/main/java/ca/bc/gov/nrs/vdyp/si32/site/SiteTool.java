package ca.bc.gov.nrs.vdyp.si32.site;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedGenera;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsBiomassConversionSupportedSpecies;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsDeadConversionParams;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsLiveConversionParams;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsSpeciesMethods;
import ca.bc.gov.nrs.vdyp.si32.cfs.CfsTreeSpecies;
import ca.bc.gov.nrs.vdyp.si32.enumerations.SpeciesRegion;
import ca.bc.gov.nrs.vdyp.si32.vdyp.SP64Name;
import ca.bc.gov.nrs.vdyp.si32.vdyp.VdypMethods;
import ca.bc.gov.nrs.vdyp.sindex.Reference;
import ca.bc.gov.nrs.vdyp.sindex.Sindxdll;

public class SiteTool {
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(SiteTool.class);

	/**
	 * Converts a MoF sp64 species name (e.g, "AC" from SP64Name.sp64_AC) to its equivalent
	 * in {@link CfsBiomassConversionSupportedSpecies}, should one exist. If one doesn't,
	 * CFSBiomassConversionSupportedSpecies.spcsInt_UNKNOWN is returned.
	 * 
	 * @param spcsNm the text portion of a SP64Name
	 * @return as described
	 */
	public static CfsBiomassConversionSupportedSpecies lcl_MoFSP64ToCFSSpecies(String spcsNm) {

		SP64Name sp64Name = SP64Name.forText(spcsNm);
		
		switch (sp64Name) {
		   case sp64_AC: return CfsBiomassConversionSupportedSpecies.spcsInt_AC;
		   case sp64_ACB: return CfsBiomassConversionSupportedSpecies.spcsInt_ACB;
		   case sp64_AT: return CfsBiomassConversionSupportedSpecies.spcsInt_AT;
		   case sp64_B: return CfsBiomassConversionSupportedSpecies.spcsInt_B;
		   case sp64_BA: return CfsBiomassConversionSupportedSpecies.spcsInt_BA;
		   case sp64_BG: return CfsBiomassConversionSupportedSpecies.spcsInt_BG;
		   case sp64_BL: return CfsBiomassConversionSupportedSpecies.spcsInt_BL;
		   case sp64_CW: return CfsBiomassConversionSupportedSpecies.spcsInt_CW;
		   case sp64_DR: return CfsBiomassConversionSupportedSpecies.spcsInt_DR;
		   case sp64_EA: return CfsBiomassConversionSupportedSpecies.spcsInt_EA;
		   case sp64_EP: return CfsBiomassConversionSupportedSpecies.spcsInt_EP;
		   case sp64_EXP: return CfsBiomassConversionSupportedSpecies.spcsInt_EXP;
		   case sp64_FD: return CfsBiomassConversionSupportedSpecies.spcsInt_FD;
		   case sp64_FDC: return CfsBiomassConversionSupportedSpecies.spcsInt_FDC;
		   case sp64_FDI: return CfsBiomassConversionSupportedSpecies.spcsInt_FDI;
		   case sp64_H: return CfsBiomassConversionSupportedSpecies.spcsInt_H;
		   case sp64_HM: return CfsBiomassConversionSupportedSpecies.spcsInt_HM;
		   case sp64_HW: return CfsBiomassConversionSupportedSpecies.spcsInt_HW;
		   case sp64_L: return CfsBiomassConversionSupportedSpecies.spcsInt_L;
		   case sp64_LA: return CfsBiomassConversionSupportedSpecies.spcsInt_LA;
		   case sp64_LT: return CfsBiomassConversionSupportedSpecies.spcsInt_LT;
		   case sp64_LW: return CfsBiomassConversionSupportedSpecies.spcsInt_LW;
		   case sp64_MB: return CfsBiomassConversionSupportedSpecies.spcsInt_MB;
		   case sp64_PA: return CfsBiomassConversionSupportedSpecies.spcsInt_PA;
		   case sp64_PL:
		   case sp64_PLI: return CfsBiomassConversionSupportedSpecies.spcsInt_PL;
		   case sp64_PLC: return CfsBiomassConversionSupportedSpecies.spcsInt_PLC;
		   case sp64_PW: return CfsBiomassConversionSupportedSpecies.spcsInt_PW;
		   case sp64_PY: return CfsBiomassConversionSupportedSpecies.spcsInt_PY;
		   case sp64_S: return CfsBiomassConversionSupportedSpecies.spcsInt_S;
		   case sp64_SB: return CfsBiomassConversionSupportedSpecies.spcsInt_SB;
		   case sp64_SE: return CfsBiomassConversionSupportedSpecies.spcsInt_SE;
		   case sp64_SS: return CfsBiomassConversionSupportedSpecies.spcsInt_SS;
		   case sp64_SW: return CfsBiomassConversionSupportedSpecies.spcsInt_SW;
		   case sp64_SX: return CfsBiomassConversionSupportedSpecies.spcsInt_SX;
		   case sp64_W: return CfsBiomassConversionSupportedSpecies.spcsInt_W;
		   case sp64_X: return CfsBiomassConversionSupportedSpecies.spcsInt_XC;
		   case sp64_YC: return CfsBiomassConversionSupportedSpecies.spcsInt_YC;
		   default: return CfsBiomassConversionSupportedSpecies.spcsInt_UNKNOWN;
		}
	}

	/**
	 * Convert the supplied Internal Species Index into its corresponding string.
	 *
	 * @param intSpeciesNdx the internal species index to be converted into a string.
	 * @return the string corresponding to the supplied internal species index. If 
	 * {@code intSpeciesNdx} is null or has the value "spcsInt_UNKNOWN", "??" is 
	 * returned.
	 */
	public static String lcl_InternalSpeciesIndexToString(CfsBiomassConversionSupportedSpecies intSpeciesNdx)
	{
		if (intSpeciesNdx == null || CfsBiomassConversionSupportedSpecies.spcsInt_UNKNOWN.equals(intSpeciesNdx)) {
			return "??";
		} else {
			return intSpeciesNdx.getText();
		}
	}

	/**
	 * Convert the supplied Internal Genus Index into its corresponding string.
	 *
	 * @param intGenusNdx the internal genus index to be converted into a string.
	 * @return the string corresponding to the supplied internal genus index. If 
	 * {@code intGenusNdx} is null or has the value "genusInt_INVALID", 
	 * "genusInt_INVALID" is returned.
	 */
	public static String lcl_InternalGenusIndexToString(CfsBiomassConversionSupportedGenera intGenusNdx) {
		if (intGenusNdx == null || intGenusNdx.equals(CfsBiomassConversionSupportedGenera.genusInt_INVALID))
			return "genusInt_INVALID";
		else
			return intGenusNdx.getText();
	}

	/**
	 * Convert the supplied Live Conversion Parameter into an identifying string.
	 *
	 * @param liveParam the conversion parameter to be converted into a string.
	 * @param nameFormat indicates in what format the enumeration constant is to be 
	 * converted.
	 * @return a string representation for the live conversion parameter. If
	 * {@code liveParam} has the value null or <code>cfsLiveParm_UNKNOWN</code>, 
	 * "cfsLiveParm_UNKNOWN" is returned.
	 */
	public static String lcl_LiveConversionParamToString(CfsLiveConversionParams liveParam, NameFormat nameFormat) {
		if (liveParam == null || liveParam.equals(CfsLiveConversionParams.cfsLiveParm_UNKNOWN)) {
			return "cfsLiveParm_UNKNOWN";
		} else if (nameFormat == null) {
			return liveParam.toString();
		} else {
			switch (nameFormat) {
			case catOnly:
				return liveParam.getCategory();
			case nameOnly:
				return liveParam.getText();
			case catName:
				return MessageFormat.format("{0} {1}", liveParam.getCategory(), liveParam.getText());
			default:
				throw new IllegalStateException(MessageFormat.format("Unsupported enumNameFormat {0}", nameFormat));
			}
		}
	}

	/**
	 * Convert the supplied Dead Conversion Parameter into an identifying string.
	 * <p>
	 * These strings are meant to be int names for the parameters.
	 * 
	 * @param deadParam the conversion parameter to be converted into a string.
	 * @param nameFormat indicates into which format the enumeration constant is to be converted.
	 * @return a string representation for the dead conversion parameter. "cfsDeadParm_UNKNOWN" 
	 * is returned if the parameter was not recognized.
	 */
	public static String lcl_DeadConversionParamToString(CfsDeadConversionParams deadParam, NameFormat nameFormat) {
		if (deadParam == null || deadParam.equals(CfsDeadConversionParams.cfsDeadParm_UNKNOWN)) {
			return "cfsDeadParm_UNKNOWN";
		} else if (nameFormat == null) {
			return deadParam.toString();
		} else {
			switch (nameFormat) {
			case catOnly:
				return "Dead";
			case nameOnly:
				return deadParam.getText();
			case catName:
				return MessageFormat.format("Dead {0}", deadParam, deadParam.getText());
			default:
				throw new IllegalStateException(MessageFormat.format("Unsupported enumNameFormat {0}", nameFormat));
			}
		}
	}

	/**
	 * Return the species number for the given CFS Tree Species.
	 * <p>
	 * CFS Species are defined in Appendix 7 of the document 'Model_based_volume_to_biomass_CFS.pdf' found in
	 * 'Documents/CFS-Biomass'.
	 *
	 * @param cfsSpcs the CFS tree species whose species number is to be returned.
	 * 
	 * @return as described.
	 */
	public static int SiteTool_CFSSpcsToCFSSpcsNum(CfsTreeSpecies cfsSpcs) {
		return CfsSpeciesMethods.getSpeciesIndexBySpecies(cfsSpcs);
	}

	/** 
	 * Determines if the supplied species is a deciduous or coniferous species.
	 * 
	 * @param sp64Index the SP64Name's -index- of species in question.
	 * @return as described
	 */
	public static boolean SiteTool_IsDeciduous(int sp64Index) {
	
		return VdypMethods.VDYP_IsDeciduous(SP64Name.forIndex(sp64Index));
	}

	/** 
	 * Determines if the supplied species is a softwood species.
	 * 
	 * @param spName the species short ("code") name.
	 * @return as described
	 */
	public static boolean SiteTool_IsSoftwood(String spName) {
		
		// Note that if spName is not a recognized species name, the correct default value is returned.
		return VdypMethods.speciesTable.getByCode(spName).isSoftwood();
	}

	/**
	 * Determines if the supplied species corresponds to a Pine species or not.
	 * 
	 * @param spName the species short ("code") name.
	 * @return {@code true} when the supplied species is a Pine related species and false if not, or the supplied
	 *         species was not recognized.
	 */
	public static boolean SiteTool_IsPine(String spName) {
		
		String sSP0 = VdypMethods.VDYP_GetVDYP7Species(spName);
		if (sSP0 != null) {
			switch (sSP0) {
			case "PA", "PL", "PW", "PY":
				return true;
			default:
				return false;
			}
		}
	
		return false;
	}

	/**
	 * Converts a species name to its corresponding CFS defined species.
	 * <p>
	 * The list of species mappings is defined in the file 'BCSpcsToCFSSpcs-SAS.txt' found in 'Documents/CFS-Biomass'.
	 * 
	 * @param spName the species short ("code") name.
	 * @return the mapping to the equivalent CFS defined tree species (if a mapping exists). {@code cfsSpcs_UNKNOWN} is
	 *         returned if the species was not recognized or a mapping does not exist.
	 */
	public static CfsTreeSpecies SiteTool_GetSpeciesCFSSpcs(String spName) {

		// Note that if spName is not a recognized species name, the correct default value is returned.
		return VdypMethods.speciesTable.getByCode(spName).cfsSpecies();
	}

	/**
	 * Returns the Canadian Forest Service Species Number corresponding to the MoF Species Number.
	 * <p>
	 * The mapping from MoF Species is defined in 'BCSpcsToCFSSpcs-SAS.txt' found in 'Documents/CFS-Biomass'.
	 *
	 * @param spName the species short ("code") name.
	 * @return the CFS Species Number corresponding to the MoF Species index, and -1 if the species 
	 * index is not in range or there is no mapping from the MoF Species to the CFS Species.
	 */
	public static int SiteTool_GetSpeciesCFSSpcsNum(String spName) {
		
		CfsTreeSpecies cfsSpcs = SiteTool_GetSpeciesCFSSpcs(spName);

		if (cfsSpcs != CfsTreeSpecies.cfsSpcs_UNKNOWN) {
			return SiteTool_CFSSpcsToCFSSpcsNum(cfsSpcs);
		} else {
			return -1;
		}
	}

	/**
	 * Converts a Height and Age to a Site Index for a particular Site Index Curve.
	 *
	 * @param curve the particular site index curve to project the height and age along.
	 *			This curve must be one of the active curves defined in "sindex.h"
	 * @param age the age of the trees indicated by the curve selection. The
	 *			interpretation of this age is modified by the 'ageType' parameter.
	 * @param ageType must be one of:
	 * <ul>
	 * <li>AT_TOTAL the age is the total age of the stand in years since planting.
	 * <li>AT_BREAST the age indicates the number of years since the stand reached breast height.
	 * </ul>
	 * @param height the height of the species in meters.
	 * @param estType must be one of:
	 * <ul>
	 * <li>SI_EST_DIRECT compute the site index based on direct equations if available. If 
	 * 		the equations are not available, then automatically fall to the SI_EST_ITERATE
	 *		method.
	 * <li> SI_EST_ITERATE compute the site index based on an iterative method which converges
	 * 		on the true site index.
	 * </ul>
	 * @return the site index of the pure species stand given the height and age.
	 */
	public static double SiteTool_HtAgeToSI(int curve, double age, int ageType, double height, int estType)
			throws CommonCalculatorException {
		
		Reference<Double> siRef = new Reference<>();
		// This method always returns 0; in the event of an error, an exception is thrown.
		Sindxdll.HtAgeToSI(curve, age, ageType, height, estType, siRef);
		
		double SI = siRef.get();

		// Round SI off to two decimals.
		SI = ((int) (SI * 100.0 + 0.5)) / 100.0;

		return SI;
	}
	
	/**
	 * Converts a Height and Site Index to an Age for a particular Site Index Curve.
	 *
	 * @param curve the particular site index curve to project the height and age along.
	 *			This curve must be one of the active curves defined in "sindex.h"
	 * @param height the height of the species in meters.
	 * @param ageType must be one of:
	 * <ul>
	 * <li>AT_TOTAL the age is the total age of the stand in years since planting.
	 * <li>AT_BREAST the age indicates the number of years since the stand reached breast height.
	 * </ul>
	 * @param siteIndex the site index value of the stand.
	 * @param years2BreastHeight the number of years it takes the stand to reach breast height.
	 * 
	 * @return the age of the stand (given the ageType) at which point it has reached the 
	 * height specified.
	 */
	public static double SiteTool_HtSIToAge(int curve, double height, int ageType, double siteIndex, 
			double years2BreastHeight)
				throws CommonCalculatorException {
		
		Reference<Double> tempRef_rtrn = new Reference<>();
		
		// This call always returns 0; in the event of an error, an exception is thrown.
		Sindxdll.HtSIToAge(curve, height, ageType, siteIndex, years2BreastHeight, tempRef_rtrn);
		
		return tempRef_rtrn.get();
	}

	/**
	 * Converts an Age and Site Index to a Height for a particular Site Index Curve.
	 *
	 * @param curve the particular site index curve to project the height and age along.
	 *		This curve must be one of the active curves defined in "sindex.h"
	 *
	 * @param age the age of the trees indicated by the curve selection. The
	 *		interpretation of this age is modified by the 'ageType' parameter.
	 *
	 * @param ageType must be one of:
	 * <ul>
	 * <li>AT_TOTAL the age is the total age of the stand in years since planting.
	 * <li>AT_BREAST the age indicates the number of years since the stand reached breast height.
	 * </ul>
	 * @param siteIndex the site index value of the stand.
	 * @param years2BreastHeight the number of years it takes the stand to reach breast height.
	 *	
	 * @return the height of the stand given the height and site index.
	 * 
	 * @throws CommonCalculatorException
	 */
	public static double SiteTool_AgeSIToHt(int curve, double age, int ageType, double siteIndex, 
				double years2BreastHeight)
			throws CommonCalculatorException {
		
		int freddieCurve = curve;
		int freddieAgeType = ageType;
		double freddieAge = age;
		double freddieSI = siteIndex;
		double freddieY2BH = years2BreastHeight;

		Reference<Double> tempRef_rtrn = new Reference<>();
		
		// This call always returns 0; if an error occurs, an exception is thrown.
		Sindxdll.AgeSIToHt(freddieCurve, freddieAge, freddieAgeType, freddieSI, freddieY2BH, tempRef_rtrn);

		return tempRef_rtrn.get();
	}

	/**
	 * Calculates the number of years a stand takes to grow from seed to breast height.
	 *
	 * @param curve the particular site index curve to project the height and age along.
	 *			This curve must be one of the active curves defined in "sindex.h"
	 * @param siteIndex the site index value of the stand.
	 * @return the number of years to grow from seed to breast height.
	 * @throws CommonCalculatorException in the event of an error
	 */
	public static double SiteTool_YearsToBreastHeight(int curve, double siteIndex) throws CommonCalculatorException {
		double rtrn = 0.0;

		Reference<Double> tempRef_rtrn = new Reference<>(rtrn);
		
		// This call always returns 0; if an error occurs, an exception is thrown.
		Sindxdll.Y2BH(curve, siteIndex, tempRef_rtrn);

		rtrn = tempRef_rtrn.get();

		// Round off to 1 decimal.
		rtrn = Math.round((int) (rtrn * 10.0 + 0.5)) / 10.0;

		return rtrn;
	}

	/**
	 * Returns the name of a particular curve.
	 * 
	 * @param siCurve the site index curve to get the name of.
	 * @return string corresponding the name of the supplied curve number. "Unknown 
	 * 		Curve" is returned for unrecognized curves.
	 */
	public static String SiteTool_SICurveName(int siCurve) {
		String retStr;

		try {
			retStr = Sindxdll.CurveName(siCurve);
		} catch (CurveErrorException e) {
			retStr = "Unknown Curve";
		}

		return retStr;
	}

	public static int SiteTool_NumSpecies() {
		return VdypMethods.VDYP_NumDefinedSpecies();
	}

	public static String SiteTool_SpeciesShortName(int sp64Index) {
		return VdypMethods.VDYP_GetSpeciesShortName(SP64Name.forIndex(sp64Index));
	}

	public static int SiteTool_SpeciesIndex(String spcsCodeName) {
		return VdypMethods.VDYP_SpeciesIndex(spcsCodeName);
	}

	public static String SiteTool_SpeciesFullName(String spcsCodeName) {
		return VdypMethods.VDYP_GetSpeciesFullName(SP64Name.forText(spcsCodeName));
	}

	public static String SiteTool_SpeciesLatinName(String spcsCodeName) {
		return VdypMethods.VDYP_GetSpeciesLatinName(SP64Name.forText(spcsCodeName));
	}

	public static String SiteTool_SpeciesGenusCode(String spcsCodeName) {
		return VdypMethods.VDYP_GetSpeciesGenus(SP64Name.forText(spcsCodeName));
	}

	public static String SiteTool_SpeciesSINDEXCode(String spcsCode, boolean isCoastal) {
		return VdypMethods.VDYP_GetSINDEXSpecies(spcsCode, 
				isCoastal ? SpeciesRegion.spcsRgn_Coast : SpeciesRegion.spcsRgn_Interior);
	}

	public static String SiteTool_SpeciesVDYP7Code(String spcsCode) {
		return VdypMethods.VDYP_GetVDYP7Species(spcsCode);
	}

	/**
	 * Sets the Site Index curve to use for a particular species.
	 *
	 * @param speciesCodeName the short ("code") name of the species.
	 * @param coastalInd if <code>true</code>, the Coastal region is used and otherwise Interior is used.
	 * @param siCurve the site index curve to use for the specified species. -1 resets the curve 
	 * 		to the default.
	 *
	 * @return the previous value.
	 */
	public static int SiteTool_SetSICurve(String speciesCodeName, boolean coastalInd, int siCurve) {
		
		SpeciesRegion region = (coastalInd ? SpeciesRegion.spcsRgn_Coast : SpeciesRegion.spcsRgn_Interior);
		return VdypMethods.VDYP_SetCurrentSICurve(speciesCodeName, region, siCurve);
	}

	/**
	 * Maps a Species code name to a specific SI Curve.
	 *
	 * @param spcsCodeName the species short ("code") name.
	 * @param isCoastal <code>true</code> if coastal, <code>false</code> if interior.
	 * @return the SI Curve number for the species, or -1 if the species was not recognized.
	 */
	public static int SiteTool_GetSICurve(String spcsCode, boolean isCoastal) {

		return VdypMethods.VDYP_GetCurrentSICurve(spcsCode, 
				isCoastal ? SpeciesRegion.spcsRgn_Coast : SpeciesRegion.spcsRgn_Interior);
	}

	/**
	 * Converts a SI Curve number to a Species code name, or "" if the SI Curve number
	 * is not recognized.
	 * 
	 * @param siCurve the SI Curve number for the species
	 * @return the short ("code") name of the species, in SIndex33 format (leading character
	 * in upper case; following characters in lower case.)
	 */
	public static String SiteTool_SiteCurveSINDEXSpecies(int siCurve) {
		
		int spcsNdx = VdypMethods.VDYP_GetSICurveSpeciesIndex(siCurve);

		String spcsNm;
		if (spcsNdx >= 0) {
			spcsNm = Sindxdll.SpecCode(spcsNdx);
		} else {
			spcsNm = "";
		}

		return spcsNm;
	}

	/**
	 * For a specific species, returns the default Crown Closure associated with that species within a particular region
	 * of the province.
	 *
	 * @param spName the short ("code") name to be looked up.
	 * @param isCoastal if <code>true</code>, region is Coastal. Otherwise, the region is Interior.
	 *
	 * @return the default CC associated with the species in that particular region and -1.0 if the species was not
	 *         recognized or no default CC has been assigned to that species and region.
	 */
	public static float SiteTool_SpeciesDefaultCrownClosure(String speciesCodeName, boolean isCoastal) {
		return VdypMethods.VDYP_GetDefaultCrownClosure(
				speciesCodeName, (isCoastal ? SpeciesRegion.spcsRgn_Coast : SpeciesRegion.spcsRgn_Interior)
		);
	}

	/**
	 * Compute the third of three age values given two of the others. Exactly one of these parameters
	 * must be -9.0. The other two must be set to valid values. If this does not hold, then nothing is
	 * computed.
	 * <p>
	 * This routine implements the equation:
	 * <p>
	 * Total Age = Breast Height Age + YTBH - 0.5
	 * <p>
	 * As this equation recently changed, it was thought to place it in a
	 * single routine to automatically keep all the individualcalculations
	 * up to date. Previously, all places that needed to calculate the
	 * third value, did this "in-place" and now all have to be seartched for
	 * and modified.
	 * <p>
	 * The following note from Gordon Nigh explains the rationale behind
	 * the 0.5 half year age correction. It is dated June 11, 2003 and was
	 * received from Cam embedded in his e-mail dated January 21, 2004:
	 * <p>
	 * Further comments: there has been some confusion about the
	 * issues surrounding age in Sindex. I would like to clarify them
	 * here, and make a proposition.
	 * <ol>
	 * <li>The newer ministry recommended height-age curves have the
	 * 0.5 age correction. These curves have been developed
	 * internally. The older curves do not have the age correction.
	 * This correction (documented in Research Report 03) was
	 * implemented to make the models consistent with the definition
	 * of breast height age, which is the number of annual growth rings
	 * at breast height. Since the innermost ring represents a half
	 * years growth (on average), the height-age models acutally go
	 * through a height of 1.3 at breast height age 0.5, not age 0 (it
	 * also means that site index is the height of the tree at 49.5
	 * growing seasons after the tree reaches breast height).
	 * Therefore, the age adjustment explicitiy incorporates this
	 * assumption (i.e., tree reaches breast height midway through the
	 * growing season) into the height-age model.
	 * <li>Logic has it that if we make this assumption for the height-age
	 * curves, then we should make the same assumption for the
	 * years-to-breast-height functions as well. That is, if the
	 * years-to-breast-height functions give us the number of years to
	 * reach breast height, then this number should end in 0.5. To do
	 * this, we had KenP truncate the decimal part of the years to
	 * breast height estimate, and then add 0.5.
	 * <li>Total age is just the number of years the tree has been growing.
	 * If we add breast height age to years-to-breast-height, we get
	 * xx.5 (see item 2 above) but we should be getting a whole number.
	 * This 0.5 year "error" results because of the definition of
	 * breast height age. At breast height age nn the tree has only
	 * been growing nn-0.5 years since it reached breast height (e.g.
	 * it has only been growing 0.5 years since it reached breast
	 * height at breast height age 1). Therefore,
	 *
	 * Total Age = Breast Height Age + Years-to-Breast-Height - 0.5.
	 * <li>
	 * It seems to me that if we accept item 1 above (and I think it is
	 * generally accepted now since it is logically correct), then items 2
	 * and 3 should follow automatically to make the systems internally
	 * consistent with respect to the assumptions.
	 * </ol>
	 *
	 * @param rTotalAge        total age of the stand, or -9.0 if unknown
	 * @param rBreastHeightAge breast height age of the stand, or -9.0 if unknown
	 * @param rYTBH            years to breast height of the stand, or -9.0 if unknown
	 *
	 * @return always returns zero. In the future, this routine will return an error code, but none has
	 *         been defined for this library yet.
	 */
	public static int SiteTool_FillInAgeTriplet(Reference<Double> rTotalAge, Reference<Double> rBreastHeightAge,
			Reference<Double> rYTBH) {
		
		int rtrnCode = 0;

		// Ensure the parameters have values - one of them must be set to "unknown".
		if (rTotalAge.isPresent() && rBreastHeightAge.isPresent() && rYTBH.isPresent()) {
			// All the parameters are supplied, perform the calculation based on which of the two values are supplied.

			// Note that because BHAge can be negative, we must use the less restrictive test of not being equal to

			if (rTotalAge.get() >= 0.0 && rBreastHeightAge.get() != -9.0 && rYTBH.get() < 0.0 /* unknown */) {
				rYTBH.set(rTotalAge.get() - rBreastHeightAge.get() + 0.5);
			} else if (rTotalAge.get() >= 0.0 && rBreastHeightAge.get() == -9.0 /* unknown */ && rYTBH.get() >= 0.0) {
				rBreastHeightAge.set(rTotalAge.get() - rYTBH.get() + 0.5);
			} else if (rTotalAge.get() < 0.0 /* unknown */ && rBreastHeightAge.get() != -9.0 && rYTBH.get() >= 0.0) {
				rTotalAge.set(rBreastHeightAge.get() + rYTBH.get() - 0.5);
			}
		}

		return rtrnCode;
	}
}