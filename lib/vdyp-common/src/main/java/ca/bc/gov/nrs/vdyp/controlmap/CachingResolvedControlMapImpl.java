package ca.bc.gov.nrs.vdyp.controlmap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.io.parse.coe.ModifierParser;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.Coefficients;
import ca.bc.gov.nrs.vdyp.model.ComponentSizeLimits;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;
import ca.bc.gov.nrs.vdyp.model.NonprimaryHLCoefficients;
import ca.bc.gov.nrs.vdyp.model.Region;
import ca.bc.gov.nrs.vdyp.model.SiteCurveAgeMaximum;

public class CachingResolvedControlMapImpl implements ResolvedControlMap {

	private final Optional<BecLookup> becLookup;
	private final Optional<GenusDefinitionMap> genusDefinitionMap;
	private final Optional<Map<String, Coefficients>> netDecayWasteCoeMap;
	private final Optional<MatrixMap2<Integer, Integer, Optional<Coefficients>>> netDecayCoeMap;
	private final Optional<MatrixMap2<String, Region, Float>> wasteModifierMap;
	private final Optional<MatrixMap2<String, Region, Float>> decayModifierMap;
	private final Optional<MatrixMap2<Integer, Integer, Optional<Coefficients>>> closeUtilizationCoeMap;
	private final Optional<Map<Integer, Coefficients>> totalStandWholeStepVolumeCoeMap;
	private final Optional<MatrixMap2<Integer, Integer, Optional<Coefficients>>> wholeStemUtilizationComponentMap;
	private final Optional<MatrixMap3<Integer, String, String, Coefficients>> quadMeanDiameterUtilizationComponentMap;
	private final Optional<MatrixMap3<Integer, String, String, Coefficients>> basalAreaDiameterUtilizationComponentMap;
	private final Optional<Map<String, Coefficients>> smallComponentWholeStemVolumeCoefficients;
	private final Optional<Map<String, Coefficients>> smallComponentLoreyHeightCoefficients;
	private final Optional<Map<String, Coefficients>> smallComponentQuadMeanDiameterCoefficients;
	private final Optional<Map<String, Coefficients>> smallComponentBasalAreaCoefficients;
	private final Optional<Map<String, Coefficients>> smallComponentProbabilityCoefficients;
	private final Optional<Map<Integer, SiteCurveAgeMaximum>> maximumAgeBySiteCurveNumber;
	private final Optional<Map<Integer, Coefficients>> upperBounds;
	private final Optional<MatrixMap2<String, String, Integer>> defaultEquationGroup;
	private final Optional<MatrixMap2<Integer, Integer, Optional<Integer>>> equationModifierGroup;
	private final Optional<MatrixMap2<String, Region, Coefficients>> hl1Coefficients;
	private final Optional<MatrixMap2<String, Region, Coefficients>> hl2Coefficients;
	private final Optional<MatrixMap2<String, Region, Coefficients>> hl3Coefficients;
	private final Optional<MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>>> hlNonPrimaryCoefficients;
	private final Optional<MatrixMap2<String, Region, ComponentSizeLimits>> componentSizeLimitCoefficients;
	private final Optional<Map<Integer, Coefficients>> breakageMap;
	private final Optional<MatrixMap2<String, String, Integer>> volumeEquationGroups;
	private final Optional<MatrixMap2<String, String, Integer>> decayEquationGroups;
	private final Optional<MatrixMap2<String, String, Integer>> breakageEquationGroups;
	private final Optional<Map<String, Coefficients>> quadMeanDiameterBySpeciesCoefficients;

	private final Map<String, Object> controlMap;

