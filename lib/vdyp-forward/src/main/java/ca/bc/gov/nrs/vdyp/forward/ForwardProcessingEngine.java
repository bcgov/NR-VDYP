package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.math.FloatMath.clamp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.exp;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.log;
import static ca.bc.gov.nrs.vdyp.math.FloatMath.pow;
import static java.lang.Math.max;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.RuntimeProcessingException;
import ca.bc.gov.nrs.vdyp.application.StandProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.common.ReconcilationMethods;
import ca.bc.gov.nrs.vdyp.common.Reference;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.common_calculators.BaseAreaTreeDensityDiameter;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CommonCalculatorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.CurveErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.NoAnswerException;
import ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions.SpeciesErrorException;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexAgeType;
import ca.bc.gov.nrs.vdyp.common_calculators.enumerations.SiteIndexEquation;
import ca.bc.gov.nrs.vdyp.forward.model.ControlVariable;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings.Vars;
import ca.bc.gov.nrs.vdyp.io.parse.coe.UpperBoundsParser;
import ca.bc.gov.nrs.vdyp.io.write.VdypOutputWriter;
import ca.bc.gov.nrs.vdyp.math.FloatMath;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.CommonData;
import ca.bc.gov.nrs.vdyp.model.CompatibilityVariableMode;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2Impl;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3Impl;
import ca.bc.gov.nrs.vdyp.model.ModelCoefficients;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;
import ca.bc.gov.nrs.vdyp.model.Sp64Distribution;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;
import ca.bc.gov.nrs.vdyp.model.VolumeComputeMode;
import ca.bc.gov.nrs.vdyp.model.VolumeVariable;
import ca.bc.gov.nrs.vdyp.si32.site.SiteTool;

/**
 * This class manages "growing" polygons. Create an instance of the class, providing a control map, and then call
 * <code>processPolygon</code> for each polygon to be processed. All calls to <code>processPolygon</code> are entirely
 * independent of one another, allowing (different) polygons to the processed in parallel.
 */
public class ForwardProcessingEngine {

	private static final Logger logger = LoggerFactory.getLogger(ForwardProcessor.class);

	private static final int UC_ALL_INDEX = UtilizationClass.ALL.ordinal();
	private static final int UC_SMALL_INDEX = UtilizationClass.SMALL.ordinal();

	public static final float MIN_BASAL_AREA = 0.001f;

	/** π/4/10⁴ */
	public static final float PI_40K = (float) (Math.PI / 40_000);

	/* pp */ final ForwardProcessingState fps;

	/** The entity to which result information is written */
	private Optional<VdypOutputWriter> outputWriter = Optional.empty();

	private final boolean doCheckpoint;

	public ForwardProcessingEngine(Map<String, Object> controlMap, Optional<VdypOutputWriter> outputWriter)
			throws ProcessingException {
		this.fps = new ForwardProcessingState(controlMap);
		this.outputWriter = outputWriter;

		int cv7Value = this.fps.fcm.getForwardControlVariables().getControlVariable(ControlVariable.CHECKPOINT_7);
		doCheckpoint = cv7Value == 1;
	}

	public ForwardProcessingEngine(Map<String, Object> controlMap) throws ProcessingException {
		this(controlMap, Optional.empty());
	}

	public enum ExecutionStep {
		// Must be first
		NONE, //

		CHECK_FOR_WORK, //
		CALCULATE_MISSING_SITE_CURVES, //
		CALCULATE_COVERAGES, //
		DETERMINE_POLYGON_RANKINGS, //
		ESTIMATE_MISSING_SITE_INDICES, //
		ESTIMATE_MISSING_YEARS_TO_BREAST_HEIGHT_VALUES, //
		CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX, //
		SET_COMPATIBILITY_VARIABLES, //
		GROW_1_LAYER_DHDELTA, //
		GROW_2_LAYER_BADELTA, //
		GROW_3_LAYER_DQDELTA, //
		GROW_4_LAYER_BA_AND_DQTPH_EST, //
		GROW_5A_LH_EST, //
		GROW_5_SPECIES_BADQTPH, //
		GROW_6_LAYER_TPH2, //
		GROW_7_LAYER_DQ2, //
		GROW_8_SPECIES_LH, //
		GROW_9_SPECIES_PCT, //
		GROW_10_COMPATIBILITY_VARS, //
		GROW_11_SPECIES_UC, //
		GROW_12_SPECIES_UC_SMALL, //
		GROW_13_STORE_SPECIES_DETAILS, //
		GROW, //

		// Must be last
		ALL; //

		public ExecutionStep predecessor() {
			if (this == NONE) {
				throw new IllegalStateException("ExecutionStep.None has no predecessor");
			}

			return ExecutionStep.values()[ordinal() - 1];
		}

		public ExecutionStep successor() {
			if (this == ALL) {
				throw new IllegalStateException("ExecutionStep.All has no successor");
			}

			return ExecutionStep.values()[ordinal() + 1];
		}

		public boolean lt(ExecutionStep that) {
			return this.ordinal() < that.ordinal();
		}

		public boolean le(ExecutionStep that) {
			return this.ordinal() <= that.ordinal();
		}

		public boolean eq(ExecutionStep that) {
			return this.ordinal() == that.ordinal();
		}

		public boolean ge(ExecutionStep that) {
			return this.ordinal() >= that.ordinal();
		}

		public boolean gt(ExecutionStep that) {
			return this.ordinal() > that.ordinal();
		}
	}

	/**
	 * Run all steps of the engine on the given polygon.
	 *
	 * @param polygon the polygon on which to operate
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public void processPolygon(VdypPolygon polygon) throws ProcessingException {

		processPolygon(polygon, ExecutionStep.ALL);
	}

	/**
	 * Run all steps of the engine on the given polygon up to and including the given <code>lastStep</code>.
	 *
	 * @param polygon           the polygon on which to operate
	 * @param lastStepInclusive execute up to and including this step
	 *
	 * @throws ProcessingException should an error with the data occur during processing
	 */
	public void processPolygon(VdypPolygon polygon, ExecutionStep lastStepInclusive) throws ProcessingException {

		logger.info("Starting processing of the primary layer of polygon {}", polygon.getPolygonIdentifier());

		fps.setPolygon(polygon);

		// All of BANKCHK1 that we need. Note that setting UC ALL (METH_CHK == 1) in BANKCHK1 is
		// performed when the Bank instance is created in setPolygon, above.

		validatePolygon(polygon);

		// Determine the target year of the growth
		int targetYear;

		int growTargetControlVariableValue = fps.fcm.getForwardControlVariables()
				.getControlVariable(ControlVariable.GROW_TARGET_1);
		if (growTargetControlVariableValue == -1) {
			if (polygon.getTargetYear().isEmpty()) {
				throw new ProcessingException(
						"Control Variable 1 has the value -1, indicating that the grow-to years are"
								+ " to be read from a grow-to-year file (at " + ControlKey.FORWARD_INPUT_GROWTO.name()
								+ " in the control file), but no such file was specified."
				);
			}
			targetYear = polygon.getTargetYear().get();
		} else {
			if (growTargetControlVariableValue <= 400) {
				targetYear = polygon.getPolygonIdentifier().getYear() + growTargetControlVariableValue;
			} else {
				targetYear = growTargetControlVariableValue;
			}
		}

		// Run the forward algorithm for this polygon

		executeForwardAlgorithm(lastStepInclusive, targetYear);
	}

	private void executeForwardAlgorithm(ExecutionStep lastStepInclusive, int stoppingYearInclusive)
			throws ProcessingException {

		LayerProcessingState plps = fps.getPrimaryLayerProcessingState();

		Optional<VdypLayer> veteranLayer = Optional
				.ofNullable(fps.getCurrentPolygon().getLayers().get(LayerType.VETERAN));

		if (lastStepInclusive.ge(ExecutionStep.CHECK_FOR_WORK)) {
			stopIfNoWork(fps);
		}

		// SCINXSET
		if (lastStepInclusive.ge(ExecutionStep.CALCULATE_MISSING_SITE_CURVES)) {
			calculateMissingSiteCurves(plps, fps.fcm.getSiteCurveMap());

			fps.getVeteranLayerProcessingState().ifPresent(vlps -> {
				assert veteranLayer.isPresent();
				calculateMissingSiteCurves(vlps, fps.fcm.getSiteCurveMap());
			});
		}

		// VPRIME1, method == 1
		if (lastStepInclusive.ge(ExecutionStep.CALCULATE_COVERAGES)) {
			calculateCoverages(plps);
		}

		if (lastStepInclusive.ge(ExecutionStep.DETERMINE_POLYGON_RANKINGS)) {
			determinePolygonRankings(CommonData.PRIMARY_SPECIES_TO_COMBINE);
		}

		// SITEADD (TODO: SITEADDU when NDEBUG 11 > 0)
		if (lastStepInclusive.ge(ExecutionStep.ESTIMATE_MISSING_SITE_INDICES)) {
			estimateMissingSiteIndices(plps);
		}

		if (lastStepInclusive.ge(ExecutionStep.ESTIMATE_MISSING_YEARS_TO_BREAST_HEIGHT_VALUES)) {
			estimateMissingYearsToBreastHeightValues(plps);
		}

		// VHDOM1 METH_H = 2, METH_A = 2, METH_SI = 2
		if (lastStepInclusive.ge(ExecutionStep.CALCULATE_DOMINANT_HEIGHT_AGE_SITE_INDEX)) {
			calculateDominantHeightAgeSiteIndex(plps, fps.fcm.getHl1Coefficients());
		}

		// CVSET1
		if (lastStepInclusive.ge(ExecutionStep.SET_COMPATIBILITY_VARIABLES)) {
			setCompatibilityVariables();
		}

		// VGROW1
		if (lastStepInclusive.gt(ExecutionStep.SET_COMPATIBILITY_VARIABLES)) {
			int startingYear = fps.getCurrentStartingYear();

			writeCurrentPolygon(startingYear, startingYear, stoppingYearInclusive);

			boolean doRecalculateGroupsPriorToOutput = fps.fcm.getDebugSettings()
					.getValue(ForwardDebugSettings.Vars.SPECIES_DYNAMICS_1) != 1 && plps.getNSpecies() > 1;

			boolean doRecalculateGroups = fps.fcm.getForwardControlVariables()
					.getControlVariable(ControlVariable.UPDATE_DURING_GROWTH_6) >= 1;

			int currentYear = startingYear + 1;
			while (currentYear <= stoppingYearInclusive) {

				logger.info(
						"Growing polygon {} Primary layer for year {}", fps.getCompactPolygonIdentifier(), currentYear
				);

				grow(plps, currentYear, veteranLayer, lastStepInclusive);

				// Some unit tests require only some of the grow steps to be executed (and, by
				// implication, only for the first growth year.) If this is the case, stop
				// processing now.
				if (ExecutionStep.ALL.gt(lastStepInclusive)) {
					break;
				}

				// If update-during-growth is set, update the context prior to output
				if (doRecalculateGroupsPriorToOutput) {
					calculateCoverages(plps);
					calculateDominantHeightAgeSiteIndex(plps, fps.fcm.getHl1Coefficients());
				}

				// Write polygon (both primary and veteran layers) to output
				writeCurrentPolygon(startingYear, currentYear, stoppingYearInclusive);

				// If update-during-growth is set, and not already recalculated, recalculate
				// context now.
				if (!doRecalculateGroupsPriorToOutput && doRecalculateGroups) {
					calculateCoverages(plps);
					calculateDominantHeightAgeSiteIndex(plps, fps.fcm.getHl1Coefficients());
				}

				currentYear += 1;
			}
		}
	}

	/**
	 * VGROW1 - "grow" the primary layer, located in <code>lps.bank</code>, starting at the given year, and return the
	 * resulting bank. The veteran layer instance is supplied but at this point is ignored.
	 *
	 * @param lps               the layer processing state
	 * @param currentYear       the current growth period
	 * @param veteranLayer      the polygon's veteran layer
	 * @param lastStepInclusive
	 * @return as described
	 *
	 * @throws ProcessingException
	 */
	private void grow(
			LayerProcessingState lps, int currentYear, Optional<VdypLayer> veteranLayer, ExecutionStep lastStepInclusive
	) throws ProcessingException {

		assert lastStepInclusive.ge(ExecutionStep.GROW_1_LAYER_DHDELTA);

		Bank bank = lps.getBank();

		float dhStart = lps.getPrimarySpeciesDominantHeight();
		int pspSiteCurveNumber = lps.getSiteCurveNumber(lps.getPrimarySpeciesIndex());
		float pspSiteIndex = lps.getPrimarySpeciesSiteIndex();
		float pspYtbhStart = lps.getPrimarySpeciesAgeToBreastHeight();
		float pspYabhStart = lps.getPrimarySpeciesAgeAtBreastHeight();

		// (1) Calculate change in dominant height (layer)

		float dhDelta = calculateDominantHeightDelta(dhStart, pspSiteCurveNumber, pspSiteIndex, pspYtbhStart);

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_1_LAYER_DHDELTA.eq(lastStepInclusive))
			return;

		// (2) Calculate change in basal area (layer)

		final Optional<Float> veteranLayerBasalArea = veteranLayer
				.flatMap((l) -> Optional.of(l.getBaseAreaByUtilization().get(UtilizationClass.ALL)));

		float dqStart = bank.quadMeanDiameters[0][UC_ALL_INDEX];
		float baStart = bank.basalAreas[0][UC_ALL_INDEX];
		float tphStart = bank.treesPerHectare[0][UC_ALL_INDEX];
		float lhStart = bank.loreyHeights[0][UC_ALL_INDEX];

