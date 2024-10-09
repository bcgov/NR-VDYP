package ca.bc.gov.nrs.vdyp.forward.controlmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.forward.ForwardControlParser;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardControlVariables;
import ca.bc.gov.nrs.vdyp.forward.model.ForwardDebugSettings;
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.BecLookup;
import ca.bc.gov.nrs.vdyp.model.CompVarAdjustments;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.MatrixMap2;
import ca.bc.gov.nrs.vdyp.model.MatrixMap3;

public class ForwardResolvedControlMapTest {

	private static final Logger logger = LoggerFactory.getLogger(ForwardResolvedControlMapTest.class);

	@Test
	void testForwardResolvedControlMap() throws IOException, ResourceParseException {
		logger.info(this.getClass().getName() + ":testForwardResolvedControlMap running...");

		var parser = new ForwardControlParser();
		var rawControlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");
		var forwardControlMap = new ForwardResolvedControlMapImpl(rawControlMap);

		assertThat(forwardControlMap.getControlMap(), is(rawControlMap));

		Object e;
		e = forwardControlMap.getDebugSettings();
		assertThat(e, instanceOf(ForwardDebugSettings.class));
		e = forwardControlMap.getForwardControlVariables();
		assertThat(e, instanceOf(ForwardControlVariables.class));
		e = forwardControlMap.getSiteCurveMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getCompVarAdjustments();
		assertThat(e, instanceOf(CompVarAdjustments.class));
		e = forwardControlMap.getBasalAreaYieldCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getQuadMeanDiameterYieldCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getBasalAreaGrowthFiatDetails();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getBasalAreaGrowthEmpiricalCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getUpperBoundsCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = forwardControlMap.getQuadMeanDiameterGrowthFiatDetails();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getQuadMeanDiameterGrowthEmpiricalCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getQuadMeanDiameterGrowthEmpiricalLimits();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getLoreyHeightPrimarySpeciesEquationP1Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getLoreyHeightNonPrimaryCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = forwardControlMap.getPrimarySpeciesBasalAreaGrowthCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getNonPrimarySpeciesBasalAreaGrowthCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getPrimarySpeciesQuadMeanDiameterGrowthCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getNonPrimarySpeciesQuadMeanDiameterGrowthCoefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getBecLookup();
		assertThat(e, instanceOf(BecLookup.class));
		e = forwardControlMap.getGenusDefinitionMap();
		assertThat(e, instanceOf(GenusDefinitionMap.class));
		e = forwardControlMap.getNetDecayWasteCoeMap();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getNetDecayCoeMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getWasteModifierMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getDecayModifierMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getCloseUtilizationCoeMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getTotalStandWholeStepVolumeCoeMap();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getWholeStemUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getQuadMeanDiameterUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = forwardControlMap.getBasalAreaDiameterUtilizationComponentMap();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = forwardControlMap.getSmallComponentWholeStemVolumeCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getSmallComponentLoreyHeightCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getSmallComponentQuadMeanDiameterCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getSmallComponentBasalAreaCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getSmallComponentProbabilityCoefficients();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getMaximumAgeBySiteCurveNumber();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getUpperBounds();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getDefaultEquationGroup();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getEquationModifierGroup();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getHl1Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getHl2Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getHl3Coefficients();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getHlNonPrimaryCoefficients();
		assertThat(e, instanceOf(MatrixMap3.class));
		e = forwardControlMap.getComponentSizeLimits();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getNetBreakageMap();
		assertThat(e, instanceOf(Map.class));
		e = forwardControlMap.getVolumeEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getDecayEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getBreakageEquationGroups();
		assertThat(e, instanceOf(MatrixMap2.class));
		e = forwardControlMap.getQuadMeanDiameterBySpeciesCoefficients();
		assertThat(e, instanceOf(Map.class));
	}
}