	public CachingResolvedControlMapImpl(Map<String, Object> controlMap) {

		this.controlMap = controlMap;

		this.becLookup = this.get(ControlKey.BEC_DEF, BecLookup.class);

		this.genusDefinitionMap = this.get(ControlKey.SP0_DEF, GenusDefinitionMap.class);
		this.netDecayWasteCoeMap = this.get(ControlKey.VOLUME_NET_DECAY_WASTE, Map.class);
		this.netDecayCoeMap = this.get(ControlKey.VOLUME_NET_DECAY, MatrixMap2.class);
		this.wasteModifierMap = this.get(ControlKey.WASTE_MODIFIERS, MatrixMap2.class);
		this.decayModifierMap = this.get(ModifierParser.CONTROL_KEY_MOD301_DECAY, MatrixMap2.class);
		this.closeUtilizationCoeMap = this.get(ControlKey.CLOSE_UTIL_VOLUME, MatrixMap2.class);
		this.totalStandWholeStepVolumeCoeMap = this.get(ControlKey.TOTAL_STAND_WHOLE_STEM_VOL, Map.class);
		this.wholeStemUtilizationComponentMap = this.get(ControlKey.UTIL_COMP_WS_VOLUME, MatrixMap2.class);
		this.quadMeanDiameterUtilizationComponentMap = this.get(ControlKey.UTIL_COMP_DQ, MatrixMap3.class);
		this.basalAreaDiameterUtilizationComponentMap = this.get(ControlKey.UTIL_COMP_BA, MatrixMap3.class);
		this.smallComponentWholeStemVolumeCoefficients = this.get(ControlKey.SMALL_COMP_WS_VOLUME, Map.class);
		this.smallComponentLoreyHeightCoefficients = this.get(ControlKey.SMALL_COMP_HL, Map.class);
		this.smallComponentQuadMeanDiameterCoefficients = this.get(ControlKey.SMALL_COMP_DQ, Map.class);
		this.smallComponentBasalAreaCoefficients = this.get(ControlKey.SMALL_COMP_BA, Map.class);
		this.smallComponentProbabilityCoefficients = this.get(ControlKey.SMALL_COMP_PROBABILITY, Map.class);
		this.maximumAgeBySiteCurveNumber = this.get(ControlKey.SITE_CURVE_AGE_MAX, Map.class);
		this.defaultEquationGroup = this.get(ControlKey.DEFAULT_EQ_NUM, MatrixMap2.class);
		this.hl1Coefficients = this.get(ControlKey.HL_PRIMARY_SP_EQN_P1, MatrixMap2.class);
		this.hl2Coefficients = this.get(ControlKey.HL_PRIMARY_SP_EQN_P2, MatrixMap2.class);
		this.hl3Coefficients = this.get(ControlKey.HL_PRIMARY_SP_EQN_P3, MatrixMap2.class);
		this.hlNonPrimaryCoefficients = this.get(ControlKey.HL_NONPRIMARY, MatrixMap3.class);
		this.upperBounds = this.get(ControlKey.BA_DQ_UPPER_BOUNDS, Map.class);
		this.equationModifierGroup = this.get(ControlKey.EQN_MODIFIERS, MatrixMap2.class);
		this.componentSizeLimitCoefficients = this.get(ControlKey.SPECIES_COMPONENT_SIZE_LIMIT, MatrixMap2.class);
		this.breakageMap = this.get(ControlKey.BREAKAGE, Map.class);
		this.volumeEquationGroups = this.get(ControlKey.VOLUME_EQN_GROUPS, MatrixMap2.class);
		this.decayEquationGroups = this.get(ControlKey.DECAY_GROUPS, MatrixMap2.class);
		this.breakageEquationGroups = this.get(ControlKey.BREAKAGE_GROUPS, MatrixMap2.class);
		this.quadMeanDiameterBySpeciesCoefficients = this.get(ControlKey.BY_SPECIES_DQ, Map.class);
	}

	/**
	 * @return the underlying control map
	 */
	@Override
	public Map<String, Object> getControlMap() {
		return Collections.unmodifiableMap(controlMap);
	}

	protected <U> Optional<U> get(ControlKey key, Class<? super U> clazz) {
		return Utils.parsedControl(controlMap, key, clazz);
	}

	@Override
	public BecLookup getBecLookup() {
		return becLookup.orElseThrow();
	}