		float baDelta = calculateBasalAreaDelta(pspYabhStart, dhStart, baStart, veteranLayerBasalArea, dhDelta);

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_2_LAYER_BADELTA.eq(lastStepInclusive))
			return;

		// (3) Calculate change in quad-mean-diameter (layer)

		Reference<Boolean> wasDqGrowthLimitApplied = new Reference<>();
		float dqDelta = calculateQuadMeanDiameterDelta(
				pspYabhStart, baStart, dhStart, dqStart, veteranLayerBasalArea, veteranLayerBasalArea, dhDelta,
				wasDqGrowthLimitApplied
		);

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_3_LAYER_DQDELTA.eq(lastStepInclusive))
			return;

		int debugSetting9Value = fps.fcm.getDebugSettings()
				.getValue(ForwardDebugSettings.Vars.DO_LIMIT_BA_WHEN_DQ_LIMITED_9);
		if (debugSetting9Value == 1 && wasDqGrowthLimitApplied.get() /* is true */) {
			// Limit BA growth if DQ hit limit.
			float dqEnd = dqStart + dqDelta;
			float baEndMax = baStart * (dqEnd * dqEnd) / (dqStart * dqStart);
			baDelta = Math.min(baDelta, baEndMax - baStart);
		}

		float baChangeRate = baDelta / baStart;

		// (4) Begin storing computed results - dq, ba and tph for the layer

		// Cache some values for calculations below.

		float pspLhStart = bank.loreyHeights[lps.getPrimarySpeciesIndex()][UC_ALL_INDEX];
		float pspTphStart = bank.treesPerHectare[lps.getPrimarySpeciesIndex()][UC_ALL_INDEX];

		float dhEnd = dhStart + dhDelta;
		float dqEnd = dqStart + dqDelta;
		float baEnd = baStart + baDelta;
		float tphEnd = BaseAreaTreeDensityDiameter.treesPerHectare(baEnd, dqEnd);
		float tphMultiplier = tphEnd / tphStart;

		bank.quadMeanDiameters[0][UC_ALL_INDEX] = dqEnd;
		bank.basalAreas[0][UC_ALL_INDEX] = baEnd;
		bank.treesPerHectare[0][UC_ALL_INDEX] = tphEnd;

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_4_LAYER_BA_AND_DQTPH_EST.eq(lastStepInclusive))
			return;

		// (5) Now calculate per-species (UC All only) end values for basal area, quad-mean-diameter
		// and trees-per-hectare, using one of several options: "full species dynamics",
		// "partial species dynamics" or "no species dynamics."

		int debugSetting1Value = fps.fcm.getDebugSettings().getValue(ForwardDebugSettings.Vars.SPECIES_DYNAMICS_1);

		boolean wasSolutionFound = false;
		if (debugSetting1Value == 2) {
			// (5a) This is the PARTIAL SPECIES DYNAMICS section.

			// (5a1) Begin by updating HL for all species (UC All only), as well
			// as the per-layer value (UC All).

			// First save the at-start Lorey Height values, needed for the - they will be restored
			// below and re-calculated later once more precise information is known.

			float[] lhAtStart = new float[bank.getNSpecies() + 1];
			lhAtStart[0] = bank.loreyHeights[0][UC_ALL_INDEX];
			for (int i : bank.getIndices()) {
				lhAtStart[i] = bank.loreyHeights[i][UC_ALL_INDEX];
			}

			// Compute the per-species Lorey Height estimates.

			float pspTphEndEstimate = pspTphStart * (tphEnd / tphStart);

			growLoreyHeights(lps, dhStart, dhEnd, pspTphStart, pspTphEndEstimate, pspLhStart);

			// Calculate the per-site Lorey Height estimate.

			float sum1 = 0.0f;
			float sum2 = 0.0f;

			for (int i : bank.getIndices()) {
				sum1 += bank.basalAreas[i][UC_ALL_INDEX] * bank.loreyHeights[i][UC_ALL_INDEX];
				sum2 += bank.basalAreas[i][UC_ALL_INDEX];
			}

			bank.loreyHeights[0][UC_ALL_INDEX] = sum1 / sum2;

			writeCheckpoint(currentYear);

			if (ExecutionStep.GROW_5A_LH_EST.eq(lastStepInclusive))
				return;

			// Now do the actual per-species updates of ba, qmd and tph, based in part
			// on both the starting Lorey Heights and the estimated Lorey Heights at the
			// end of the growth period.

			wasSolutionFound = growUsingPartialSpeciesDynamics(baStart, baDelta, dqStart, dqDelta, tphStart, lhAtStart);

			// Restore the Lorey Heights back to the values at the beginning of the period.
			// They will be updated below using the new estimate of TPH-primary species.

			for (int i = 0; i <= bank.getNSpecies(); i++) {
				bank.loreyHeights[i][UC_ALL_INDEX] = lhAtStart[i];
			}
		}

		if (!wasSolutionFound) {
			// Calculate the basal area, trees-per-hectare and quad-mean-diameter for all
			// species in the polygon (UC All)

			if (debugSetting1Value == 1 || bank.getNSpecies() == 1) {

				// (5b) This is the NO SPECIES DYNAMICS section
				growUsingNoSpeciesDynamics(baChangeRate, tphMultiplier);
			} else {

				// (5c) This is the FULL SPECIES DYNAMICS section
				growUsingFullSpeciesDynamics(baStart, baDelta, dqStart, dqDelta, tphStart, lhStart);
			}
		}

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_5_SPECIES_BADQTPH.eq(lastStepInclusive))
			return;

		// (6) Calculate layer trees-per-hectare, UC All

		float tphEndSum = 0.0f;
		for (int i : bank.getIndices()) {
			if (bank.basalAreas[i][UC_ALL_INDEX] > 0.0f) {
				tphEndSum += bank.treesPerHectare[i][UC_ALL_INDEX];
			}
		}

		if (tphEndSum < 0.0f) {
			throw new ProcessingException(
					MessageFormat.format(
							"During processing of primary layer of {0}, trees-per-hectare was calculated to be negative ({1})",
							fps.getCompactPolygonIdentifier(), tphEndSum
					)
			);
		}

		bank.treesPerHectare[0][UC_ALL_INDEX] = tphEndSum;

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_6_LAYER_TPH2.eq(lastStepInclusive))
			return;

		// (7) Calculate layer quad-mean-diameter, uc All

		bank.quadMeanDiameters[0][UC_ALL_INDEX] = BaseAreaTreeDensityDiameter
				.quadMeanDiameter(bank.basalAreas[0][UC_ALL_INDEX], bank.treesPerHectare[0][UC_ALL_INDEX]);

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_7_LAYER_DQ2.eq(lastStepInclusive))
			return;

		// (8) Calculate per-species Lorey heights, uc All

		float pspTphEnd = bank.treesPerHectare[lps.getPrimarySpeciesIndex()][UC_ALL_INDEX];
		growLoreyHeights(lps, dhStart, dhEnd, pspTphStart, pspTphEnd, pspLhStart);

		// We now have site (layer) level predications for basal area, quad-mean-diameter,
		// trees-per-hectare and Lorey height. Proceed to per-species estimates.

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_8_SPECIES_LH.eq(lastStepInclusive))
			return;

		// (9) Calculate basal area percentages per species, uc UC_ALL_INDEX
		for (int i : bank.getIndices()) {
			bank.percentagesOfForestedLand[i] = 100.0f * bank.basalAreas[i][UC_ALL_INDEX]
					/ bank.basalAreas[0][UC_ALL_INDEX];
		}

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_9_SPECIES_PCT.eq(lastStepInclusive))
			return;

		// (10) update the compatibility variables to reflect the changes during the growth period
		lps.updateCompatibilityVariablesAfterGrowth();

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_10_COMPATIBILITY_VARS.eq(lastStepInclusive))
			return;

		// (11) calculate All and the large component volumes to reflect the changes in growth

		VdypLayer primaryLayer = fps.updatePolygon().getLayers().get(LayerType.PRIMARY);

		VolumeComputeMode volumeComputationMode = VolumeComputeMode.BY_UTIL_WITH_WHOLE_STEM_BY_SPEC;
		int controlVariable3Value = fps.fcm.getForwardControlVariables()
				.getControlVariable(ControlVariable.COMPAT_VAR_APPLICATION_3);
		CompatibilityVariableMode compatibilityVariableMode = CompatibilityVariableMode
				.getByInteger(controlVariable3Value);
		lps.getFps().computers.computeUtilizationComponentsPrimary(
				lps.getBecZone(), primaryLayer, volumeComputationMode, compatibilityVariableMode
		);

		bank.refreshBank(primaryLayer);

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_11_SPECIES_UC.eq(lastStepInclusive))
			return;

		// (12) calculate the small component volumes to reflect the changes in growth

		calculateSmallComponentYields(lps);

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_12_SPECIES_UC_SMALL.eq(lastStepInclusive))
			return;

		// (13) Update the running values.

		lps.updatePrimarySpeciesDetailsAfterGrowth(dhEnd);

		for (int i : lps.getIndices()) {
			if (i == lps.getPrimarySpeciesIndex()) {
				bank.ageTotals[i] = lps.getPrimarySpeciesTotalAge();
				bank.dominantHeights[i] = dhEnd;
				bank.siteIndices[i] = lps.getPrimarySpeciesSiteIndex();
				bank.yearsAtBreastHeight[i] = lps.getPrimarySpeciesAgeAtBreastHeight();
			} else {
				if (bank.ageTotals[i] > 0.0f) {
					bank.ageTotals[i] += 1;
				}
				if (bank.yearsAtBreastHeight[i] > 0.0) {
					bank.yearsAtBreastHeight[i] += 1;
				}

				float spSiStart = bank.siteIndices[i];
				float spDhStart = bank.dominantHeights[i];
				float spYtbh = bank.yearsToBreastHeight[i];
				float spYabhEnd = bank.yearsAtBreastHeight[i];

				if (!Float.isNaN(spSiStart) && !Float.isNaN(spDhStart) && !Float.isNaN(spYtbh)
						&& !Float.isNaN(spYabhEnd)) {

					float dhGrowth = calculateDominantHeightDelta(spDhStart, pspSiteCurveNumber, spSiStart, spYtbh);
					bank.dominantHeights[i] += dhGrowth;
				} else {
					bank.dominantHeights[i] = Float.NaN;
				}
			}
		}

		fps.getVeteranLayerProcessingState().ifPresent(vlps -> {
			assert veteranLayer.isPresent();
			updateVeteranSpeciesAges(vlps);
		});

		writeCheckpoint(currentYear);

		if (ExecutionStep.GROW_13_STORE_SPECIES_DETAILS.eq(lastStepInclusive))
			return;
	}

	private void updateVeteranSpeciesAges(LayerProcessingState vlps) {

		for (int i : vlps.getIndices()) {
			vlps.getBank().ageTotals[i] += 1;
			vlps.getBank().yearsAtBreastHeight[i] += 1;
		}

	}

	private void writeCheckpoint(int year) {
		if (doCheckpoint) {
			outputWriter.ifPresent(o -> {
				try {
					var polygon = fps.updatePolygon();
					o.writePolygonWithSpeciesAndUtilizationForYear(polygon, year);
				} catch (IOException e) {
					throw new RuntimeProcessingException(new ProcessingException(e));
				}
			});
		}
	}

	/**
	 * Calculate, using the "no species dynamics" algorithm, the basal area, trees-per-hectare and quad-mean-diameter at
	 * the end of the growth period for all species in the polygon, given the per-layer basal area and trees-per-hectare
	 * rate of change.
	 *
	 * @param baChangeRate  the rate of change of the basal area during the growth period, expressed as a percentage /
	 *                      100. So a rate of 10% (i.e., 1.1 times the starting value) would be expressed at 0.1f.
	 * @param tphChangeRate the rate of change of trees-per-hectare during the growth period, expressed the same way.
	 */
	void growUsingNoSpeciesDynamics(float baChangeRate, float tphChangeRate) {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		for (int i : lps.getIndices()) {

			float spBaStart = bank.basalAreas[i][UC_ALL_INDEX];
			if (spBaStart > 0.0f) {
				float spBaEnd = spBaStart * (1.0f + baChangeRate);
				float spTphStart = bank.treesPerHectare[i][UC_ALL_INDEX];
				float spTphEnd = spTphStart * tphChangeRate;
				float spDqEnd = BaseAreaTreeDensityDiameter.quadMeanDiameter(spBaEnd, spTphEnd);
				if (spDqEnd < 7.51f) {
					spDqEnd = 7.51f;
					spTphEnd = BaseAreaTreeDensityDiameter.treesPerHectare(spBaEnd, spDqEnd);
				}

				bank.basalAreas[i][UC_ALL_INDEX] = spBaEnd;
				bank.treesPerHectare[i][UC_ALL_INDEX] = spTphEnd;
				bank.quadMeanDiameters[i][UC_ALL_INDEX] = spDqEnd;
			}
		}
	}

	private static final int NSTAGES = 10;

	private record AdjustmentParameters(
			/** normal maximum change towards zero */
			float cjMax,
			/** true iff allowed to cross the zero boundary */
			boolean canCrossZero,
			/** allowable wrong-way change for species closest to zero */
			float cjWrongWayChange1, /** allowable wrong-way change for other species */
			float cjWrongWayChangeX
	) {
	}

	private static AdjustmentParameters[] adjustmentParametersByStage = new AdjustmentParameters[NSTAGES];

	static {
		adjustmentParametersByStage[0] = new AdjustmentParameters(0.01f, false, 0.0f, 0.0f);
		adjustmentParametersByStage[1] = new AdjustmentParameters(0.015f, true, 0.005f, 0.0f);
		adjustmentParametersByStage[2] = new AdjustmentParameters(0.03f, true, 0.02f, 0.01f);
		adjustmentParametersByStage[3] = new AdjustmentParameters(0.045f, true, 0.03f, 0.02f);
		adjustmentParametersByStage[4] = new AdjustmentParameters(0.06f, true, 0.045f, 0.035f);
		adjustmentParametersByStage[5] = adjustmentParametersByStage[0];
		adjustmentParametersByStage[6] = adjustmentParametersByStage[1];
		adjustmentParametersByStage[7] = adjustmentParametersByStage[2];
		adjustmentParametersByStage[8] = adjustmentParametersByStage[3];
		adjustmentParametersByStage[9] = adjustmentParametersByStage[4];
	}

	/**
	 * GRSPpart - calculate, using the "no species dynamics" algorithm, the basal area, trees-per-hectare and
	 * quad-mean-diameter at the end of the growth period for all species in the current layer of the polygon.
	 *
	 * @param baStart  overall basal area at start of growth period
	 * @param baDelta  change in overall basal area during the growth period
	 * @param dqStart  overall quad-mean-diameter at start of growth period
	 * @param dqDelta  change in overall quad-mean-diameter during the growth period
	 * @param tphStart overall trees-per-hectare value at the start of growth period
	 * @param hlStart  stand and per-species (UC All) Lorey heights at the start of the growth period index 0 - stand;
	 *                 indices 1 - # species for the individual species
	 *
	 * @return true if and only if a solution was found.
	 * @throws ProcessingException
	 */
	boolean growUsingPartialSpeciesDynamics(
			float baStart, float baDelta, float dqStart, float dqDelta, float tphStart, float[] hlStart
	) throws ProcessingException {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();
		Region polygonRegion = fps.getCurrentBecZone().getRegion();

		if (dqDelta == 0 || baDelta == 0 || lps.getNSpecies() == 1) {
			return false /* no solution available */;
		}

		float[] spDqEsts = new float[lps.getNSpecies() + 1];
		float[] spTphEsts = new float[lps.getNSpecies() + 1];
		float[] rs1 = new float[lps.getNSpecies() + 1];

		float[] spDqStartEsts = new float[lps.getNSpecies() + 1];
		float[] spDqEndEsts = new float[lps.getNSpecies() + 1];

		float[] baEndAll = new float[lps.getNSpecies() + 1];
		baEndAll[0] = baStart + baDelta;
		for (int i : lps.getIndices()) {
			baEndAll[i] = bank.basalAreas[i][UC_ALL_INDEX] * baEndAll[0] / baStart;
		}

		float[] dqEndAll = new float[lps.getNSpecies() + 1];
		dqEndAll[0] = dqStart + dqDelta;

		float[] tphEndAll = new float[lps.getNSpecies() + 1];
		tphEndAll[0] = BaseAreaTreeDensityDiameter.treesPerHectare(baEndAll[0], dqEndAll[0]);

		Map<String, Float> basalAreaPercentagesPerSpecies = new HashMap<>();
		for (String spAlias : fps.fcm.getGenusDefinitionMap().getAllGeneraAliases()) {
			basalAreaPercentagesPerSpecies.put(spAlias, 0.0f);
		}
		for (int i : lps.getIndices()) {
			basalAreaPercentagesPerSpecies.put(bank.speciesNames[i], bank.percentagesOfForestedLand[i] / 100.0f);
		}

		for (int i : lps.getIndices()) {

			spDqStartEsts[i] = fps.estimators.estimateQuadMeanDiameterForSpecies(
					bank.speciesNames[i], hlStart[i], bank.quadMeanDiameters[i][UC_ALL_INDEX],
					basalAreaPercentagesPerSpecies, lps.getBecZone().getRegion(), dqStart, baStart, tphStart, hlStart[0]
			);

			spDqEndEsts[i] = fps.estimators.estimateQuadMeanDiameterForSpecies(
					bank.speciesNames[i], bank.loreyHeights[i][UC_ALL_INDEX], dqEndAll[0],
					basalAreaPercentagesPerSpecies, lps.getBecZone().getRegion(), dqEndAll[0], baEndAll[0],
					tphEndAll[0], bank.loreyHeights[0][UC_ALL_INDEX]
			);
		}

		float[] dqLowerBoundBySpecies = new float[lps.getNSpecies() + 1];
		float[] dqUpperBoundBySpecies = new float[lps.getNSpecies() + 1];
		float[] tphLowerBoundBySpecies = new float[lps.getNSpecies() + 1];
		float[] tphUpperBoundBySpecies = new float[lps.getNSpecies() + 1];
		boolean exactMatchFound = false;
		int incorrectlySignedSpeciesIndex = 0;

		float tphLow = 0.0f;
		float tphHigh = 0.0f;

		int stage;
		for (stage = 0; stage < NSTAGES; stage++) {

			if (stage == 0 || stage == 5) {
				// Set bounds

				for (int i : lps.getIndices()) {
					dqLowerBoundBySpecies[i] = 7.51f;
					dqUpperBoundBySpecies[i] = 100.0f;

					if (bank.treesPerHectare[i][UC_ALL_INDEX] <= 0) {
						continue;
					}

					var sizeLimits = fps.estimators.getLimitsForHeightAndDiameter(bank.speciesNames[i], polygonRegion);
					var spDqMax = sizeLimits.quadMeanDiameterMaximum();

					float spDqStart = bank.quadMeanDiameters[i][UC_ALL_INDEX];

					// Non-negotiable bounds
					dqUpperBoundBySpecies[i] = FloatMath.max(dqEndAll[0], dqStart, spDqMax, spDqStart) + 10.0f;

					// Non-decline constraint imposed unless net change in ba/tree < 1%
					float dqNetChange = dqEndAll[0] / dqStart;
					float rateDq2 = dqNetChange * dqNetChange - 1.0f;
					if (rateDq2 > 0.01) {
						dqLowerBoundBySpecies[i] = spDqStart;
					} else {
						float dq2Min = spDqStart * spDqStart * (1.0f + rateDq2 - 0.01f);
						if (dq2Min > 0.0f) {
							dqLowerBoundBySpecies[i] = Math
									.max(dqLowerBoundBySpecies[i], Math.min(FloatMath.sqrt(dq2Min), spDqStart));
						}
					}

					if (stage >= 5) {
						continue;
					}

					// More restrictive bounds, stages 0 - 4 only.

					float spHlStart = bank.loreyHeights[i][UC_ALL_INDEX];

					float trialMax = Math.max(spDqStart, spDqMax);
					if (spDqStart < 1.001 * sizeLimits.maxQuadMeanDiameterLoreyHeightRatio() * hlStart[i]) {
						trialMax = Math.min(trialMax, sizeLimits.maxQuadMeanDiameterLoreyHeightRatio() * spHlStart);
					}

					dqUpperBoundBySpecies[i] = Math.min(dqUpperBoundBySpecies[i], trialMax);

					float spDqMin = sizeLimits.minQuadMeanDiameterLoreyHeightRatio() * hlStart[i];
					if (spDqStart > 0.999 * spDqMin) {
						float trialMin = sizeLimits.minQuadMeanDiameterLoreyHeightRatio() * spHlStart;
						dqLowerBoundBySpecies[i] = Math.max(dqLowerBoundBySpecies[i], trialMin);
					}
				}
			}

			// Bounds now set

			// With CJ = 0, subject to constraints, find the resultant trees-per-hectare.

			float tphEst = 0.0f;
			for (int i : lps.getIndices()) {
				if (bank.basalAreas[i][UC_ALL_INDEX] <= 0.0f) {
					continue;
				}

				float spDqStart = bank.quadMeanDiameters[i][UC_ALL_INDEX];

				spDqEsts[i] = 7.5f + (spDqEndEsts[i] - 7.5f) * ( (spDqStart - 7.5f) / (spDqStartEsts[i] - 7.5f));
				spDqEsts[i] = FloatMath.clamp(spDqEsts[i], dqLowerBoundBySpecies[i], dqUpperBoundBySpecies[i]);

				rs1[i] = FloatMath.log( (spDqStart - 7.5f) / (spDqStartEsts[i] - 7.5f));
				spTphEsts[i] = BaseAreaTreeDensityDiameter.treesPerHectare(baEndAll[i], spDqEsts[i]);
				tphEst += spTphEsts[i];
			}

			if (tphEst == tphEndAll[0]) {
				exactMatchFound = true;
				break;
			}

			boolean estIsAboveActual = tphEst > tphEndAll[0];
			incorrectlySignedSpeciesIndex = 0;
			float amountWrong = 50000.0f;

			for (int i : lps.getIndices()) {
				if (bank.basalAreas[i][UC_ALL_INDEX] <= 0.0f) {
					continue;
				}

				if (estIsAboveActual && rs1[i] > 0.0f) {
					if (rs1[i] < amountWrong) {
						incorrectlySignedSpeciesIndex = i;
						amountWrong = rs1[i];
					}
				} else if (!estIsAboveActual && rs1[i] < 0.0f) {
					if (-1.0f * rs1[i] < amountWrong) {
						incorrectlySignedSpeciesIndex = i;
						amountWrong = -rs1[i];
					}
				}
			}

			// Calculate the low and high quad-mean-diameters

			tphLow = 0.0f;
			tphHigh = 0.0f;

			float cjLow;
			float cjHigh;
			float cjOther;

			for (int i : lps.getIndices()) {
				if (bank.basalAreas[i][UC_ALL_INDEX] <= 0.0f) {
					continue;
				}

				if (i == incorrectlySignedSpeciesIndex) {
					cjOther = adjustmentParametersByStage[stage].cjWrongWayChange1;
				} else {
					cjOther = adjustmentParametersByStage[stage].cjWrongWayChangeX;
				}

				if (estIsAboveActual) {
					if (rs1[i] <= 0.0f) {
						cjLow = -cjOther;
						cjHigh = adjustmentParametersByStage[stage].cjMax;
						if (!adjustmentParametersByStage[stage].canCrossZero) {
							cjHigh = Math.min(cjHigh, -rs1[i]);
						}
					} else {
						cjLow = 0.0f;
						cjHigh = cjOther;
					}
				} else {
					if (rs1[i] <= 0.0f) {
						cjLow = -cjOther;
						cjHigh = 0.0f;
					} else {
						cjLow = -adjustmentParametersByStage[stage].cjMax;
						if (!adjustmentParametersByStage[stage].canCrossZero) {
							cjLow = Math.max(-adjustmentParametersByStage[stage].cjMax, -rs1[i]);
						}
						cjHigh = 0.0f;
					}
				}

				float trialDqLow = 7.5f + (spDqEndEsts[i] - 7.5f) * FloatMath.exp(rs1[i] + cjLow);
				trialDqLow = FloatMath.clamp(trialDqLow, dqLowerBoundBySpecies[i], dqUpperBoundBySpecies[i]);
				float trialDqHigh = 7.5f + (spDqEndEsts[i] - 7.5f) * FloatMath.exp(rs1[i] + cjHigh);
				trialDqHigh = FloatMath.clamp(trialDqHigh, dqLowerBoundBySpecies[i], dqUpperBoundBySpecies[i]);

				tphUpperBoundBySpecies[i] = BaseAreaTreeDensityDiameter.treesPerHectare(baEndAll[i], trialDqLow);
				tphLowerBoundBySpecies[i] = BaseAreaTreeDensityDiameter.treesPerHectare(baEndAll[i], trialDqHigh);

				tphLow += tphLowerBoundBySpecies[i];
				tphHigh += tphUpperBoundBySpecies[i];
			}

			if (tphEndAll[0] >= tphLow && tphEndAll[0] <= tphHigh) {
				break;
			}
		}

		if (stage + 1 == NSTAGES) {
			// Finished all stages and no viable solution found.
			return false;
		}

		if (!exactMatchFound) {
			// A viable solution was found, but not an exact one. Find the solution for trees.

			if (tphLow > tphHigh) {
				throw new ProcessingException(
						MessageFormat.format(
								"Polygon {0}, layer {1}: while computing growth"
										+ " using partial species dynamics, trees-per-hectare lower bound ({2}) was found to be"
										+ " greater than the upper bound ({3})",
								fps.getCompactPolygonIdentifier(), lps.getLayerType(), tphLow, tphHigh
						)
				);
			}

			float k;
			if (tphLow == tphHigh) {
				k = 0.0f;
			} else {
				k = (tphEndAll[0] - tphLow) / (tphHigh - tphLow);
			}

			for (int i : lps.getIndices()) {
				if (bank.basalAreas[i][UC_ALL_INDEX] >= 0.0f) {
					tphEndAll[i] = tphLowerBoundBySpecies[i]
							+ k * (tphUpperBoundBySpecies[i] - tphLowerBoundBySpecies[i]);
					dqEndAll[i] = BaseAreaTreeDensityDiameter.quadMeanDiameter(baEndAll[i], tphEndAll[i]);
				} else {
					baEndAll[i] = 0.0f;
					tphEndAll[i] = 0.0f;
					dqEndAll[i] = bank.quadMeanDiameters[i][UC_ALL_INDEX];
				}
			}
		} else {
			// An exact solution was found.
			for (int i : lps.getIndices()) {
				if (bank.basalAreas[i][UC_ALL_INDEX] <= 0.0f) {
					tphEndAll[i] = 0.0f;
					dqEndAll[i] = bank.quadMeanDiameters[i][UC_ALL_INDEX];
				} else {
					tphEndAll[i] = spTphEsts[i];
					dqEndAll[i] = spDqEsts[i];
				}
			}
		}

		for (int i : lps.getIndices()) {
			bank.basalAreas[i][UC_ALL_INDEX] = baEndAll[i];
			bank.quadMeanDiameters[i][UC_ALL_INDEX] = dqEndAll[i];
			bank.treesPerHectare[i][UC_ALL_INDEX] = tphEndAll[i];
		}

		return true /* was successful */;
	}

	/**
	 * GRSPBT - Calculate the overall per-species basal area, trees-per-hectare and quad-mean-diameter growth during the
	 * growth period. The result is stored in <code>end</code>, Utilization Class ALL.
	 *
	 * @param baStart  per-layer basal area at start of growth period
	 * @param baDelta  per-layer change in basal area during growth period
	 * @param dqStart  per-layer quad-mean-diameter at the start of growth period
	 * @param dqDelta  per-layer change in quad-mean-diameter during growth period
	 * @param tphStart per-layer trees-per-breast-height at that of growth period
	 * @param lhStart  per-layer Lorey height at the start of the growth period
	 * @throws ProcessingException
	 */
	void growUsingFullSpeciesDynamics(
			float baStart, float baDelta, float dqStart, float dqDelta, float tphStart, float lhStart
	) throws ProcessingException {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		float spBaEnd[] = new float[lps.getNSpecies() + 1];
		float spTphEnd[] = new float[lps.getNSpecies() + 1];
		float spDqEnd[] = new float[lps.getNSpecies() + 1];
		boolean doSkip[] = new boolean[lps.getNSpecies() + 1];

		for (int i : lps.getIndices()) {
			spBaEnd[i] = Float.NaN;
			spTphEnd[i] = Float.NaN;
			spDqEnd[i] = Float.NaN;

			doSkip[i] = false;
		}

		float sumSpBaDelta = 0.0f;
		float spBaDelta[] = new float[lps.getNSpecies() + 1];

		float pspLhStart = bank.loreyHeights[lps.getPrimarySpeciesIndex()][UC_ALL_INDEX];
		for (int i : lps.getIndices()) {
			if (i == lps.getPrimarySpeciesIndex()) {
				float pspBaStart = bank.basalAreas[i][UC_ALL_INDEX];
				float pspYabhStart = lps.getPrimarySpeciesAgeAtBreastHeight();

				// Note: the FORTRAN passes Lorey height into parameter "HD" ("Dominant Height") - are these
				// equivalent?
				spBaDelta[i] = growBasalAreaForPrimarySpecies(
						baStart, baDelta, pspBaStart, lhStart, pspYabhStart, pspLhStart
				);
			} else {
				float spBaStart = bank.basalAreas[i][UC_ALL_INDEX];
				float spDqStart = bank.quadMeanDiameters[i][UC_ALL_INDEX];
				float spLhStart = bank.loreyHeights[i][UC_ALL_INDEX];
				spBaDelta[i] = growBasalAreaForNonPrimarySpecies(
						bank.speciesNames[i], baStart, baDelta, pspLhStart, spBaStart, spDqStart, spLhStart
				);
			}

			sumSpBaDelta += spBaDelta[i];
		}

		{
			// Estimate of basal area growth by species

			// Iteratively attempt to find a value f such that:
			//
			// if revised spBaDelta = spBaStart * (f + spBaDelta / spBaStart)
			// then sum(revised spBaDelta) = baDelta
			//
			// for all species whose basal area after growth is non-negative.

			var baBase = baStart;
			var passNumber = 0;

			while (true) {

				var f = (baDelta - sumSpBaDelta) / baBase;

				int nSkipped = 0;
				sumSpBaDelta = 0.0f;

				for (int i : lps.getIndices()) {
					if (!doSkip[i]) {
						var spBaStart = lps.getBank().basalAreas[i][UC_ALL_INDEX];
						spBaEnd[i] = spBaStart + spBaDelta[i] + f * spBaStart;
						if (spBaEnd[i] < 0.0f) {
							spBaEnd[i] = 0.0f;
							doSkip[i] = true;
							nSkipped += 1;
							sumSpBaDelta -= spBaStart;
							baBase -= spBaStart;
						} else {
							sumSpBaDelta += spBaEnd[i] - spBaStart;
						}
					}
				}

				if (nSkipped == 0) {
					break;
				}

				passNumber += 1;
				if (passNumber > 5 || baBase <= 0.0f) {
					throw new ProcessingException(
							MessageFormat.format(
									"Unable to converge on a value for \"f\" in"
											+ " growUsingFullSpeciesDynamics({0}, {1}, {2}, {3}, {4}, {5})",
									dqDelta, baDelta, baStart, dqStart, tphStart, lhStart
							)
					);
				}
			}
		}

		{
			// Estimate of quad-mean-diameter growth by species

			var passNumber = 0;
			var bestScore = 1000.0;
			var bestF = Float.NaN;

			while (true) {

				var f = 0.0f;
				var nSkipped = 0;
				var totalBasalAreaSkipped = 0.0f;

				for (int i : lps.getIndices()) {
					float spDqStart = bank.quadMeanDiameters[i][UC_ALL_INDEX];
					float spLhStart = bank.loreyHeights[i][UC_ALL_INDEX];

					float spDqDelta;
					if (i == lps.getPrimarySpeciesIndex()) {

						spDqDelta = calculateQuadMeanDiameterDeltaForPrimarySpecies(
								dqStart, dqDelta, spDqStart, lhStart, spLhStart
						);
					} else {
						spDqDelta = calculateQuadMeanDiameterDeltaForNonPrimarySpecies(
								i, dqStart, dqDelta, spDqStart, lhStart, spLhStart
						);
					}

					spDqDelta += f;

					ComponentSizeLimits csl = getComponentSizeLimits(
							bank.speciesNames[i], lps.getBecZone().getRegion()
					);

					var spLhAllStart = bank.loreyHeights[i][UC_ALL_INDEX];

					float spDqMaximum = Math.min(
							csl.quadMeanDiameterMaximum(), csl.maxQuadMeanDiameterLoreyHeightRatio() * spLhAllStart
					);

					if (spDqStart + spDqDelta > spDqMaximum) {
						spDqDelta = Math.min(0.0f, spDqMaximum - spDqStart);
						nSkipped += 1;
						totalBasalAreaSkipped += bank.basalAreas[i][UC_ALL_INDEX];
					}

					float spDqMinimum = Math.max(
							UtilizationClass.U75TO125.lowBound + 1.0f,
							csl.minQuadMeanDiameterLoreyHeightRatio() * spLhAllStart
					);

					if (spDqStart + spDqDelta < spDqMinimum) {
						spDqDelta = spDqMinimum - spDqStart;
						nSkipped += 1;
						totalBasalAreaSkipped += bank.basalAreas[i][UC_ALL_INDEX];
					}

					spDqEnd[i] = spDqStart + spDqDelta;
				}

				float tph = 0.0f;
				for (int i : bank.getIndices()) {
					if (spBaEnd[i] > 0.0f) {
						spTphEnd[i] = BaseAreaTreeDensityDiameter.treesPerHectare(spBaEnd[i], spDqEnd[i]);
					} else {
						spTphEnd[i] = 0.0f;
					}
					tph += spTphEnd[i];
				}

				if (passNumber == 15 || (nSkipped == lps.getNSpecies() && passNumber > 2)) {
					break;
				}

				float dqNewBar = BaseAreaTreeDensityDiameter.quadMeanDiameter(baStart + baDelta, tph);
				float dqStartEstimate = BaseAreaTreeDensityDiameter.quadMeanDiameter(baStart, tphStart);
				float dqWant = dqStartEstimate + dqDelta;

				var score = FloatMath.abs(dqWant - dqNewBar);
				if (score < bestScore) {
					bestScore = score;
					bestF = f;
				}

				if (FloatMath.abs(score) < 0.001) {
					break;
				}

				if (totalBasalAreaSkipped > 0.7f) {
					totalBasalAreaSkipped = 0.7f * baStart;
				}

				score = score * baStart / (baStart - totalBasalAreaSkipped);
				f += score;

				passNumber += 1;
				if (passNumber == 15) {
					f = bestF;
				}
			}

			for (int i : bank.getIndices()) {
				bank.basalAreas[i][UC_ALL_INDEX] = spBaEnd[i];
				bank.treesPerHectare[i][UC_ALL_INDEX] = spTphEnd[i];
				if (spBaEnd[i] > 0.0f) {
					bank.quadMeanDiameters[i][UC_ALL_INDEX] = BaseAreaTreeDensityDiameter
							.quadMeanDiameter(spBaEnd[i], spTphEnd[i]);
				}
			}
		}
	}

	/**
	 * EMP061 - get the component size limits for the given genus and alias.
	 *
	 * @param genusAlias the alias of the genus in question
	 * @param region     the region in question
	 *
	 * @return as described
	 */
	private ComponentSizeLimits getComponentSizeLimits(String genusAlias, Region region) {
		return fps.fcm.getComponentSizeLimits().get(genusAlias, region);
	}

	/**
	 * EMP150 - return the change in quad-mean-diameter over the growth period for the primary species of the Primary
	 * layer. Based on IPSJF150.doc (July, 1999).
	 *
	 * @param dqStart    primary layer quad-mean-diameter at start of growth period
	 * @param dqDelta    change in quad-mean-diameter of primary layer during growth period
	 * @param pspDqStart primary species quad-mean-diameter at start of growth period
	 * @param lhStart    primary layer Lorey height at start of growth period
	 * @param pspLhStart primary species Lorey height at start of growth period
	 *
	 * @return as described
	 * @throws ProcessingException in the event of an error
	 */
	private float calculateQuadMeanDiameterDeltaForPrimarySpecies(
			float dqStart, float dqDelta, float pspDqStart, float lhStart, float pspLhStart
	) throws ProcessingException {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		int pspStratumNumber = lps.getPrimarySpeciesStratumNumber();

		ModelCoefficients mc = fps.fcm.getPrimarySpeciesQuadMeanDiameterGrowthCoefficients().get(pspStratumNumber);

		if (mc == null) {
			throw new ProcessingException(
					MessageFormat.format(
							"primaryQuadMeanDiameterGrowthCoefficients do not exist"
									+ " for stratum number {0}, call growQuadMeanDiameterForPrimarySpecies("
									+ "{1}, {2}, {3}, {4}, {5})",
							dqStart, dqDelta, pspDqStart, lhStart, pspLhStart
					)
			);
		}

		return calculateQuadMeanDiameterDelta(mc.getCoefficients(), dqStart, dqDelta, pspDqStart, lhStart, pspLhStart);
	}

	/**
	 * EMP151 - calculate and return the quad-mean-diameter delta over the growth period for a non-primary species.
	 *
	 * @param speciesIndex the index of the species (1..nSpecies) in the current bank
	 * @param dqStart      layer quad-mean-diameter at start of growth period
	 * @param dqDelta      layer quad-mean-diameter delta over growth period
	 * @param spDqStart    species quad-mean-diameter at start of growth period
	 * @param lhStart      layer Lorey height at start of growth period
	 * @param spLhStart    species Lorey height at start of growth period
	 * @return as described
	 * @throws ProcessingException
	 */
	private float calculateQuadMeanDiameterDeltaForNonPrimarySpecies(
			int speciesIndex, float dqStart, float dqDelta, float spDqStart, float lhStart, float spLhStart
	) throws ProcessingException {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();

		String speciesName = lps.getBank().speciesNames[speciesIndex];
		int pspStratumNumber = lps.getPrimarySpeciesStratumNumber();

		var modelCoefficientsOpt = fps.fcm.getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients()
				.get(speciesName, pspStratumNumber);

		if (modelCoefficientsOpt.isEmpty()) {
			modelCoefficientsOpt = fps.fcm.getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients().get(speciesName, 0);
		}

		if (modelCoefficientsOpt.isEmpty()) {
			throw new ProcessingException(
					MessageFormat.format(
							"No nonPrimarySpeciesQuadMeanDiameterGrowthCoefficients exist for stratum {0}"
									+ "; call growQuadMeanDiameterForNonPrimarySpecies({1}, {2}, {3}, {4}, {5}, {6})",
							pspStratumNumber, speciesIndex, dqStart, dqDelta, lhStart, spDqStart, spLhStart
					)
			);
		}

		return calculateQuadMeanDiameterDelta(
				modelCoefficientsOpt.get(), dqStart, dqDelta, spDqStart, lhStart, spLhStart
		);
	}

	/**
	 * Calculate and return the quad-mean-diameter delta over the growth period for a non-primary species.
	 *
	 * @param mc        coefficients to use for the calculation
	 * @param dqStart   layer quad-mean-diameter at start of growth period
	 * @param dqDelta   layer quad-mean-diameter delta over growth period
	 * @param spDqStart species quad-mean-diameter at start of growth period
	 * @param lhStart   layer Lorey height at start of growth period
	 * @param spLhStart species Lorey height at start of growth period
	 * @return as described
	 * @throws ProcessingException
	 */
	private static float calculateQuadMeanDiameterDelta(
			Coefficients mc, float dqStart, float dqDelta, float spDqStart, float lhStart, float spLhStart
	) {
		float a0 = mc.getCoe(1);
		float a1 = mc.getCoe(2);
		float a2 = mc.getCoe(3);

		final float dqBase = UtilizationClass.U75TO125.lowBound - 0.05f;

		float dqRateStart = (spDqStart - dqBase) / (dqStart - dqBase);
		float logDqRateStart = FloatMath.log(dqRateStart);
		float logDqRateDelta = a0 + a1 * FloatMath.log(spDqStart) + a2 * spLhStart / lhStart;
		float dqRateEnd = FloatMath.exp(logDqRateStart + logDqRateDelta);

		float spDqEnd = dqRateEnd * (dqStart + dqDelta - dqBase) + dqBase;
		if (spDqEnd < UtilizationClass.U75TO125.lowBound + 0.01f) {
			spDqEnd = UtilizationClass.U75TO125.lowBound + 0.01f;
		}

		float spDqDelta = spDqEnd - spDqStart;

		return spDqDelta;
	}

	/**
	 * EMP149 - return the basal area growth for non-Primary species of Primary layer. Based on IPSJF149.doc (July,
	 * 1999).
	 *
	 * @param speciesName
	 * @param baStart     layer basal area at start of growth period
	 * @param baDelta     layer basal area growth during period
	 * @param lhStart     layer Lorey height at start of growth period
	 * @param spBaStart   this species basal area at start of growth period
	 * @param spDqStart   this species quad-mean-diameter at start of growth period
	 * @param spLhStart   this species Lorey height at start of growth period\
	 *
	 * @return as described
	 * @throws ProcessingException
	 */
	private float growBasalAreaForNonPrimarySpecies(
			String speciesName, float baStart, float baDelta, float lhStart, float spBaStart, float spDqStart,
			float spLhStart
	) throws ProcessingException {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();

		if (spBaStart <= 0.0f || spBaStart >= baStart) {
			throw new ProcessingException(
					MessageFormat.format(
							"Species basal area {0} is out of range; it must be"
									+ " positive and less that overall basal area {1}",
							spBaStart, baStart
					)
			);
		}

		int pspStratumNumber = lps.getPrimarySpeciesStratumNumber();

		var coe = fps.fcm.getNonPrimarySpeciesBasalAreaGrowthCoefficients();
		var modelCoefficientsOpt = coe.get(speciesName, pspStratumNumber);

		if (modelCoefficientsOpt.isEmpty()) {
			modelCoefficientsOpt = fps.fcm.getNonPrimarySpeciesBasalAreaGrowthCoefficients().get(speciesName, 0);
		}

		if (modelCoefficientsOpt.isEmpty()) {
			throw new ProcessingException(
					MessageFormat.format(
							"No nonPrimarySpeciesBasalAreaGrowthCoefficients exist for stratum {0}"
									+ "; call growBasalAreaForNonPrimarySpecies({0}, {1}, {2}, {3}, {4}, {5}, {6}, {7})",
							pspStratumNumber, speciesName, baStart, baDelta, lhStart, spBaStart, spDqStart, spLhStart
					)
			);
		}

		Coefficients mc = modelCoefficientsOpt.get();

		float a0 = mc.getCoe(1);
		float a1 = mc.getCoe(2);
		float a2 = mc.getCoe(3);

		var baProportionStart = spBaStart / baStart;
		var logBaProportionStart = FloatMath.log(baProportionStart / (1.0f - baProportionStart));

		var logBaDeltaProportion = a0 + a1 * FloatMath.log(spDqStart) + a2 * spLhStart / lhStart;
		var logBaProportionEnd = logBaProportionStart + logBaDeltaProportion;
		var baProportionEnd = FloatMath.exp(logBaProportionEnd) / (1.0f + FloatMath.exp(logBaProportionEnd));

		var spDeltaBa = baProportionEnd * (baStart + baDelta) - spBaStart;

		return spDeltaBa;
	}

	/**
	 * EMP148 - return the basal area growth for Primary species of Primary layer. Based on IPSJF148.doc (July, 1999).
	 *
	 * @param baStart      layer basal area at start of growth period
	 * @param baDelta      layer basal area growth during period
	 * @param pspBaStart   primary species basal area at start of growth period
	 * @param dhStart      dominant height of primary species at start of growth period
	 * @param pspYabhStart primary species years at breast height at start of growth period
	 * @param pspLhStart   primary species Lorey height at start of growth period
	 *
	 * @return as described
	 * @throws ProcessingException
	 */
	private float growBasalAreaForPrimarySpecies(
			float baStart, float baDelta, float pspBaStart, float dhStart, float pspYabhStart, float pspLhStart
	) throws ProcessingException {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();

		float pspBaDelta;

		float spToAllProportionStart = pspBaStart / baStart;
		if (spToAllProportionStart <= 0.999f) {
			var psStratumNumber = lps.getPrimarySpeciesStratumNumber();

			ModelCoefficients mc = fps.fcm.getPrimarySpeciesBasalAreaGrowthCoefficients().get(psStratumNumber);
			int model = mc.getModel();
			var a0 = mc.getCoefficients().getCoe(1);
			var a1 = mc.getCoefficients().getCoe(2);
			var a2 = mc.getCoefficients().getCoe(3);

			var logBaProportionStart = FloatMath.log(spToAllProportionStart / (1.0f - spToAllProportionStart));

			float logBaDeltaProportion;
			if (model == 3) {
				logBaDeltaProportion = a0 + a1 * dhStart;
			} else if (model == 8) {
				logBaDeltaProportion = a0 + a1 * pspYabhStart + a2 * pspLhStart / dhStart;
			} else if (model == 9) {
				logBaDeltaProportion = a0 + a1 * logBaProportionStart + a2 * baStart;
			} else {
				throw new ProcessingException(
						MessageFormat
								.format("Model value {} for polygon stratum {} is out of range", model, psStratumNumber)
				);
			}

			var x = FloatMath.exp(logBaProportionStart + logBaDeltaProportion);
			var spToAllProportionEnd = x / (1 + x);
			pspBaDelta = spToAllProportionEnd * (baStart + baDelta) - pspBaStart;
		} else {
			pspBaDelta = baDelta;
		}

		return pspBaDelta;
	}

	/**
	 * YSMALL - records in bank the small component utilization values for current layer.
	 *
	 * @throws ProcessingException
	 */
	private void calculateSmallComponentYields(LayerProcessingState lps) throws ProcessingException {

		Bank bank = lps.getBank();

		float lhSum = 0.0f;
		float baSum = 0.0f;
		float tphSum = 0.0f;
		float wsVolumeSum = 0.0f;

		for (int speciesIndex : lps.getIndices()) {

			float spLhAll = bank.loreyHeights[speciesIndex][UC_ALL_INDEX];
			float spBaAll = bank.basalAreas[speciesIndex][UC_ALL_INDEX];
			float spDqAll = bank.quadMeanDiameters[speciesIndex][UC_ALL_INDEX];

			Region region = lps.getBecZone().getRegion();
			String speciesName = bank.speciesNames[speciesIndex];

			// EMP080
			float smallProbability = smallComponentProbability(speciesName, spLhAll, region);

			// This whole operation is on Actual BA's, not 100% occupancy.
			float fractionAvailable = fps.getCurrentPolygon().getPercentAvailable() / 100.0f;

			if (fractionAvailable > 0.0f) {
				spBaAll *= fractionAvailable;
			}

			// EMP081
			float conditionalExpectedBasalArea = calculateSmallComponentConditionalExpectedBasalArea(
					speciesName, spBaAll, spLhAll, region
			);

			if (fractionAvailable > 0.0f) {
				conditionalExpectedBasalArea /= fractionAvailable;
			}

			float spBaSmall = smallProbability * conditionalExpectedBasalArea;

			// EMP082
			float spDqSmall = smallComponentQuadMeanDiameter(speciesName, spLhAll); // DQSMsp

			// EMP085
			float spLhSmall = smallComponentLoreyHeight(speciesName, spLhAll, spDqSmall, spDqAll); // HLSMsp

			// EMP086
			float meanVolumeSmall = meanVolumeSmall(speciesName, spLhSmall, spDqSmall); // VMEANSMs

			int controlVar3Value = fps.fcm.getForwardControlVariables()
					.getControlVariable(ControlVariable.COMPAT_VAR_APPLICATION_3);

			if (controlVar3Value >= 1 /* apply compatibility variables */) {
				spBaSmall += lps.getCVSmall(speciesIndex, UtilizationClassVariable.BASAL_AREA);
				if (spBaSmall < 0.0f) {
					spBaSmall = 0.0f;
				}
				spDqSmall += lps.getCVSmall(speciesIndex, UtilizationClassVariable.QUAD_MEAN_DIAMETER);
				FloatMath.clamp(spDqSmall, 4.01f, 7.49f);

				spLhSmall = 1.3f + (spLhSmall - 1.3f)
						* FloatMath.exp(lps.getCVSmall(speciesIndex, UtilizationClassVariable.LOREY_HEIGHT));

				if (controlVar3Value >= 2 && meanVolumeSmall > 0.0f) {
					meanVolumeSmall *= FloatMath
							.exp(lps.getCVSmall(speciesIndex, UtilizationClassVariable.WHOLE_STEM_VOLUME));
				}
			}

			float spTphSmall = BaseAreaTreeDensityDiameter.treesPerHectare(spBaSmall, spDqSmall);
			float spWsVolumeSmall = spTphSmall * meanVolumeSmall;

			bank.loreyHeights[speciesIndex][UC_SMALL_INDEX] = spLhSmall;
			bank.basalAreas[speciesIndex][UC_SMALL_INDEX] = spBaSmall;
			bank.treesPerHectare[speciesIndex][UC_SMALL_INDEX] = spTphSmall;
			bank.quadMeanDiameters[speciesIndex][UC_SMALL_INDEX] = spDqSmall;
			bank.wholeStemVolumes[speciesIndex][UC_SMALL_INDEX] = spWsVolumeSmall;
			bank.closeUtilizationVolumes[speciesIndex][UC_SMALL_INDEX] = 0.0f;
			bank.cuVolumesMinusDecay[speciesIndex][UC_SMALL_INDEX] = 0.0f;
			bank.cuVolumesMinusDecayAndWastage[speciesIndex][UC_SMALL_INDEX] = 0.0f;

			lhSum += spBaSmall * spLhSmall;
			baSum += spBaSmall;
			tphSum += spTphSmall;
			wsVolumeSum += spWsVolumeSmall;
		}

		if (baSum > 0.0) {
			bank.loreyHeights[0][UC_SMALL_INDEX] = lhSum / baSum;
		} else {
			bank.loreyHeights[0][UC_SMALL_INDEX] = 0.0f;
		}
		bank.basalAreas[0][UC_SMALL_INDEX] = baSum;
		bank.treesPerHectare[0][UC_SMALL_INDEX] = tphSum;
		bank.quadMeanDiameters[0][UC_SMALL_INDEX] = BaseAreaTreeDensityDiameter.quadMeanDiameter(baSum, tphSum);
		bank.wholeStemVolumes[0][UC_SMALL_INDEX] = wsVolumeSum;
		bank.closeUtilizationVolumes[0][UC_SMALL_INDEX] = 0.0f;
		bank.cuVolumesMinusDecay[0][UC_SMALL_INDEX] = 0.0f;
		bank.cuVolumesMinusDecayAndWastage[0][UC_SMALL_INDEX] = 0.0f;
	}

	/**
	 * GRSPHL - estimate the Lorey Heights of all species at end of growth period. The results of the calculations are
	 * persisted in {@code lps.bank}.
	 *
	 * @param lps         processing state context
	 * @param dhStart     (primary species) dominant height at start
	 * @param dhEnd       (primary species) dominant height at end
	 * @param pspTphStart primary species trees-per-hectare at start
	 * @param pspTphEnd   primary species trees-per-hectare at end
	 * @param pspLhStart  primary species Lorey height at end
	 */
	void growLoreyHeights(
			LayerProcessingState lps, float dhStart, float dhEnd, float pspTphStart, float pspTphEnd, float pspLhStart
	) {
		Bank bank = lps.getBank();

		float pspLhStartEstimate = estimatePrimarySpeciesLoreyHeight(dhStart, pspTphStart);
		float pspLhEndEstimate = estimatePrimarySpeciesLoreyHeight(dhEnd, pspTphEnd);

		float primaryF = (pspLhStart - 1.3f) / (pspLhStartEstimate - 1.3f);
		float primaryLhAdjustment = fps.fcm.getCompVarAdjustments().getLoreyHeightPrimaryParam();
		primaryF = 1.0f + (primaryF - 1.0f) * primaryLhAdjustment;

		float pspLhEnd = 1.3f + (pspLhEndEstimate - 1.3f) * primaryF;

		int debugSetting8Value = fps.fcm.getDebugSettings()
				.getValue(ForwardDebugSettings.Vars.LOREY_HEIGHT_CHANGE_STRATEGY_8);

		int primarySpeciesIndex = fps.getPrimaryLayerProcessingState().getPrimarySpeciesIndex();
		if (debugSetting8Value != 2 || dhStart != dhEnd) {
			bank.loreyHeights[primarySpeciesIndex][UC_ALL_INDEX] = pspLhEnd;
		} else if (debugSetting8Value == 2) {
			pspLhEnd = bank.loreyHeights[primarySpeciesIndex][UC_ALL_INDEX];
		}

		float nonPrimaryLhAdjustment = fps.fcm.getCompVarAdjustments().getLoreyHeightOther();

		for (int i : lps.getIndices()) {
			if (i != primarySpeciesIndex && bank.basalAreas[i][UC_ALL_INDEX] > 0.0f) {
				if (! (dhEnd == dhStart && debugSetting8Value >= 1)) {
					float spLhEstimate1 = estimateNonPrimarySpeciesLoreyHeight(i, dhStart, pspLhStart);
					float spLhEstimate2 = estimateNonPrimarySpeciesLoreyHeight(i, dhEnd, pspLhEnd);

					float otherF = (bank.loreyHeights[i][UC_ALL_INDEX] - 1.3f) / (spLhEstimate1 - 1.3f);
					otherF = 1.0f + (otherF - 1.0f) * nonPrimaryLhAdjustment;
					bank.loreyHeights[i][UC_ALL_INDEX] = 1.3f + (spLhEstimate2 - 1.3f) * otherF;
				}
			}
		}
	}

	/**
	 * EMP050, method 1. Estimate the Lorey height of the primary species from the its dominant height and
	 * trees-per-hectare.
	 *
	 * @param dh     the dominantHeight (of the primary species)
	 * @param pspTph trees-per-hectare of the primary species
	 *
	 * @return as described
	 */
	private float estimatePrimarySpeciesLoreyHeight(float dh, float pspTph) {

		String primarySpeciesAlias = fps.getPrimaryLayerProcessingState().getPrimarySpeciesAlias();
		Region polygonRegion = fps.getPrimaryLayerProcessingState().getBecZone().getRegion();
		var coefficients = fps.fcm.getLoreyHeightPrimarySpeciesEquationP1Coefficients();

		float a0 = coefficients.get(primarySpeciesAlias, polygonRegion).getCoe(1);
		float a1 = coefficients.get(primarySpeciesAlias, polygonRegion).getCoe(2);
		float a2 = coefficients.get(primarySpeciesAlias, polygonRegion).getCoe(3);

		float hMult = a0 - a1 + a1 * FloatMath.exp(a2 * (pspTph - 100.0f));

		return 1.3f + (dh - 1.3f) * hMult;
	}

	/**
	 * EMP053 - function that returns an estimate of the Lorey height of a non-primary species of the polygon.
	 *
	 * @param speciesIndex   (non-primary) species index
	 * @param dh             (primary species) dominant height
	 * @param pspLoreyHeight primary species Lorey height
	 *
	 * @return as described
	 */
	private float estimateNonPrimarySpeciesLoreyHeight(int speciesIndex, float dh, float pspLoreyHeight) {
		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		float spLh;

		int primarySpeciesIndex = lps.getPrimarySpeciesIndex();
		String primarySpeciesAlias = bank.speciesNames[primarySpeciesIndex];
		String speciesAlias = bank.speciesNames[speciesIndex];
		Region region = lps.getBecZone().getRegion();

		var coefficients = fps.fcm.getLoreyHeightNonPrimaryCoefficients();

		var configuredLhCoefficients = coefficients.get(speciesAlias, primarySpeciesAlias, region);
		var lhCoefficients = configuredLhCoefficients.orElseGet(() -> NonprimaryHLCoefficients.getDefault());

		float a0 = lhCoefficients.getCoe(1);
		float a1 = lhCoefficients.getCoe(2);
		int equationIndex = lhCoefficients.getEquationIndex();

		if (equationIndex == 1) {
			spLh = 1.3f + a0 * (FloatMath.pow(dh - 1.3f, a1));
		} else if (equationIndex == 2) {
			spLh = 1.3f + a0 * (FloatMath.pow(pspLoreyHeight - 1.3f, a1));
		} else {
			throw new IllegalStateException(
					MessageFormat.format("Expecting equation index 1 or 2 but saw {0}", equationIndex)
			);
		}

		return spLh;
	}

	/**
	 * EMP117A - Calculate and return the change in quad-mean-diameter for the primary layer over the growth period.
	 *
	 * @param pspYabhStart   primary species years at breast height (age) at start of growth period
	 * @param baStart        layer basal area at start of growth period
	 * @param dhStart        layer dominant height (i.e., the height of the primary species) at start of growth period
	 * @param dqStart        layer quad-mean-diameter at start of growth period
	 * @param veteranBaStart veteran layer basal area at start of growth period
	 * @param veteranBaEnd   veteran layer basal area at end of growth period
	 * @param dhDelta        growth in dominant height during growth period
	 *
	 * @return growth in quad-mean-diameter for the growth period. This may be negative.
	 *
	 * @throws StandProcessingException
	 */
	float calculateQuadMeanDiameterDelta(
			float pspYabhStart, float baStart, float dhStart, float dqStart, Optional<Float> veteranBaStart,
			Optional<Float> veteranBaEnd, float dhDelta, Reference<Boolean> dqGrowthLimitApplied
	) throws StandProcessingException {

		var lps = fps.getPrimaryLayerProcessingState();
		var becZone = lps.getBecZone();

		float[] speciesProportionsByBasalArea = getSpeciesProportionsByBasalAreaAtStartOfYear();

		var dqYieldCoefficients = fps.fcm.getQuadMeanDiameterYieldCoefficients();
		var decayBecZoneAlias = becZone.getDecayBec().getAlias();
		Coefficients coefficientsWeightedBySpeciesAndDecayBec = Coefficients.empty(6, 0);
		for (int i = 0; i < 6; i++) {
			float sum = 0.0f;
			for (int speciesIndex : lps.getIndices()) {
				String speciesAlias = lps.getBank().speciesNames[speciesIndex];
				sum += dqYieldCoefficients.get(decayBecZoneAlias, speciesAlias).getCoe(i)
						* speciesProportionsByBasalArea[speciesIndex];
			}
			coefficientsWeightedBySpeciesAndDecayBec.setCoe(i, sum);
		}

		float dqUpperBound = growQuadraticMeanDiameterUpperBound();
		float dqLimit = Math.max(dqUpperBound, dqStart);

		int debugVariable2Value = fps.fcm.getDebugSettings().getValue(Vars.MAX_BREAST_HEIGHT_AGE_2);

		float dqYieldStart = fps.estimators.estimateQuadMeanDiameterYield(
				coefficientsWeightedBySpeciesAndDecayBec, debugVariable2Value, dhStart, pspYabhStart, veteranBaStart,
				dqLimit
		);

		float dhEnd = dhStart + dhDelta;
		float pspYabhEnd = pspYabhStart + 1.0f;

		float dqYieldEnd = fps.estimators.estimateQuadMeanDiameterYield(
				coefficientsWeightedBySpeciesAndDecayBec, debugVariable2Value, dhEnd, pspYabhEnd, veteranBaEnd, dqLimit
		);

		float dqYieldGrowth = dqYieldEnd - dqYieldStart;

		int debugSetting6Value = fps.fcm.getDebugSettings().getValue(ForwardDebugSettings.Vars.DQ_GROWTH_MODEL_6);

		var growthFaitDetails = fps.fcm.getQuadMeanDiameterGrowthFiatDetails().get(becZone.getRegion());

		Optional<Float> dqGrowthFiat = Optional.empty();
		if (debugSetting6Value != 1) {
			var convergenceCoefficient = growthFaitDetails.calculateCoefficient(pspYabhStart);

			float adjust = -convergenceCoefficient * (dqStart - dqYieldStart);
			dqGrowthFiat = Optional.of(dqYieldGrowth + adjust);
		}

		Optional<Float> dqGrowthEmpirical = Optional.empty();
		if (debugSetting6Value != 0) {
			dqGrowthEmpirical = Optional.of(
					calculateQuadMeanDiameterGrowthEmpirical(
							pspYabhStart, dhStart, baStart, dqStart, dhDelta, dqYieldStart, dqYieldEnd
					)
			);
		}

		float dqGrowth;

		switch (debugSetting6Value) {
		case 0:
			dqGrowth = dqGrowthFiat.orElseThrow();
			break;

		case 1:
			dqGrowth = dqGrowthEmpirical.orElseThrow();
			break;

		case 2: {
			float empiricalProportion = 1.0f;
			if (pspYabhStart >= growthFaitDetails.getMixedCoefficient(1)) {
				empiricalProportion = 0.0f;
			} else if (pspYabhStart > growthFaitDetails.getMixedCoefficient(0)) {
				float t1 = pspYabhStart - growthFaitDetails.getMixedCoefficient(0);
				float t2 = growthFaitDetails.getMixedCoefficient(1) - growthFaitDetails.getMixedCoefficient(0);
				float t3 = growthFaitDetails.getMixedCoefficient(2);
				empiricalProportion = 1.0f - FloatMath.pow(t1 / t2, t3);
			}
			dqGrowth = empiricalProportion * dqGrowthEmpirical.orElseThrow()
					+ (1.0f - empiricalProportion) * dqGrowthFiat.orElseThrow();
			break;
		}

		default:
			throw new IllegalStateException("debugSetting6Value of " + debugSetting6Value + " is not supported");
		}

		if (dqStart + dqGrowth < 7.6f) {
			dqGrowth = 7.6f - dqStart;
		}

		if (dqStart + dqGrowth > dqLimit - 0.001f) {
			dqGrowthLimitApplied.set(true);
			dqGrowth = Math.max(dqLimit - dqStart, 0.0f);
		} else {
			dqGrowthLimitApplied.set(false);
		}

		return dqGrowth;
	}

	/**
	 * EMP122. Function that returns the quad mean diameter growth quad-mean-diameter value computed using the empirical
	 * model.
	 *
	 * @param pspYabhStart primary species years at breast height at start of growth period
	 * @param dhStart      primary species dominant height at start of growth period
	 * @param baStart      basal area of primary layer at start of growth period
	 * @param dqStart      quad mean diameter at start of growth period
	 * @param hdGrowth     growth in dominant height
	 * @param dqYieldStart quad mean diameter yield at start of growth period
	 * @param dqYieldEnd   quad mean diameter yield at end of growth period
	 *
	 * @return the change in primary layer basal area from start to start + 1 year
	 */
	private float calculateQuadMeanDiameterGrowthEmpirical(
			float pspYabhStart, float dhStart, float baStart, float dqStart, float hdGrowth, float dqYieldStart,
			float dqYieldEnd
	) {
		// Compute the growth in quadratic mean diameter

		var dqGrowthEmpiricalCoefficients = fps.fcm.getQuadMeanDiameterGrowthEmpiricalCoefficients();

		Integer stratumNumber = fps.getPrimaryLayerProcessingState().getPrimarySpeciesStratumNumber();
		var firstSpeciesDqGrowthCoe = dqGrowthEmpiricalCoefficients.get(stratumNumber);

		float a0 = firstSpeciesDqGrowthCoe.get(0);
		float a1 = firstSpeciesDqGrowthCoe.get(1);
		float a2 = firstSpeciesDqGrowthCoe.get(2);
		float a3 = firstSpeciesDqGrowthCoe.get(3);
		float a4 = firstSpeciesDqGrowthCoe.get(4);
		float a5 = firstSpeciesDqGrowthCoe.get(5);
		float a6 = firstSpeciesDqGrowthCoe.get(6);

		pspYabhStart = Math.max(pspYabhStart, 1.0f);
		float dqYieldGrowth = dqYieldEnd - dqYieldStart;

		float dqDelta = FloatMath
				.exp(a0 + a2 * FloatMath.log(pspYabhStart) + a3 * dqStart + a4 * dhStart + a5 * baStart + a6 * hdGrowth)
				+ a1 * dqYieldGrowth;

		dqDelta = Math.max(dqDelta, 0.0f);

		// Compute min/max growth in quadratic mean diameter

		Map<Integer, Coefficients> quadMeanDiameterGrowthEmpiricalLimits = fps.fcm
				.getQuadMeanDiameterGrowthEmpiricalLimits();
		float[] dqDeltaLimits = new float[8];
		for (int i = 0; i < 8; i++) {
			dqDeltaLimits[i] = quadMeanDiameterGrowthEmpiricalLimits.get(stratumNumber).getCoe(i);
		}

		float x = dqStart - 7.5f;
		float xsq = x * x;

		var dqGrowthMin = Math
				.max(dqDeltaLimits[0] + dqDeltaLimits[1] * x + dqDeltaLimits[2] * xsq / 100.0f, dqDeltaLimits[6]);
		var dqGrowthMax = Math
				.min(dqDeltaLimits[3] + dqDeltaLimits[4] * x + dqDeltaLimits[5] * xsq / 100.0f, dqDeltaLimits[7]);

		dqGrowthMax = Math.max(dqGrowthMax, dqGrowthMin);

		// Apply the just-computed limits to the previously computed value.

		return FloatMath.clamp(dqDelta, dqGrowthMin, dqGrowthMax);
	}

	/**
	 * @return the species proportions at the start of the year for the current layer.
	 */
	private float[] getSpeciesProportionsByBasalAreaAtStartOfYear() {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		float[] speciesProportionsByBasalArea = new float[lps.getNSpecies() + 1];

		for (int i : lps.getIndices()) {
			speciesProportionsByBasalArea[i] = bank.basalAreas[i][UC_ALL_INDEX] / bank.basalAreas[0][UC_ALL_INDEX];
		}

		return speciesProportionsByBasalArea;
	}

	/**
	 * EMP111A - Basal area growth for the primary layer.
	 *
	 * @param pspYabhStart        primary species years-at-breast-height at the start of the year
	 * @param pspDhStart          primary species dominant height at start of year
	 * @param baStart             primary layer basal area at the start of the year
	 * @param veteranLayerBaStart veteran layer basal area at the start of the year
	 * @param dhDelta             primary layer during the year
	 *
	 * @return the growth in the basal area of the primary layer for the year
	 * @throws StandProcessingException in the event of an error
	 */
	float calculateBasalAreaDelta(
			float pspYabhStart, float pspDhStart, float baStart, Optional<Float> veteranLayerBaStart, float dhDelta
	) throws StandProcessingException {

		ForwardDebugSettings debugSettings = fps.fcm.getDebugSettings();
		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		float[] speciesProportionsByBasalArea = getSpeciesProportionsByBasalAreaAtStartOfYear();

		var baYieldCoefficients = fps.fcm.getBasalAreaYieldCoefficients();
		var becZoneAlias = fps.getPrimaryLayerProcessingState().getBecZone().getAlias();
		Coefficients estimateBasalAreaYieldCoefficients = Coefficients.empty(7, 0);
		for (int i = 0; i <= 6; i++) {
			float sum = 0.0f;
			for (int speciesIndex : lps.getIndices()) {
				String speciesAlias = bank.speciesNames[speciesIndex];
				sum += baYieldCoefficients.get(becZoneAlias, speciesAlias).getCoe(i)
						* speciesProportionsByBasalArea[speciesIndex];
			}
			estimateBasalAreaYieldCoefficients.setCoe(i, sum);
		}
		if (estimateBasalAreaYieldCoefficients.getCoe(5) > 0.0f) {
			estimateBasalAreaYieldCoefficients.setCoe(5, 0.0f);
		}

		// UPPERGEN( 1, BATOP98, DQTOP98)
		var baUpperBound = growBasalAreaUpperBound();

		boolean isFullOccupancy = true;
		int debugSetting2Value = fps.fcm.getDebugSettings().getValue(Vars.MAX_BREAST_HEIGHT_AGE_2);

		float baYieldStart = fps.estimators.estimateBaseAreaYield(
				estimateBasalAreaYieldCoefficients, debugSetting2Value, pspDhStart, pspYabhStart, veteranLayerBaStart,
				isFullOccupancy, baUpperBound
		);

		float pspDhEnd = pspDhStart + dhDelta;
		float pspYabhEnd = pspYabhStart + 1.0f;

		float baYieldEnd = fps.estimators.estimateBaseAreaYield(
				estimateBasalAreaYieldCoefficients, debugSetting2Value, pspDhEnd, pspYabhEnd, veteranLayerBaStart,
				isFullOccupancy, baUpperBound
		);

		var growthFaitDetails = fps.fcm.getBasalAreaGrowthFiatDetails()
				.get(fps.getPrimaryLayerProcessingState().getBecZone().getRegion());

		var convergenceCoefficient = growthFaitDetails.calculateCoefficient(pspYabhStart);

		float baGrowth = baYieldEnd - baYieldStart;
		var adjust = -convergenceCoefficient * (baStart - baYieldStart);
		baGrowth += adjust;

		// Special check at young ages.
		if (pspYabhStart < 40.0f && baStart > 5.0f * baYieldStart) {
			// This stand started MUCH faster than base yield model. We'll let it keep going like
			// this for a while.
			baGrowth = Math.min(baYieldStart / pspYabhStart, Math.min(0.5f, baGrowth));
		}

		int debugSetting3Value = debugSettings.getValue(ForwardDebugSettings.Vars.BASAL_AREA_GROWTH_MODEL_3);

		if (debugSetting3Value >= 1) {
			float baGrowthFiatModel = baGrowth;

			float baGrowthEmpiricalModel = calculateBasalAreaGrowthEmpirical(
					speciesProportionsByBasalArea, baStart, pspYabhStart, pspDhStart, baYieldStart, baYieldEnd
			);

			baGrowth = baGrowthEmpiricalModel;

			if (debugSetting3Value == 2) {
				float c = 1.0f;
				if (pspYabhStart >= growthFaitDetails.getMixedCoefficient(1)) {
					c = 0.0f;
				} else if (pspYabhStart > growthFaitDetails.getMixedCoefficient(0)) {
					float t1 = pspYabhStart - growthFaitDetails.getMixedCoefficient(0);
					float t2 = growthFaitDetails.getMixedCoefficient(1) - growthFaitDetails.getMixedCoefficient(0);
					float t3 = growthFaitDetails.getMixedCoefficient(2);
					c = 1.0f - FloatMath.pow(t1 / t2, t3);
				}
				baGrowth = c * baGrowthEmpiricalModel + (1.0f - c) * baGrowthFiatModel;
			}
		}

		baUpperBound = baUpperBound / EstimationMethods.EMPIRICAL_OCCUPANCY;
		var baLimit = Math.max(baUpperBound, baStart);

		// Enforce upper limit on growth
		if (baStart + baGrowth > baLimit) {
			baGrowth = Math.max(baLimit - baStart, 0.0f);
		}

		// Undocumented check to prevent negative growth that causes BA to go to less than
		// 1.0. It is doubtful that this condition will ever occur...
		if (baGrowth < 0.0f && baStart + baGrowth < 1.0f) {
			baGrowth = -baStart + 1.0f;
		}

		return baGrowth;
	}

	/**
	 * EMP121. Function that returns the basal area growth calculated using the empirical model.
	 *
	 * @param speciesBasalAreaProportions the proportion by basal area of each of the polygon's species
	 * @param baStart                     basal area of primary layer
	 * @param pspYabhStart                primary species years at breast height or more
	 * @param pspDhStart                  primary species dominant height
	 * @param baYieldStart                basal area yield at start of period
	 * @param baYieldEnd                  basal area yield at end of period
	 *
	 * @return the change in primary layer basal area from start to start + 1 year
	 */
	private float calculateBasalAreaGrowthEmpirical(
			float[] speciesBasalAreaProportions, float baStart, float pspYabhStart, float pspDhStart,
			float baYieldStart, float baYieldEnd
	) {

		pspYabhStart = Math.max(pspYabhStart, 1.0f);
		if (pspYabhStart > 999.0f) {
			pspYabhStart = 999.0f;
		}

		var basalAreaGrowthEmpiricalCoefficients = fps.fcm.getBasalAreaGrowthEmpiricalCoefficients();

		String becZoneAlias = fps.getPrimaryLayerProcessingState().getBecZone().getAlias();
		String firstSpecies = fps.fcm.getGenusDefinitionMap().getByIndex(1).getAlias();
		var firstSpeciesBaGrowthCoe = basalAreaGrowthEmpiricalCoefficients.get(becZoneAlias, firstSpecies);

		float b0 = firstSpeciesBaGrowthCoe.get(0);
		float b1 = firstSpeciesBaGrowthCoe.get(1);
		float b2 = firstSpeciesBaGrowthCoe.get(2);
		float b3 = firstSpeciesBaGrowthCoe.get(3);
		float b4 = 0.0f;
		float b5 = 0.0f;
		float b6 = firstSpeciesBaGrowthCoe.get(6);
		float b7 = firstSpeciesBaGrowthCoe.get(7);

		for (int i = 1; i <= fps.getPrimaryLayerProcessingState().getNSpecies(); i++) {
			String speciesAlias = fps.getPrimaryLayerProcessingState().getBank().speciesNames[i];
			var baGrowthCoe = basalAreaGrowthEmpiricalCoefficients.get(becZoneAlias, speciesAlias);
			b4 += speciesBasalAreaProportions[i] * baGrowthCoe.getCoe(4);
			b5 += speciesBasalAreaProportions[i] * baGrowthCoe.getCoe(5);
		}

		b4 = Math.max(b4, 0.0f);
		b5 = Math.min(b5, 0.0f);

		float term1;
		if (pspDhStart > b0) {
			term1 = 1.0f - FloatMath.exp(b1 * (pspDhStart - b0));
		} else {
			term1 = 0.0f;
		}

		float fLogIt = -0.05f * (pspYabhStart - 350.0f);
		float term2a = FloatMath.exp(fLogIt) / (1.0f + FloatMath.exp(fLogIt));
		float term2 = b2 * FloatMath.pow(pspDhStart / 20.0f, b3) * term2a;
		float term3 = b4 * FloatMath.exp(b5 * pspYabhStart);

		float term4;
		float basalAreaYieldDelta = baYieldEnd - baYieldStart;
		if (basalAreaYieldDelta > 0.0) {
			term4 = b6 * FloatMath.pow(basalAreaYieldDelta, b7);
		} else {
			term4 = 0.0f;
		}

		float basalAreaDelta = term1 * (term2 + term3) + term4;

		// An undocumented check to prevent negative growth that causes BA to go to
		// less than 1.0. It is doubtful that this condition will ever occur.
		if (basalAreaDelta < 0.0 && baStart + basalAreaDelta < 1.0f) {
			basalAreaDelta = -baStart + 1.0f;
		}

		return basalAreaDelta;
	}

	/**
	 * UPPERGEN(1, BATOP98, DQTOP98) for basal area
	 */
	private float growBasalAreaUpperBound() {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();

		int debugSetting4Value = fps.fcm.getDebugSettings()
				.getValue(ForwardDebugSettings.Vars.PER_SPECIES_AND_REGION_MAX_BREAST_HEIGHT_4);
		if (debugSetting4Value > 0) {
			var upperBoundsCoefficients = fps.fcm.getUpperBoundsCoefficients();
			Region region = lps.getBecZone().getRegion();
			int primarySpeciesIndex = lps.getPrimarySpeciesIndex();
			return upperBoundsCoefficients.get(region, lps.getBank().speciesNames[primarySpeciesIndex], 1);
		} else {
			var primarySpeciesGroupNumber = lps.getPrimarySpeciesGroupNumber();
			return fps.fcm.getUpperBounds().get(primarySpeciesGroupNumber).getCoe(UpperBoundsParser.BA_INDEX);
		}
	}

	/**
	 * UPPERGEN(1, BATOP98, DQTOP98) for quad-mean-diameter
	 */
	private float growQuadraticMeanDiameterUpperBound() {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();

		int debugSetting4Value = fps.fcm.getDebugSettings()
				.getValue(ForwardDebugSettings.Vars.PER_SPECIES_AND_REGION_MAX_BREAST_HEIGHT_4);
		if (debugSetting4Value > 0) {

			var upperBoundsCoefficients = fps.fcm.getUpperBoundsCoefficients();
			Region region = lps.getBecZone().getRegion();
			int primarySpeciesIndex = lps.getPrimarySpeciesIndex();
			return upperBoundsCoefficients.get(region, lps.getBank().speciesNames[primarySpeciesIndex], 2);
		} else {
			var primarySpeciesGroupNumber = fps.getPrimaryLayerProcessingState().getPrimarySpeciesGroupNumber();
			return fps.fcm.getUpperBounds().get(primarySpeciesGroupNumber).getCoe(UpperBoundsParser.DQ_INDEX);
		}
	}

	/**
	 * HDGROW - calculate growth in dominant height from the current dominant height and the parameters needed to do the
	 * computation
	 *
	 * @param spDhStart           dominant height at the start of the growth period
	 * @param siteCurveNumber     site curve number, used to find the site index equation
	 * @param siStart
	 * @param yearsToBreastHeight for the given species
	 *
	 * @return the difference in dominant height from the beginning to the end of the growth period
	 * @throws ProcessingException
	 */
	float calculateDominantHeightDelta(float spDhStart, int siteCurveNumber, float siStart, float yearsToBreastHeight)
			throws ProcessingException {

		SiteCurveAgeMaximum scAgeMaximums = fps.fcm.getMaximumAgeBySiteCurveNumber().get(siteCurveNumber);
		Region region = fps.getPrimaryLayerProcessingState().getBank().getBecZone().getRegion();

		if (siteCurveNumber == VdypEntity.MISSING_INTEGER_VALUE) {
			throw new ProcessingException("No SiteCurveNumber supplied");
		}

		var siteIndexEquation = SiteIndexEquation.getByIndex(siteCurveNumber);

		if (spDhStart <= 1.3) {
			throw new ProcessingException(
					MessageFormat.format("(current) DominantHeight {0} is out of range (must be above 1.3)", spDhStart)
			);
		}

		final SiteIndexAgeType ageType = SiteIndexAgeType.SI_AT_BREAST;

		double siStart_d = siStart;
		double dhStart_d = spDhStart;
		double yearsToBreastHeight_d = yearsToBreastHeight;

		double ageStart;
		try {
			ageStart = SiteTool
					.heightAndSiteIndexToAge(siteIndexEquation, dhStart_d, ageType, siStart_d, yearsToBreastHeight_d);
		} catch (CommonCalculatorException e) {
			throw new ProcessingException(
					MessageFormat.format(
							"Encountered exception when calling heightAndSiteIndexToAge({0}, {1}, {2}, {3}, {4})",
							siteIndexEquation, dhStart_d, ageType, siStart_d, yearsToBreastHeight_d
					), e
			);
		}

		if (ageStart <= 0.0d) {
			if (dhStart_d > siStart_d) {
				return 0.0f /* no growth */;
			} else {
				throw new ProcessingException(
						MessageFormat.format("currentBreastHeightAge value {0} must be positive", ageStart)
				);
			}
		}

		double ageEnd = ageStart + 1.0f;

		// If we are past the total age limit for site curve, assign no growth. If we are almost there, go
		// slightly past the limit (by .01 yr). Once there, we should stop growing. The TOTAL age limit was
		// stored so we must calculate a BH age limit first...

		float ageLimitInYears = scAgeMaximums.getAgeMaximum(region);

		float breastHeightAgeLimitInYears = 0.0f;
		if (ageLimitInYears > 0) {
			breastHeightAgeLimitInYears = ageLimitInYears - yearsToBreastHeight;
		}

		if (ageStart <= breastHeightAgeLimitInYears || scAgeMaximums.getT1() <= 0.0f) {

			float yearPart = 1.0f;

			if (scAgeMaximums.getT1() <= 0.0f) {

				if (breastHeightAgeLimitInYears > 0.0f && ageEnd > breastHeightAgeLimitInYears) {
					if (ageStart > breastHeightAgeLimitInYears) {
						return 0.0f /* no growth */;
					}

					yearPart = (float) (breastHeightAgeLimitInYears - ageStart + 0.01);
					ageEnd = ageStart + yearPart;
				}
			}

			// The above code to find ages allows errors up to .005 m. At high ages with some
			// species this can correspond to a half year. Therefore, AGED1 can not be assumed to
			// correspond to HDD1. Find a new HDD1 to at least get the increment correct.

			double currentDominantHeight = ageAndSiteIndexToHeight(
					siteIndexEquation, ageStart, ageType, siStart_d, yearsToBreastHeight_d
			);

			double nextDominantHeight = ageAndSiteIndexToHeight(
					siteIndexEquation, ageEnd, ageType, siStart_d, yearsToBreastHeight_d, r -> r >= 0.0
			);

			if (nextDominantHeight < currentDominantHeight && yearPart == 1.0) {
				// Rounding error in site routines?
				if (Math.abs(currentDominantHeight - nextDominantHeight) < 0.01) {
					return 0.0f /* no growth */;
				} else {
					throw new ProcessingException(
							MessageFormat.format(
									"New dominant height {0} is less than the current dominant height {1}",
									nextDominantHeight, currentDominantHeight
							)
					);
				}
			}

			return (float) (nextDominantHeight - currentDominantHeight);

		} else {
			// We are in a special extension of the curve. Derive the new curve form and then
			// compute the answer.

			double breastHeightAgeLimitInYears_d = breastHeightAgeLimitInYears;

			double currentDominantHeight = ageAndSiteIndexToHeight(
					siteIndexEquation, breastHeightAgeLimitInYears_d, ageType, siStart_d, yearsToBreastHeight_d
			);

			breastHeightAgeLimitInYears_d += 1.0;

			double nextDominantHeight = ageAndSiteIndexToHeight(
					siteIndexEquation, breastHeightAgeLimitInYears_d, ageType, siStart_d, yearsToBreastHeight_d
			);

			float rate = (float) (nextDominantHeight - currentDominantHeight);
			if (rate < 0.0005f) {
				rate = 0.0005f;
			}

			float a = FloatMath.log(0.5f) / scAgeMaximums.getT1();
			float y = (float) currentDominantHeight;
			// Model is:
			// Y = y - rate/a * (1 - exp(a * t)) where t = AGE - BHAGELIM
			// Solve for t:
			// 1 - exp(a * t) = (y - dominantHeight) * a/rate
			// -exp(a * t) = (y - dominantHeight) * a/rate - 1
			// exp(a * t) = (dominantHeight - y) * a/rate + 1
			// a * t = ln(1 + (dominantHeight - y) * a/rate)
			// t = ln(1 + (dominantHeight - y) * a/rate) / a
			float t;
			if (spDhStart > y) {
				float term = 1.0f + (spDhStart - y) * a / rate;
				if (term <= 1.0e-7) {
					return 0.0f;
				}
				t = FloatMath.log(term) / a;
			} else {
				t = 0.0f;
			}

			if (t > scAgeMaximums.getT2()) {
				return 0.0f;
			} else {
				return rate / a * (-FloatMath.exp(a * t) + FloatMath.exp(a * (t + 1.0f)));
			}
		}
	}

	private static double ageAndSiteIndexToHeight(
			SiteIndexEquation curve, double age, SiteIndexAgeType ageType, double siteIndex, double years2BreastHeight,
			Function<Double, Boolean> checkResultValidity
	) throws ProcessingException {
		Double r = ageAndSiteIndexToHeight(curve, age, ageType, siteIndex, years2BreastHeight);
		if (!checkResultValidity.apply(r)) {
			throw new ProcessingException(
					MessageFormat.format(
							"SiteTool.ageAndSiteIndexToHeight({0}, {1}, {2}, {3}, {4}) returned {5}", curve, age,
							ageType, siteIndex, years2BreastHeight, r
					)
			);
		}

		return r;
	}

	private static double ageAndSiteIndexToHeight(
			SiteIndexEquation curve, double age, SiteIndexAgeType ageType, double siteIndex, double years2BreastHeight
	) throws ProcessingException {
		try {
			return SiteTool.ageAndSiteIndexToHeight(curve, age, ageType, siteIndex, years2BreastHeight);
		} catch (CommonCalculatorException e) {
			throw new ProcessingException(
					MessageFormat.format(
							"SiteTool.ageAndSiteIndexToHeight({0}, {1}, {2}, {3}, {4}) threw exception", curve, age,
							ageType, siteIndex, years2BreastHeight
					), e
			);
		}
	}

	@SuppressWarnings("java:S3776")
	private void writeCurrentPolygon(int startYear, int currentYear, int endYear) throws ProcessingException {

		try {
			outputWriter.ifPresent((o) -> {
				var polygon = fps.updatePolygon();

				logger.info("Writing polygon {} for year {}", polygon, currentYear);

				int controlVariable4Value = fps.fcm.getForwardControlVariables()
						.getControlVariable(ControlVariable.OUTPUT_FILES_4);

				switch (controlVariable4Value) {
				case 0: {
					/* never write output */
					return;
				}
				case 1: {
					/* write only first growth period */
					if (currentYear != startYear) {
						return;
					}
				}
				case 2: {
					/* write only first and last growth periods */
					if (currentYear != startYear && currentYear != endYear) {
						return;
					}
				}
				case 4: {
					/* write only the first, every tenth subsequent, and the last periods */
					if ( (currentYear - startYear) % 10 != 0 && currentYear != endYear) {
						return;
					}
				}
				case 3: {
					break;
				}
				default:
					throw new RuntimeProcessingException(
							new ProcessingException(
									MessageFormat
											.format("Invalid value for control variable 4: {0}", controlVariable4Value)
							)
					);
				}

				try {
					o.writePolygonWithSpeciesAndUtilizationForYear(polygon, currentYear);
				} catch (IOException e) {
					throw new RuntimeProcessingException(new ProcessingException(e));
				}
			});
		} catch (RuntimeProcessingException e) {
			throw e.getCause();
		}
	}

	private static final float[] DEFAULT_QUAD_MEAN_DIAMETERS = new float[] { Float.NaN, 10.0f, 15.0f, 20.0f, 25.0f };
	private static final float V_BASE_MIN = 0.1f;
	private static final float B_BASE_MIN = 0.01f;

	/**
	 * CVSET1 - computes cvVolume, cvBasalArea, cvQuadraticMeanDiameter and cvSmall and assigns them to the current
	 * LayerProcessingState.
	 *
	 * @throws ProcessingException
	 */
	@SuppressWarnings("unchecked")
	void setCompatibilityVariables() throws ProcessingException {

		Coefficients aAdjust = new Coefficients(new float[] { 0.0f, 0.0f, 0.0f, 0.0f }, 1);

		var growthDetails = fps.fcm.getForwardControlVariables();
		var lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		// Note: L1COM2 (INL1VGRP, INL1DGRP, INL1BGRP) is initialized when
		// PolygonProcessingState (volumeEquationGroups, decayEquationGroups
		// breakageEquationGroups, respectively) is constructed. Copying
		// the values into LCOM1 is not necessary. Note, however, that
		// VolumeEquationGroup 10 is mapped to 11 (VGRPFIND) - this is done
		// when volumeEquationGroups is built (i.e., when the equivalent to
		// INL1VGRP is built, rather than when LCOM1 VGRPL is built in the
		// original code.)

		var cvVolume = new MatrixMap3[lps.getNSpecies() + 1];
		var cvBasalArea = new MatrixMap2[lps.getNSpecies() + 1];
		var cvQuadraticMeanDiameter = new MatrixMap2[lps.getNSpecies() + 1];
		var cvSmall = new HashMap[lps.getNSpecies() + 1];

		for (int s : lps.getIndices()) {

			String genusName = bank.speciesNames[s];

			float spLoreyHeight_All = bank.loreyHeights[s][UtilizationClass.ALL.ordinal()];

			UtilizationVector basalAreas = Utils.utilizationVector();
			UtilizationVector wholeStemVolumes = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumes = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumesNetOfDecay = Utils.utilizationVector();
			UtilizationVector closeUtilizationVolumesNetOfDecayAndWaste = Utils.utilizationVector();
			UtilizationVector quadMeanDiameters = Utils.utilizationVector();
			UtilizationVector treesPerHectare = Utils.utilizationVector();

			cvVolume[s] = new MatrixMap3Impl<UtilizationClass, VolumeVariable, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, VolumeVariable.ALL, LayerType.ALL_USED, (k1, k2, k3) -> 0f
			);
			cvBasalArea[s] = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0f
			);
			cvQuadraticMeanDiameter[s] = new MatrixMap2Impl<UtilizationClass, LayerType, Float>(
					UtilizationClass.UTIL_CLASSES, LayerType.ALL_USED, (k1, k2) -> 0f
			);

			for (UtilizationClass uc : UtilizationClass.ALL_BUT_SMALL) {

				basalAreas.setCoe(uc.index, bank.basalAreas[s][uc.ordinal()]);
				wholeStemVolumes.setCoe(uc.index, bank.wholeStemVolumes[s][uc.ordinal()]);
				closeUtilizationVolumes.setCoe(uc.index, bank.closeUtilizationVolumes[s][uc.ordinal()]);
				closeUtilizationVolumesNetOfDecay.setCoe(uc.index, bank.cuVolumesMinusDecay[s][uc.ordinal()]);
				closeUtilizationVolumesNetOfDecayAndWaste
						.setCoe(uc.index, bank.cuVolumesMinusDecayAndWastage[s][uc.ordinal()]);

				quadMeanDiameters.setCoe(uc.index, bank.quadMeanDiameters[s][uc.ordinal()]);
				if (uc != UtilizationClass.ALL && quadMeanDiameters.getCoe(uc.index) <= 0.0f) {
					quadMeanDiameters.setCoe(uc.index, DEFAULT_QUAD_MEAN_DIAMETERS[uc.ordinal()]);
				}
			}

			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {

				float adjustment;
				float baseVolume;

				// Volume less decay and waste
				adjustment = 0.0f;
				baseVolume = bank.cuVolumesMinusDecay[s][uc.ordinal()];

				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {

					// EMP094
					fps.estimators.estimateNetDecayAndWasteVolume(
							lps.getBecZone().getRegion(), uc, aAdjust, bank.speciesNames[s], spLoreyHeight_All,
							quadMeanDiameters, closeUtilizationVolumes, closeUtilizationVolumesNetOfDecay,
							closeUtilizationVolumesNetOfDecayAndWaste
					);

					float actualVolume = bank.cuVolumesMinusDecayAndWastage[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumesNetOfDecayAndWaste.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}

				cvVolume[s]
						.put(uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY, adjustment);

				// Volume less decay
				adjustment = 0.0f;
				baseVolume = bank.closeUtilizationVolumes[s][uc.ordinal()];

				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {

					// EMP093
					int decayGroup = lps.getDecayEquationGroups()[s];
					fps.estimators.estimateNetDecayVolume(
							bank.speciesNames[s], lps.getBecZone().getRegion(), uc, aAdjust, decayGroup,
							lps.getPrimarySpeciesAgeAtBreastHeight(), quadMeanDiameters, closeUtilizationVolumes,
							closeUtilizationVolumesNetOfDecay
					);

					float actualVolume = bank.cuVolumesMinusDecay[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumesNetOfDecay.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}

				cvVolume[s].put(uc, VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY, adjustment);

				// Volume
				adjustment = 0.0f;
				baseVolume = bank.wholeStemVolumes[s][uc.ordinal()];

				if (growthDetails.allowCalculation(baseVolume, V_BASE_MIN, (l, r) -> l > r)) {

					// EMP092
					int volumeGroup = lps.getVolumeEquationGroups()[s];
					fps.estimators.estimateCloseUtilizationVolume(
							uc, aAdjust, volumeGroup, spLoreyHeight_All, quadMeanDiameters, wholeStemVolumes,
							closeUtilizationVolumes
					);

					float actualVolume = bank.closeUtilizationVolumes[s][uc.ordinal()];
					float staticVolume = closeUtilizationVolumes.getCoe(uc.index);
					adjustment = calculateCompatibilityVariable(actualVolume, baseVolume, staticVolume);
				}

				cvVolume[s].put(uc, VolumeVariable.CLOSE_UTIL_VOL, LayerType.PRIMARY, adjustment);
			}

			int primarySpeciesVolumeGroup = lps.getVolumeEquationGroups()[s];
			float primarySpeciesQMDAll = bank.quadMeanDiameters[s][UC_ALL_INDEX];
			var wholeStemVolume = bank.treesPerHectare[s][UC_ALL_INDEX] * fps.estimators
					.estimateWholeStemVolumePerTree(primarySpeciesVolumeGroup, spLoreyHeight_All, primarySpeciesQMDAll);

			wholeStemVolumes.setCoe(UC_ALL_INDEX, wholeStemVolume);

			fps.estimators.estimateWholeStemVolume(
					UtilizationClass.ALL, 0.0f, primarySpeciesVolumeGroup, spLoreyHeight_All, quadMeanDiameters,
					basalAreas, wholeStemVolumes
			);

			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				float adjustment = 0.0f;
				float basalArea = basalAreas.getCoe(uc.index);
				if (growthDetails.allowCalculation(basalArea, B_BASE_MIN, (l, r) -> l > r)) {
					adjustment = calculateWholeStemVolume(
							bank.wholeStemVolumes[s][uc.ordinal()], basalArea, wholeStemVolumes.getCoe(uc.index)
					);
				}

				cvVolume[s].put(uc, VolumeVariable.WHOLE_STEM_VOL, LayerType.PRIMARY, adjustment);
			}

			fps.estimators.estimateQuadMeanDiameterByUtilization(lps.getBecZone(), quadMeanDiameters, genusName);

			fps.estimators.estimateBaseAreaByUtilization(lps.getBecZone(), quadMeanDiameters, basalAreas, genusName);

			// Calculate trees-per-hectare per utilization
			treesPerHectare.setCoe(UtilizationClass.ALL.index, bank.treesPerHectare[s][UC_ALL_INDEX]);
			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				treesPerHectare.setCoe(
						uc.index,
						BaseAreaTreeDensityDiameter
								.treesPerHectare(basalAreas.getCoe(uc.index), quadMeanDiameters.getCoe(uc.index))
				);
			}

			ReconcilationMethods.reconcileComponents(basalAreas, treesPerHectare, quadMeanDiameters);

			for (UtilizationClass uc : UtilizationClass.UTIL_CLASSES) {
				float baCvValue = bank.basalAreas[s][uc.ordinal()] - basalAreas.getCoe(uc.index);
				cvBasalArea[s].put(uc, LayerType.PRIMARY, baCvValue);

				float originalQmd = bank.quadMeanDiameters[s][uc.ordinal()];
				float adjustedQmd = quadMeanDiameters.getCoe(uc.index);

				float qmdCvValue;
				if (growthDetails.allowCalculation(() -> originalQmd < B_BASE_MIN)) {
					qmdCvValue = 0.0f;
				} else if (originalQmd > 0 && adjustedQmd > 0) {
					qmdCvValue = originalQmd - adjustedQmd;
				} else {
					qmdCvValue = 0.0f;
				}

				cvQuadraticMeanDiameter[s].put(uc, LayerType.PRIMARY, qmdCvValue);
			}

			// Small components

			cvSmall[s] = calculateSmallCompatibilityVariables(s, growthDetails);
		}

		lps.setCompatibilityVariableDetails(cvVolume, cvBasalArea, cvQuadraticMeanDiameter, cvSmall);
	}

	/**
	 * Function that calculates values for the small component compatibility variables and returns the result.
	 *
	 * @param speciesIndex            the index of the species for which this operation is to be performed
	 * @param forwardControlVariables the control variables for this run
	 *
	 * @throws ProcessingException
	 */
	private HashMap<UtilizationClassVariable, Float>
			calculateSmallCompatibilityVariables(int speciesIndex, ForwardControlVariables forwardControlVariables)
					throws ProcessingException {

		var lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		Region region = lps.getBecZone().getRegion();
		String speciesName = bank.speciesNames[speciesIndex];

		float spLoreyHeight_All = bank.loreyHeights[speciesIndex][UC_ALL_INDEX]; // HLsp
		float spQuadMeanDiameter_All = bank.quadMeanDiameters[speciesIndex][UC_ALL_INDEX]; // DQsp

		// this WHOLE operation on Actual BA's, not 100% occupancy.
		// TODO: verify this: float fractionAvailable = polygon.getPercentForestLand();
		float spBaseArea_All = bank.basalAreas[speciesIndex][UC_ALL_INDEX] /* * fractionAvailable */; // BAsp

		// EMP080
		float smallProbability = smallComponentProbability(speciesName, spLoreyHeight_All, region); // PROBsp

		// EMP081
		float conditionalExpectedBaseArea = calculateSmallComponentConditionalExpectedBasalArea(
				speciesName, spBaseArea_All, spLoreyHeight_All, region
		); // BACONDsp

		// TODO (see previous TODO): conditionalExpectedBaseArea /= fractionAvailable;

		float spBaSmall = smallProbability * conditionalExpectedBaseArea;

		// EMP082
		float spDqSmall = smallComponentQuadMeanDiameter(speciesName, spLoreyHeight_All); // DQSMsp

		// EMP085
		float spLhSmall = smallComponentLoreyHeight(speciesName, spLoreyHeight_All, spDqSmall, spQuadMeanDiameter_All); // HLSMsp

		// EMP086
		float spMeanVolumeSmall = meanVolumeSmall(speciesName, spLhSmall, spDqSmall); // VMEANSMs

		var spCvSmall = new HashMap<UtilizationClassVariable, Float>();

		float spInputBasalArea_Small = bank.basalAreas[speciesIndex][UC_SMALL_INDEX];
		spCvSmall.put(UtilizationClassVariable.BASAL_AREA, spInputBasalArea_Small - spBaSmall);

		if (forwardControlVariables.allowCalculation(spInputBasalArea_Small, B_BASE_MIN, (l, r) -> l > r)) {
			float spInputQuadMeanDiameter_Small = bank.quadMeanDiameters[speciesIndex][UC_SMALL_INDEX];
			spCvSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, spInputQuadMeanDiameter_Small - spDqSmall);
		} else {
			spCvSmall.put(UtilizationClassVariable.QUAD_MEAN_DIAMETER, 0.0f);
		}

		float spInputLoreyHeight_Small = bank.loreyHeights[speciesIndex][UC_SMALL_INDEX];
		if (spInputLoreyHeight_Small > 1.3f && spLhSmall > 1.3f && spInputBasalArea_Small > 0.0f) {
			float cvLoreyHeight = FloatMath.log( (spInputLoreyHeight_Small - 1.3f) / (spLhSmall - 1.3f));
			spCvSmall.put(UtilizationClassVariable.LOREY_HEIGHT, cvLoreyHeight);
		} else {
			spCvSmall.put(UtilizationClassVariable.LOREY_HEIGHT, 0.0f);
		}

		float spInputWholeStemVolume_Small = bank.wholeStemVolumes[speciesIndex][UC_SMALL_INDEX];
		if (spInputWholeStemVolume_Small > 0.0f && spMeanVolumeSmall > 0.0f
				&& forwardControlVariables.allowCalculation(spInputBasalArea_Small, B_BASE_MIN, (l, r) -> l >= r)) {

			float spInputTreePerHectare_Small = bank.treesPerHectare[speciesIndex][UC_SMALL_INDEX];

			var spWsVolumeSmall = FloatMath
					.log(spInputWholeStemVolume_Small / spInputTreePerHectare_Small / spMeanVolumeSmall);
			spCvSmall.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, spWsVolumeSmall);

		} else {
			spCvSmall.put(UtilizationClassVariable.WHOLE_STEM_VOLUME, 0.0f);
		}

		return spCvSmall;
	}

	/**
	 * EMP080 - calculate the small component probability of the species with <code>speciesAlias</code>, the given Lorey
	 * height and in the given <code>region</code>.
	 *
	 * @param speciesAlias the species' alias
	 * @param loreyHeight  current Lorey height of the stand
	 * @param region       the stand's region
	 * @return as described
	 */
	private float smallComponentProbability(String speciesAlias, float loreyHeight, Region region) {
		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();

		Coefficients coe = fps.fcm.getSmallComponentProbabilityCoefficients().get(speciesAlias);

		// EQN 1 in IPSJF118.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);
		float a2 = coe.getCoe(3);
		float a3 = coe.getCoe(4);

		a1 = (region == Region.COASTAL) ? a1 : 0.0f;

		float logit = a0 + //
				a1 + //
				a2 * lps.getBank().yearsAtBreastHeight[lps.getPrimarySpeciesIndex()] + //
				a3 * loreyHeight;

		float result = exp(logit) / (1.0f + exp(logit));

		return result;
	}

	/**
	 * EMP081 - calculate the conditional expected small component basal area. See IPSJF118.doc, equation 3.
	 *
	 * @param sp0Name the species group name
	 * @param spBaEnd the species UC 0 (all) basal area
	 * @param spLhEnd the species UC 0 (all) Lorey height
	 * @param region  the region in which the calculation is occurring
	 * @return as described
	 */
	private float calculateSmallComponentConditionalExpectedBasalArea(
			String sp0Name, float spBaEnd, float spLhEnd, Region region
	) {
		Coefficients coe = fps.fcm.getSmallComponentBasalAreaCoefficients().get(sp0Name);

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);
		float a2 = coe.getCoe(3);
		float a3 = coe.getCoe(4);

		float regionMultiplier = region == Region.COASTAL ? 1.0f : 0.0f;

		// FIXME due to a bug in VDYP7 it always treats this as interior. Replicating
		// that for now.
		regionMultiplier = 0f;

		float result = (a0 + a1 * regionMultiplier + a2 * spBaEnd) * exp(a3 * spLhEnd);
		result = max(result, 0f);

		return result;
	}

	// EMP082
	private float smallComponentQuadMeanDiameter(String speciesName, float loreyHeight) {
		Coefficients coe = fps.fcm.getSmallComponentQuadMeanDiameterCoefficients().get(speciesName);

		// EQN 5 in IPSJF118.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);

		float logit = a0 + a1 * loreyHeight;

		return 4.0f + 3.5f * exp(logit) / (1.0f + exp(logit));
	}

	// EMP085
	private float smallComponentLoreyHeight(
			String speciesName, float speciesLoreyHeight_All, float quadMeanDiameterSpecSmall,
			float speciesQuadMeanDiameter_All
	) {
		Coefficients coe = fps.fcm.getSmallComponentLoreyHeightCoefficients().get(speciesName);

		// EQN 1 in IPSJF119.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);

		var result = 1.3f + (speciesLoreyHeight_All - 1.3f) //
				* exp(a0 * (pow(quadMeanDiameterSpecSmall, a1) - pow(speciesQuadMeanDiameter_All, a1)));

		return result;
	}

	// EMP086
	private float meanVolumeSmall(String speciesName, float spHlSmall, float spDqSmall) {
		Coefficients coe = fps.fcm.getSmallComponentWholeStemVolumeCoefficients().get(speciesName);

		// EQN 1 in IPSJF119.doc

		float a0 = coe.getCoe(1);
		float a1 = coe.getCoe(2);
		float a2 = coe.getCoe(3);
		float a3 = coe.getCoe(4);

		return exp(a0 + a1 * log(spDqSmall) + a2 * log(spHlSmall) + a3 * spDqSmall);
	}

	private static float calculateCompatibilityVariable(float actualVolume, float baseVolume, float staticVolume) {

		float staticRatio = staticVolume / baseVolume;
		float staticLogit;
		if (staticRatio <= 0.0f) {
			staticLogit = -7.0f;
		} else if (staticRatio >= 1.0f) {
			staticLogit = 7.0f;
		} else {
			staticLogit = clamp(log(staticRatio / (1.0f - staticRatio)), -7.0f, 7.0f);
		}

		float actualRatio = actualVolume / baseVolume;
		float actualLogit;
		if (actualRatio <= 0.0f) {
			actualLogit = -7.0f;
		} else if (actualRatio >= 1.0f) {
			actualLogit = 7.0f;
		} else {
			actualLogit = clamp(log(actualRatio / (1.0f - actualRatio)), -7.0f, 7.0f);
		}

		return actualLogit - staticLogit;
	}

	private static float calculateWholeStemVolume(float actualVolume, float basalArea, float staticVolume) {

		float staticRatio = staticVolume / basalArea;
		float staticLogit;
		if (staticRatio <= 0.0f) {
			staticLogit = -2.0f;
		} else {
			staticLogit = log(staticRatio);
		}

		float actualRatio = actualVolume / basalArea;
		float actualLogit;
		if (actualRatio <= 0.0f) {
			actualLogit = -2.0f;
		} else {
			actualLogit = log(actualRatio);
		}

		return actualLogit - staticLogit;
	}

	/**
	 * VHDOM1 METH_H = 2, METH_A = 2, METH_SI = 2.
	 *
	 * @param lps             layer processing state
	 * @param hl1Coefficients the configured dominant height recalculation coefficients
	 *
	 * @throws ProcessingException
	 */
	static void calculateDominantHeightAgeSiteIndex(
			LayerProcessingState lps, MatrixMap2<String, Region, Coefficients> hl1Coefficients
	) throws ProcessingException {

		Bank bank = lps.getBank();

		// Calculate primary species values
		int primarySpeciesIndex = lps.getPrimarySpeciesIndex();

		// (1) Dominant Height
		float primarySpeciesDominantHeight = bank.dominantHeights[primarySpeciesIndex];
		if (Float.isNaN(primarySpeciesDominantHeight)) {
			float loreyHeight = bank.loreyHeights[primarySpeciesIndex][UC_ALL_INDEX];
			if (Float.isNaN(loreyHeight)) {
				throw new ProcessingException(
						MessageFormat.format(
								"Neither dominant nor lorey height[All] is available for primary species {}",
								bank.speciesNames[primarySpeciesIndex]
						), 2
				);
			}

			// Estimate dominant height from the lorey height
			String primarySpeciesAlias = bank.speciesNames[primarySpeciesIndex];
			Region primarySpeciesRegion = lps.getBecZone().getRegion();

			var coefficients = hl1Coefficients.get(primarySpeciesAlias, primarySpeciesRegion);
			float a0 = coefficients.getCoe(1);
			float a1 = coefficients.getCoe(2);
			float a2 = coefficients.getCoe(3);

			float treesPerHectare = bank.treesPerHectare[primarySpeciesIndex][UC_ALL_INDEX];
			float hMult = a0 - a1 + a1 * FloatMath.exp(a2 * (treesPerHectare - 100.0f));

			primarySpeciesDominantHeight = 1.3f + (loreyHeight - 1.3f) / hMult;
		}

		// (2) Age (total, years at breast height, years to breast height
		float primarySpeciesTotalAge = bank.ageTotals[primarySpeciesIndex];
		float primarySpeciesYearsAtBreastHeight = bank.yearsAtBreastHeight[primarySpeciesIndex];
		float primarySpeciesYearsToBreastHeight = bank.yearsToBreastHeight[primarySpeciesIndex];

		Optional<Integer> activeIndex = Optional.empty();

		if (Float.isNaN(primarySpeciesTotalAge)) {

			if (lps.hasSecondarySpeciesIndex() && !Float.isNaN(bank.ageTotals[lps.getSecondarySpeciesIndex()])) {
				activeIndex = Optional.of(lps.getSecondarySpeciesIndex());
			} else {
				for (int i = 1; i <= lps.getNSpecies(); i++) {
					if (!Float.isNaN(bank.ageTotals[i])) {
						activeIndex = Optional.of(i);
						break;
					}
				}
			}

			activeIndex.orElseThrow(() -> new ProcessingException("Age data unavailable for ALL species", 5));

			primarySpeciesTotalAge = bank.ageTotals[activeIndex.get()];
			if (!Float.isNaN(primarySpeciesYearsToBreastHeight)) {
				primarySpeciesYearsAtBreastHeight = primarySpeciesTotalAge - primarySpeciesYearsToBreastHeight;
			} else if (!Float.isNaN(primarySpeciesYearsAtBreastHeight)) {
				primarySpeciesYearsToBreastHeight = primarySpeciesTotalAge - primarySpeciesYearsAtBreastHeight;
			} else {
				primarySpeciesYearsAtBreastHeight = bank.yearsAtBreastHeight[activeIndex.get()];
				primarySpeciesYearsToBreastHeight = bank.yearsToBreastHeight[activeIndex.get()];
			}
		}

		// (3) Site Index
		float primarySpeciesSiteIndex = bank.siteIndices[primarySpeciesIndex];
		if (Float.isNaN(primarySpeciesSiteIndex)) {

			if (lps.hasSecondarySpeciesIndex() && !Float.isNaN(bank.siteIndices[lps.getSecondarySpeciesIndex()])) {
				activeIndex = Optional.of(lps.getSecondarySpeciesIndex());
			} else {
				if (activeIndex.isEmpty() || Float.isNaN(bank.siteIndices[activeIndex.get()])) {
					for (int i = 1; i <= lps.getNSpecies(); i++) {
						if (!Float.isNaN(bank.siteIndices[i])) {
							activeIndex = Optional.of(i);
							break;
						}
					}
				}
			}
			primarySpeciesSiteIndex = bank.siteIndices[activeIndex
					.orElseThrow(() -> new ProcessingException("Site Index data unavailable for ALL species", 7))];
		} else {
			activeIndex = Optional.of(primarySpeciesIndex);
		}

		SiteIndexEquation siteCurve1 = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(activeIndex.get()));
		SiteIndexEquation siteCurve2 = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(0));

		try {
			double newSI = SiteTool.convertSiteIndexBetweenCurves(siteCurve1, primarySpeciesSiteIndex, siteCurve2);
			if (newSI > 1.3) {
				primarySpeciesSiteIndex = (float) newSI;
			}
		} catch (CommonCalculatorException e) {
			// do nothing. primarySpeciesSiteIndex will not be modified.
		}

		lps.setPrimarySpeciesDetails(
				new PrimarySpeciesDetails(
						primarySpeciesDominantHeight, primarySpeciesSiteIndex, primarySpeciesTotalAge,
						primarySpeciesYearsAtBreastHeight, primarySpeciesYearsToBreastHeight
				)
		);
	}

	/**
	 * For each species for which a years-to-breast-height value was not supplied, calculate it from the given
	 * years-at-breast-height and age-total values if given or otherwise estimate it from the site curve and site index
	 * values for the species.
	 *
	 * @param lps the current state of the processing of the polygon
	 */
	static void estimateMissingYearsToBreastHeightValues(LayerProcessingState lps) {

		Bank bank = lps.getBank();

		int primarySpeciesIndex = lps.getPrimarySpeciesIndex();
		float primarySpeciesSiteIndex = bank.siteIndices[primarySpeciesIndex];

		// Determine the default site index by using the site index of the primary species unless
		// it hasn't been set in which case pick any. Note that there may still not be a
		// meaningful value after this for example when the value is not available for the primary
		// species (see estimateMissingSiteIndices) and it's the only one.

		float defaultSiteIndex = primarySpeciesSiteIndex;

		if (Float.isNaN(defaultSiteIndex)) {
			for (int i : lps.getIndices()) {
				if (!Float.isNaN(bank.siteIndices[i])) {
					defaultSiteIndex = bank.siteIndices[i];
					break;
				}
			}
		}

		for (int i : lps.getIndices()) {
			if (!Float.isNaN(bank.yearsToBreastHeight[i])) {
				// was supplied
				continue;
			}

			// Note: this block will normally never be executed because of the logic in
			// the constructor of VdypLayerSpecies that computes missing values when the
			// other two measurement values are present.
			if (!Float.isNaN(bank.yearsAtBreastHeight[i]) && bank.ageTotals[i] > bank.yearsAtBreastHeight[i]) {
				bank.yearsToBreastHeight[i] = bank.ageTotals[i] - bank.yearsAtBreastHeight[i];
				continue;
			}

			float siteIndex = !Float.isNaN(bank.siteIndices[i]) ? bank.siteIndices[i] : defaultSiteIndex;
			try {
				SiteIndexEquation curve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(i));
				double yearsToBreastHeight = SiteTool.yearsToBreastHeight(curve, siteIndex);
				bank.yearsToBreastHeight[i] = (float) yearsToBreastHeight;
			} catch (CommonCalculatorException e) {
				logger.warn(MessageFormat.format("Unable to determine yearsToBreastHeight of species {0}", i), e);
			}
		}
	}

	/**
	 * SITEADD
	 * <p>
	 * (1) If the site index of the primary species has not been set, calculate it as the average of the site indices of
	 * the other species that -do- have one, after converting each between the site curve of the other species and that
	 * of the primary species.
	 * <p>
	 * (2) If the site index of the primary species has (now) been set, calculate that of the other species whose site
	 * index has not been set from the primary site index after converting it between the site curve of the other
	 * species and that of the primary species.
	 *
	 * @param lps the bank in which the calculations are done.
	 * @throws ProcessingException
	 */
	static void estimateMissingSiteIndices(LayerProcessingState lps) throws ProcessingException {

		Bank bank = lps.getBank();

		int pspIndex = lps.getPrimarySpeciesIndex();
		SiteIndexEquation pspSiteCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(pspIndex));

		// (1)

		if (Float.isNaN(bank.siteIndices[pspIndex])) {

			double otherSiteIndicesSum = 0.0f;
			int nOtherSiteIndices = 0;

			for (int spIndex : lps.getIndices()) {

				if (spIndex == pspIndex) {
					continue;
				}

				float spSiteIndex = bank.siteIndices[spIndex];

				if (!Float.isNaN(spSiteIndex)) {
					SiteIndexEquation spSiteCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex));

					try {
						double mappedSiteIndex = SiteTool
								.convertSiteIndexBetweenCurves(spSiteCurve, spSiteIndex, pspSiteCurve);
						if (mappedSiteIndex > 1.3) {
							otherSiteIndicesSum += mappedSiteIndex;
							nOtherSiteIndices += 1;
						}
					} catch (NoAnswerException e) {
						logger.warn(
								MessageFormat.format(
										"there is no conversion from curves {0} to {1}. Excluding species {2}"
												+ " from the estimation of the site index of {3}",
										spSiteCurve, pspSiteCurve, bank.speciesNames[spIndex],
										bank.speciesNames[pspIndex]
								)
						);
					} catch (CurveErrorException | SpeciesErrorException e) {
						throw new ProcessingException(
								MessageFormat.format(
										"convertSiteIndexBetweenCurves on {0}, {1} and {2} failed", spSiteCurve,
										spSiteIndex, pspSiteCurve
								), e
						);
					}
				}
			}

			if (nOtherSiteIndices > 0) {
				bank.siteIndices[pspIndex] = (float) (otherSiteIndicesSum / nOtherSiteIndices);
			}
		}

		// (2)

		float pspSiteIndex = bank.siteIndices[pspIndex];
		if (!Float.isNaN(bank.siteIndices[pspIndex])) {

			for (int spIndex : lps.getIndices()) {

				if (spIndex == pspIndex) {
					continue;
				}

				float spSiteIndex = bank.siteIndices[spIndex];
				if (Float.isNaN(spSiteIndex)) {
					SiteIndexEquation spSiteCurve = SiteIndexEquation.getByIndex(lps.getSiteCurveNumber(spIndex));

					try {
						double mappedSiteIndex = SiteTool
								.convertSiteIndexBetweenCurves(pspSiteCurve, pspSiteIndex, spSiteCurve);
						if (mappedSiteIndex > 1.3f) {
							pspSiteIndex = (float) mappedSiteIndex;
						}
						if (mappedSiteIndex > 0.0f) {
							bank.siteIndices[spIndex] = (float) mappedSiteIndex;
						}
					} catch (NoAnswerException e) {
						logger.warn(
								MessageFormat.format(
										"there is no conversion between curves {0} and {1}. Not calculating site index for species {2}",
										pspSiteCurve, spSiteCurve, bank.speciesNames[spIndex]
								)
						);
					} catch (CurveErrorException | SpeciesErrorException e) {
						throw new ProcessingException(
								MessageFormat.format(
										"convertSiteIndexBetweenCurves on {0}, {1} and {2} failed", pspSiteCurve,
										pspSiteIndex, spSiteCurve
								), e
						);
					}
				}
			}
		}

		// Finally, set bank.siteIndices[0] to that of the primary species.
		bank.siteIndices[0] = pspSiteIndex;
	}

	/**
	 * VPRIME1, method == 1: calculate the percentage of forested land covered by each species by dividing the basal
	 * area of each given species with the basal area of the polygon covered by forest.
	 *
	 * @param state the bank in which the calculations are performed
	 */
	static void calculateCoverages(LayerProcessingState lps) {

		Bank bank = lps.getBank();

		logger.atDebug().addArgument(lps.getNSpecies()).addArgument(bank.basalAreas[0][0]).log(
				"Calculating coverages as a ratio of Species BA over Total BA. # species: {}; Layer total 7.5cm+ basal area: {}"
		);

		for (int i : lps.getIndices()) {
			bank.percentagesOfForestedLand[i] = bank.basalAreas[i][UC_ALL_INDEX] / bank.basalAreas[0][UC_ALL_INDEX]
					* 100.0f;

			logger.atDebug().addArgument(i).addArgument(bank.speciesIndices[i]).addArgument(bank.speciesNames[i])
					.addArgument(bank.basalAreas[i][UC_ALL_INDEX]).addArgument(bank.percentagesOfForestedLand[i])
					.log("Species {}: SP0 {}, Name {}, Species 7.5cm+ BA {}, Calculated Percent {}");
		}
	}

	/**
	 * Calculate the siteCurve number of all species for which one was not supplied. All calculations are done in the
	 * given bank but the result is also stored in the LayerProcessingState.
	 * <p>
	 * FORTRAN notes: the original SXINXSET function set both INXSC/INXSCV and BANK3/SCNB, except for index 0 of SCNB.
	 *
	 * @param bank         the bank in which the calculations are done.
	 * @param siteCurveMap the Site Curve definitions.
	 * @param lps          the PolygonProcessingState to where the calculated curves are also to be
	 */
	static void calculateMissingSiteCurves(
			LayerProcessingState lps, MatrixMap2<String, Region, SiteIndexEquation> siteCurveMap
	) {
		Bank bank = lps.getBank();

		BecDefinition becZone = bank.getBecZone();

		for (int i : bank.getIndices()) {

			if (bank.siteCurveNumbers[i] == VdypEntity.MISSING_INTEGER_VALUE) {

				Optional<SiteIndexEquation> scIndex = Optional.empty();

				Optional<Sp64Distribution> sp0Dist = bank.sp64Distributions[i].getSpeciesDistribution(1);

				// First alternative is to use the name of the first of the species' sp64Distributions
				if (sp0Dist.isPresent()) {
					if (!siteCurveMap.isEmpty()) {
						scIndex = Utils.optSafe(siteCurveMap.get(sp0Dist.get().getGenusAlias(), becZone.getRegion()));
					} else {
						SiteIndexEquation siCurve = SiteTool
								.getSICurve(bank.speciesNames[i], becZone.getRegion().equals(Region.COASTAL));
						scIndex = siCurve == SiteIndexEquation.SI_NO_EQUATION ? Optional.empty() : Optional.of(siCurve);
					}
				}

				// Second alternative is to use the species name as given in the species' "speciesName" field
				if (scIndex.isEmpty()) {
					String sp0 = bank.speciesNames[i];
					if (!siteCurveMap.isEmpty()) {
						scIndex = Utils.optSafe(siteCurveMap.get(sp0, becZone.getRegion()));
					} else {
						SiteIndexEquation siCurve = SiteTool
								.getSICurve(sp0, becZone.getRegion().equals(Region.COASTAL));
						scIndex = siCurve == SiteIndexEquation.SI_NO_EQUATION ? Optional.empty() : Optional.of(siCurve);
					}
				}

				bank.siteCurveNumbers[i] = scIndex.orElseThrow().n();
			}
		}

		lps.setSiteCurveNumbers(bank.siteCurveNumbers);
	}

	/**
	 * Validate that the given polygon is in good order for processing.
	 *
	 * @param polygon the subject polygon.
	 * @returns if this method doesn't throw, all is good.
	 * @throws ProcessingException if the polygon does not pass validation.
	 */
	private static void validatePolygon(VdypPolygon polygon) throws ProcessingException {

		if (polygon.getPolygonIdentifier().getYear() < 1900) {

			throw new ProcessingException(
					MessageFormat.format(
							"Polygon {0}''s year value {1} is < 1900", polygon.getPolygonIdentifier().getName(),
							polygon.getPolygonIdentifier().getYear()
					)
			);
		}
	}

	private static void stopIfNoWork(ForwardProcessingState fps) throws ProcessingException {

		// The following is extracted from BANKCHK1, simplified for the parameters
		// METH_IN = 4, LayerI = 1, and INSTANCE = 1. So IR = 1, which is the first
		// bank, numbered 0.

		// => all that is done is that an exception is thrown if there are no species to
		// process.

		if (fps.getPrimaryLayerProcessingState().getNSpecies() == 0) {
			throw new ProcessingException(
					MessageFormat.format(
							"Polygon {0} layer 0 has no species with basal area above {1}",
							fps.getCompactPolygonIdentifier(), MIN_BASAL_AREA
					)
			);
		}
	}

	/** Default Equation Group, by species. Indexed by the species number, a one-based value. */
	private static final int[] defaultEquationGroups = { 0 /* placeholder */, 1, 2, 3, 4, 1, 2, 5, 6, 7, 1, 9, 8, 9, 9,
			10, 4 };
	private static final Set<Integer> exceptedSpeciesIndicies = new HashSet<>(List.of(3, 4, 5, 6, 10));

	// PRIMFIND
	/**
	 * Returns a {@code SpeciesRankingDetails} instance giving:
	 * <ul>
	 * <li>the index in {@code bank} of the primary species
	 * <li>the index in {@code bank} of the secondary species, or Optional.empty() if none, and
	 * <li>the percentage of forested land occupied by the primary species
	 * </ul>
	 *
	 * @param lps the bank on which to operate
	 * @return as described
	 */
	void determinePolygonRankings(Collection<List<String>> speciesToCombine) {

		LayerProcessingState lps = fps.getPrimaryLayerProcessingState();
		Bank bank = lps.getBank();

		if (lps.getNSpecies() == 0) {
			throw new IllegalArgumentException("Can not find primary species as there are no species");
		}

		float[] percentages = Arrays.copyOf(bank.percentagesOfForestedLand, bank.percentagesOfForestedLand.length);

		for (var speciesPair : speciesToCombine) {
			combinePercentages(bank.speciesNames, speciesPair, percentages);
		}

		float highestPercentage = 0.0f;
		int highestPercentageIndex = -1;
		float secondHighestPercentage = 0.0f;
		int secondHighestPercentageIndex = -1;
		for (int i : lps.getIndices()) {

			if (percentages[i] > highestPercentage) {

				secondHighestPercentageIndex = highestPercentageIndex;
				secondHighestPercentage = highestPercentage;
				highestPercentageIndex = i;
				highestPercentage = percentages[i];

			} else if (percentages[i] > secondHighestPercentage) {

				secondHighestPercentageIndex = i;
				secondHighestPercentage = percentages[i];
			}

			// TODO: implement NDEBUG22 = 1 logic
		}

		if (highestPercentageIndex == -1) {
			throw new IllegalStateException("There are no species with covering percentage > 0");
		}

		String primaryGenusName = bank.speciesNames[highestPercentageIndex];
		Optional<String> secondaryGenusName = secondHighestPercentageIndex != -1
				? Optional.of(bank.speciesNames[secondHighestPercentageIndex]) : Optional.empty();

		try {
			int inventoryTypeGroup = findInventoryTypeGroup(primaryGenusName, secondaryGenusName, highestPercentage);

			int basalAreaGroup1 = 0;

			String primarySpeciesName = bank.speciesNames[highestPercentageIndex];
			String becZoneAlias = bank.getBecZone().getAlias();

			int defaultEquationGroup = fps.fcm.getDefaultEquationGroup().get(primarySpeciesName, becZoneAlias);
			Optional<Integer> equationModifierGroup = fps.fcm.getEquationModifierGroup()
					.get(defaultEquationGroup, inventoryTypeGroup);
			basalAreaGroup1 = equationModifierGroup.orElse(defaultEquationGroup);

			int primarySpeciesIndex = bank.speciesIndices[highestPercentageIndex];
			int basalAreaGroup3 = defaultEquationGroups[primarySpeciesIndex];
			if (Region.INTERIOR.equals(bank.getBecZone().getRegion())
					&& exceptedSpeciesIndicies.contains(primarySpeciesIndex)) {
				basalAreaGroup3 += 20;
			}

			lps.setSpeciesRankingDetails(
					new SpeciesRankingDetails(
							highestPercentageIndex,
							secondHighestPercentageIndex != -1 ? Optional.of(secondHighestPercentageIndex)
									: Optional.empty(),
							inventoryTypeGroup, basalAreaGroup1, basalAreaGroup3
					)
			);
		} catch (ProcessingException e) {
			// This should never fail because the bank has already been validated and hence the genera
			// are known to be valid.

			throw new IllegalStateException(e);
		}
	}

	/**
	 * <code>combinationGroup</code> is a list of precisely two species names. This method determines the indices within
	 * <code>speciesNames</code> that match the two given names. If two do, say i and j, the <code>percentages</code> is
	 * modified as follows. Assuming percentages[i] > percentages[j], percentages[i] is set to percentages[i] +
	 * percentages[j] and percentages[j] is set to 0.0. If fewer than two indices match, nothing is done.
	 *
	 * @param speciesNames     an array of (possibly null) distinct Strings.
	 * @param combinationGroup a pair of (not null) Strings.
	 * @param percentages      an array with one entry for each entry in <code>speciesName</code>.
	 */
	static void combinePercentages(String[] speciesNames, List<String> combinationGroup, float[] percentages) {

		if (combinationGroup.size() != 2) {
			throw new IllegalArgumentException(
					MessageFormat.format("combinationGroup must have size 2; it has size {0}", combinationGroup.size())
			);
		}

		if (combinationGroup.get(0) == null || combinationGroup.get(1) == null) {
			throw new IllegalArgumentException("combinationGroup must not contain null values");
		}

		if (speciesNames.length != percentages.length) {
			throw new IllegalArgumentException(
					MessageFormat.format(
							"the length of speciesNames ({}) must match that of percentages ({}) but it doesn't",
							speciesNames.length, percentages.length
					)
			);
		}

		Set<Integer> groupIndices = new HashSet<>();
		for (int i = 0; i < speciesNames.length; i++) {
			if (combinationGroup.contains(speciesNames[i]))
				groupIndices.add(i);
		}

		if (groupIndices.size() == 2) {
			Integer[] groupIndicesArray = new Integer[2];
			groupIndices.toArray(groupIndicesArray);

			int higherPercentageIndex;
			int lowerPercentageIndex;
			if (percentages[groupIndicesArray[0]] > percentages[groupIndicesArray[1]]) {
				higherPercentageIndex = groupIndicesArray[0];
				lowerPercentageIndex = groupIndicesArray[1];
			} else {
				higherPercentageIndex = groupIndicesArray[1];
				lowerPercentageIndex = groupIndicesArray[0];
			}
			percentages[higherPercentageIndex] = percentages[higherPercentageIndex] + percentages[lowerPercentageIndex];
			percentages[lowerPercentageIndex] = 0.0f;
		}
	}

	// ITGFIND
	/**
	 * Find Inventory type group (ITG) for the given primary and secondary (if given) genera.
	 *
	 * @param primarySp0           the genus of the primary species
	 * @param optionalSecondarySp0 the genus of the primary species, which may be empty
	 * @param primaryPercentage    the percentage covered by the primary species
	 * @return as described
	 * @throws ProcessingException if primaryGenus is not a known genus
	 */
	@SuppressWarnings("java:S3776")
	static int findInventoryTypeGroup(String primarySp0, Optional<String> optionalSecondarySp0, float primaryPercentage)
			throws ProcessingException {

		if (primaryPercentage > 79.999 /* Copied from VDYP7 */) {

			Integer recordedInventoryTypeGroup = CommonData.ITG_PURE.get(primarySp0);
			if (recordedInventoryTypeGroup == null) {
				throw new ProcessingException("Unrecognized primary species: " + primarySp0);
			}

			return recordedInventoryTypeGroup;
		}

		String secondaryGenus = optionalSecondarySp0.isPresent() ? optionalSecondarySp0.get() : "";

		switch (primarySp0) {
		case "F":
			switch (secondaryGenus) {
			case "C", "Y":
				return 2;
			case "B", "H":
				return 3;
			case "S":
				return 4;
			case "PL", "PA":
				return 5;
			case "PY":
				return 6;
			case "L", "PW":
				return 7;
			default:
				return 8;
			}
		case "C", "Y":
			switch (secondaryGenus) {
			case "H", "B", "S":
				return 11;
			default:
				return 10;
			}
		case "H":
			switch (secondaryGenus) {
			case "C", "Y":
				return 14;
			case "B":
				return 15;
			case "S":
				return 16;
			default:
				return 13;
			}
		case "B":
			switch (secondaryGenus) {
			case "C", "Y", "H":
				return 19;
			default:
				return 20;
			}
		case "S":
			switch (secondaryGenus) {
			case "C", "Y", "H":
				return 23;
			case "B":
				return 24;
			case "PL":
				return 25;
			default:
				if (CommonData.HARDWOODS.contains(secondaryGenus)) {
					return 26;
				}
				return 22;
			}
		case "PW":
			return 27;
		case "PL", "PA":
			switch (secondaryGenus) {
			case "PL", "PA":
				return 28;
			case "F", "PW", "L", "PY":
				return 29;
			default:
				if (CommonData.HARDWOODS.contains(secondaryGenus)) {
					return 31;
				}
				return 30;
			}
		case "PY":
			return 32;
		case "L":
			switch (secondaryGenus) {
			case "F":
				return 33;
			default:
				return 34;
			}
		case "AC":
			if (CommonData.HARDWOODS.contains(secondaryGenus)) {
				return 36;
			}
			return 35;
		case "D":
			if (CommonData.HARDWOODS.contains(secondaryGenus)) {
				return 38;
			}
			return 37;
		case "MB":
			return 39;
		case "E":
			return 40;
		case "AT":
			if (CommonData.HARDWOODS.contains(secondaryGenus)) {
				return 42;
			}
			return 41;
		default:
			throw new ProcessingException("Unrecognized primary species: " + primarySp0);
		}
	}
}
