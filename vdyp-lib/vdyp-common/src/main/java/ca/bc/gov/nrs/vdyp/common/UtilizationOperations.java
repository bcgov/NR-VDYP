package ca.bc.gov.nrs.vdyp.common;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

public class UtilizationOperations {

	/**
	 * Perform the following operations on the UtilizationVectors of the given polygon.
	 * <ol>
	 * <li>Scale the per-hectare values of all the utilizations of the primary layer of the given polygon, and
	 * <li>For all utilizations of both the primary and veteran layer (if present) of the polygon:
	 * <ul>
	 * <li>Adjust the basal area to be within bounds of the utilization class, and 
	 * <li>Calculate the quad-mean-diameter value from the basal area and trees per hectare.
	 * </ul>
	 * </ol>
	 * 
	 * @param polygon the polygon on which to operate
	 */
	public static void doPostCreateAdjustments(VdypPolygon polygon) throws ProcessingException {

		float percentForestedLand = polygon.getPercentAvailable();
		assert !Float.isNaN(percentForestedLand);
		float scalingFactor = 100.0f / percentForestedLand;

		List<VdypUtilizationHolder> utilizationsToAdjust = new ArrayList<>();

		for (VdypLayer l : polygon.getLayers().values()) {

			utilizationsToAdjust.add(l);

			l.getSpecies().values().stream().forEach(s -> utilizationsToAdjust.add(s));
		}

		for (VdypUtilizationHolder uh : utilizationsToAdjust) {

			if (percentForestedLand > 0.0f && percentForestedLand < 100.0f) {
				scale(uh, scalingFactor);
			}

			// Implements the logic in BANKIN2 (ICHECK == 2) adjusting the utilization values according to various rules.

			resetOnMissingValues(uh);

			adjustBasalAreaToMatchTreesPerHectare(uh);

			doCalculateQuadMeanDiameter(uh);
		}
	}
	