	@Override
	public GenusDefinitionMap getGenusDefinitionMap() {
		return genusDefinitionMap.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getNetDecayWasteCoeMap() {
		return netDecayWasteCoeMap.orElseThrow();
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getNetDecayCoeMap() {
		return netDecayCoeMap.orElseThrow();
	}

	@Override
	public MatrixMap2<String, Region, Float> getWasteModifierMap() {
		return wasteModifierMap.orElseThrow();
	}

	@Override
	public MatrixMap2<String, Region, Float> getDecayModifierMap() {
		return decayModifierMap.orElseThrow();
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getCloseUtilizationCoeMap() {
		return closeUtilizationCoeMap.orElseThrow();
	}

	@Override
	public Map<Integer, Coefficients> getTotalStandWholeStepVolumeCoeMap() {
		return totalStandWholeStepVolumeCoeMap.orElseThrow();
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Coefficients>> getWholeStemUtilizationComponentMap() {
		return wholeStemUtilizationComponentMap.orElseThrow();
	}

	@Override
	public MatrixMap3<Integer, String, String, Coefficients> getQuadMeanDiameterUtilizationComponentMap() {
		return quadMeanDiameterUtilizationComponentMap.orElseThrow();
	}

	@Override
	public MatrixMap3<Integer, String, String, Coefficients> getBasalAreaDiameterUtilizationComponentMap() {
		return basalAreaDiameterUtilizationComponentMap.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getSmallComponentWholeStemVolumeCoefficients() {
		return smallComponentWholeStemVolumeCoefficients.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getSmallComponentLoreyHeightCoefficients() {
		return smallComponentLoreyHeightCoefficients.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getSmallComponentQuadMeanDiameterCoefficients() {
		return smallComponentQuadMeanDiameterCoefficients.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getSmallComponentBasalAreaCoefficients() {
		return smallComponentBasalAreaCoefficients.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getSmallComponentProbabilityCoefficients() {
		return smallComponentProbabilityCoefficients.orElseThrow();
	}

	@Override
	public Map<Integer, SiteCurveAgeMaximum> getMaximumAgeBySiteCurveNumber() {
		return maximumAgeBySiteCurveNumber.orElseThrow();
	}

	@Override
	public Map<Integer, Coefficients> getUpperBounds() {
		return upperBounds.orElseThrow();
	}

	@Override
	public MatrixMap2<String, String, Integer> getDefaultEquationGroup() {
		return defaultEquationGroup.orElseThrow();
	}

	@Override
	public MatrixMap2<Integer, Integer, Optional<Integer>> getEquationModifierGroup() {
		return equationModifierGroup.orElseThrow();
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl1Coefficients() {
		return hl1Coefficients.orElseThrow();
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl2Coefficients() {
		return hl2Coefficients.orElseThrow();
	}

	@Override
	public MatrixMap2<String, Region, Coefficients> getHl3Coefficients() {
		return hl3Coefficients.orElseThrow();
	}

	@Override
	public MatrixMap3<String, String, Region, Optional<NonprimaryHLCoefficients>> getHlNonPrimaryCoefficients() {
		return hlNonPrimaryCoefficients.orElseThrow();
	}

	@Override
	public MatrixMap2<String, Region, ComponentSizeLimits> getComponentSizeLimits() {
		return componentSizeLimitCoefficients.orElseThrow();
	}

	@Override
	public Map<Integer, Coefficients> getNetBreakageMap() {
		return breakageMap.orElseThrow();
	}

	@Override
	public MatrixMap2<String, String, Integer> getVolumeEquationGroups() {
		return volumeEquationGroups.orElseThrow();
	}

	@Override
	public MatrixMap2<String, String, Integer> getDecayEquationGroups() {
		return decayEquationGroups.orElseThrow();
	}

	@Override
	public MatrixMap2<String, String, Integer> getBreakageEquationGroups() {
		return breakageEquationGroups.orElseThrow();
	}

	@Override
	public Map<String, Coefficients> getQuadMeanDiameterBySpeciesCoefficients() {
		return quadMeanDiameterBySpeciesCoefficients.orElseThrow();
	}
}