	/**
	 * Implements VDYPGETU lines 224 - 229, in which the utilization-per-hectare values are scaled by 
	 * the given factor - the % coverage of the primary layer.
	 *
	 * @param scalingFactor the factor by which the <code>uh</code> is to be scaled
	 */
	public static void scale(VdypUtilizationHolder uh, float scalingFactor) {
	
		for (UtilizationClass uc: UtilizationClass.values()) {
			float basalArea = uh.getBaseAreaByUtilization().get(uc);
			if (basalArea > 0) {
				uh.getBaseAreaByUtilization().set(uc, basalArea * scalingFactor);
			}
			float treesPerHectare = uh.getTreesPerHectareByUtilization().get(uc);
			if (treesPerHectare > 0) {
				uh.getTreesPerHectareByUtilization().set(uc, treesPerHectare * scalingFactor);
			}
			// lorey height is not a per-hectare value and therefore
			// is excluded from scaling.
			float wholeStemVolume = uh.getWholeStemVolumeByUtilization().get(uc);
			if (wholeStemVolume > 0) {
				uh.getWholeStemVolumeByUtilization().set(uc, wholeStemVolume * scalingFactor);
			}
			float closeUtilizationVolume = uh.getCloseUtilizationVolumeByUtilization().get(uc);
			if (closeUtilizationVolume > 0) {
				uh.getCloseUtilizationVolumeByUtilization().set(uc, closeUtilizationVolume * scalingFactor);
			}
			float cuVolumeMinusDecay = uh.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc);
			if (cuVolumeMinusDecay > 0) {
				uh.getCloseUtilizationVolumeNetOfDecayByUtilization().set(uc, cuVolumeMinusDecay * scalingFactor);
			}
			float cuVolumeMinusDecayWastage = uh.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc);
			if (cuVolumeMinusDecayWastage > 0) {
				uh.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(uc, cuVolumeMinusDecayWastage * scalingFactor);
			}
			float cuVolumeMinusDecayWastageBreakage = uh.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().get(uc);
			if (cuVolumeMinusDecayWastageBreakage > 0) {
				uh.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().set(uc, cuVolumeMinusDecayWastageBreakage * scalingFactor);
			}
			// quadratic mean diameter is not a per-hectare value and
			// therefore not scaled.
		}
	}

	private static final float MAX_ACCEPTABLE_BASAL_AREA_ERROR = 0.1f;
	private static final UtilizationVector CLASS_LOWER_BOUNDS = new UtilizationVector(4.0f, 7.5f, 7.5f, 12.5f, 17.5f, 22.5f);
	private static final UtilizationVector CLASS_UPPER_BOUNDS = new UtilizationVector(7.5f, 2000.0f, 12.5f, 17.5f, 22.5f, 2000.0f);
	private static final float DQ_EPS = 0.005f;

	/**
	 * If either basalArea or liveTreesPerHectare is not positive, clear everything.
	 */
	private static void resetOnMissingValues(VdypUtilizationHolder uh) {

		for (UtilizationClass uc: UtilizationClass.values()) {
			if (uh.getBaseAreaByUtilization().get(uc) <= 0.0f || uh.getTreesPerHectareByUtilization().get(uc) <= 0.0f) {
				uh.getBaseAreaByUtilization().set(uc, 0.0f);
				uh.getTreesPerHectareByUtilization().set(uc, 0.0f);
				// DO NOT zero-out the lorey height value.
				uh.getWholeStemVolumeByUtilization().set(uc, 0.0f);
				uh.getCloseUtilizationVolumeByUtilization().set(uc, 0.0f);
				uh.getCloseUtilizationVolumeNetOfDecayByUtilization().set(uc, 0.0f);
				uh.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().set(uc, 0.0f);
				uh.getCloseUtilizationVolumeNetOfDecayWasteAndBreakageByUtilization().set(uc, 0.0f);
			}
		}
	}

	/**
	 * Adjust Basal Area to match the Trees-Per-Hectare value.
	 *
	 * @throws ProcessingException
	 */
	private static void adjustBasalAreaToMatchTreesPerHectare(VdypUtilizationHolder uh) throws ProcessingException {

		for (UtilizationClass uc: UtilizationClass.values()) {
			float tph = uh.getTreesPerHectareByUtilization().get(uc);
			if (tph > 0.0f) {
				float basalAreaLowerBound = BaseAreaTreeDensityDiameter
						.basalArea(CLASS_LOWER_BOUNDS.get(uc) + DQ_EPS, tph);
				float basalAreaUpperBound = BaseAreaTreeDensityDiameter
						.basalArea(CLASS_UPPER_BOUNDS.get(uc) - DQ_EPS, tph);
	
				float basalAreaError;
				float newBasalArea;
				String message = null;
	
				float basalArea = uh.getBaseAreaByUtilization().get(uc);
				if (basalArea < basalAreaLowerBound) {
					basalAreaError = FloatMath.abs(basalArea - basalAreaLowerBound);
					newBasalArea = basalAreaLowerBound;
					message = MessageFormat.format(
							"{0}: Error 6: basal area {1} is {2} below threshold, exceeding the maximum error of {3}.",
							uh, basalArea, basalAreaError, MAX_ACCEPTABLE_BASAL_AREA_ERROR
					);
				} else if (basalArea > basalAreaUpperBound) {
					basalAreaError = FloatMath.abs(basalArea - basalAreaUpperBound);
					message = MessageFormat.format(
							"{0}: Error 6: basal area {1} is {2} above threshold, exceeding the maximum error of {3}.",
							uh, basalArea, basalAreaError, MAX_ACCEPTABLE_BASAL_AREA_ERROR
					);
					newBasalArea = basalAreaUpperBound;
				} else {
					basalAreaError = 0.0f;
					newBasalArea = basalArea;
				}
	
				if (basalAreaError > MAX_ACCEPTABLE_BASAL_AREA_ERROR) {
					throw new ProcessingException(message);
				} else {
					uh.getBaseAreaByUtilization().set(uc, newBasalArea);
				}
			}
		}
	}

	/**
	 * Calculate QuadMeanDiameter for the given utilization.
	 *
	 * The value supplied in the input is IGNORED REPEAT IGNORED
	 *
	 * @throws ProcessingException
	 */
	private static void doCalculateQuadMeanDiameter(VdypUtilizationHolder uh) throws ProcessingException {

		for (UtilizationClass uc: UtilizationClass.values()) {
			float basalArea = uh.getBaseAreaByUtilization().get(uc);
			if (basalArea > 0.0f) {
				float tph = uh.getTreesPerHectareByUtilization().get(uc);
				float qmd = BaseAreaTreeDensityDiameter.quadMeanDiameter(basalArea, tph);
	
				if (qmd < CLASS_LOWER_BOUNDS.get(uc)) {
					qmd = qmd + DQ_EPS;
					if (qmd /* is still */ < CLASS_LOWER_BOUNDS.get(uc)) {
						throw new ProcessingException(
								MessageFormat.format(
										"{0}: Error 6: calculated quad-mean-diameter value {1} is below lower limit {2}",
										uh, qmd, CLASS_LOWER_BOUNDS.get(uc)
								)
						);
					}
				} else if (qmd > CLASS_UPPER_BOUNDS.get(uc)) {
					qmd = qmd - DQ_EPS;
					if (qmd /* is still */ > CLASS_UPPER_BOUNDS.get(uc)) {
						throw new ProcessingException(
								MessageFormat.format(
										"{0}: Error 6: calculated quad-mean-diameter value {1} is above upper limit {2}",
										uh, qmd, CLASS_UPPER_BOUNDS.get(uc)
								)
						);
					}
				}
	
				uh.getQuadraticMeanDiameterByUtilization().set(uc, qmd);
			}
		}
	}
}
