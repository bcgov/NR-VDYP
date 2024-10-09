package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.OVER225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U125TO175;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U175TO225;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClass.U75TO125;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.BASAL_AREA;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.LOREY_HEIGHT;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.QUAD_MEAN_DIAMETER;
import static ca.bc.gov.nrs.vdyp.model.UtilizationClassVariable.WHOLE_STEM_VOLUME;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE;
import static ca.bc.gov.nrs.vdyp.model.VolumeVariable.WHOLE_STEM_VOL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;

class PreliminarySetCompatibilityVariablesTest extends AbstractForwardProcessingEngineTest {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(PreliminarySetCompatibilityVariablesTest.class);

	@Test
	/** SET_COMPATIBILITY_VARIABLES */
	void testSetCompatibilityVariables() throws ResourceParseException, IOException, ProcessingException {

		var reader = new ForwardDataStreamReader(controlMap);
		var polygon = reader.readNextPolygon().orElseThrow();

		ForwardProcessingEngine fpe = new ForwardProcessingEngine(controlMap);
		fpe.processPolygon(polygon, ForwardProcessingEngine.ExecutionStep.SET_COMPATIBILITY_VARIABLES);

		// These values have been verified against the FORTRAN implementation, allowing for minor
		// platform-specific differences.

		LayerProcessingState lps = fpe.fps.getPrimaryLayerProcessingState();

		assertThat(
				lps.getVolumeEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 12, 20, 25, 37, 66 })
		);
		assertThat(
				lps.getDecayEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 7, 14, 19, 31, 54 })
		);
		assertThat(
				lps.getBreakageEquationGroups(),
				Matchers.is(new int[] { VdypEntity.MISSING_INTEGER_VALUE, 5, 6, 12, 17, 28 })
		);

		assertThat(lps.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0063979626f));
		assertThat(lps.getCVVolume(1, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00016450882f));

		assertThat(lps.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00024962425f));
		assertThat(lps.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00011026859f));
		assertThat(lps.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.000006198883f));
		assertThat(lps.getCVVolume(2, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.000024557114f));

		assertThat(lps.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0062299743f));
		assertThat(lps.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0010375977f));
		assertThat(lps.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0001244545f));
		assertThat(lps.getCVVolume(3, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0000038146973f));

		assertThat(lps.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.00013566017f));
		assertThat(lps.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.00033128262f));
		assertThat(lps.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.00021290779f));
		assertThat(lps.getCVVolume(4, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.0000059604645f));

		assertThat(lps.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.000882864f));
		assertThat(lps.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.0002478361f));
		assertThat(lps.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(0.0008614063f));
		assertThat(lps.getCVVolume(5, OVER225, CLOSE_UTIL_VOL, LayerType.PRIMARY), is(-0.0000052452087f));

		assertThat(lps.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-0.0048389435f));
		assertThat(lps.getCVVolume(1, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(1.3446808E-4f));

		assertThat(lps.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.01768279f));
		assertThat(lps.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0010006428f));
		assertThat(lps.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(2.2292137E-4f));
		assertThat(lps.getCVVolume(2, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-1.9311905E-5f));

		assertThat(lps.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.010708809f));
		assertThat(lps.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(7.4100494E-4f));
		assertThat(lps.getCVVolume(3, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-2.4795532E-5f));

		assertThat(lps.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.011499405f));
		assertThat(lps.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-0.0010294914f));
		assertThat(lps.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-9.3603134E-4f));
		assertThat(lps.getCVVolume(4, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-5.4836273E-5f));

		assertThat(lps.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(0.010175705f));
		assertThat(lps.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-0.0014338493f));
		assertThat(lps.getCVVolume(5, OVER225, CLOSE_UTIL_VOL_LESS_DECAY, LayerType.PRIMARY), is(-2.9087067E-5f));

		assertThat(lps.getCVVolume(1, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(
				lps.getCVVolume(1, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.035768032f)
		);
		assertThat(
				lps.getCVVolume(1, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0016698837f)
		);

		assertThat(
				lps.getCVVolume(2, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.16244507f)
		);
		assertThat(
				lps.getCVVolume(2, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0045113564f)
		);
		assertThat(
				lps.getCVVolume(2, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.0030164719f)
		);
		assertThat(
				lps.getCVVolume(2, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(3.528595E-5f)
		);

		assertThat(lps.getCVVolume(3, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(3, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(3, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(
				lps.getCVVolume(3, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(4.1484833E-5f)
		);

		assertThat(
				lps.getCVVolume(4, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(-0.13775301f)
		);
		assertThat(
				lps.getCVVolume(4, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.005630493f)
		);
		assertThat(
				lps.getCVVolume(4, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(0.0028266907f)
		);
		assertThat(
				lps.getCVVolume(4, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY),
				is(3.7765503E-4f)
		);

		assertThat(lps.getCVVolume(5, U75TO125, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(5, U125TO175, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(5, U175TO225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(0.0f));
		assertThat(
				lps.getCVVolume(5, OVER225, CLOSE_UTIL_VOL_LESS_DECAY_LESS_WASTAGE, LayerType.PRIMARY), is(5.378723E-4f)
		);

		assertThat(lps.getCVVolume(1, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0f));
		assertThat(lps.getCVVolume(1, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-2.5427341E-4f));
		assertThat(lps.getCVVolume(1, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0023424625f));
		assertThat(lps.getCVVolume(1, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-7.033348E-5f));

		assertThat(lps.getCVVolume(2, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-2.9444695E-5f));
		assertThat(lps.getCVVolume(2, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(4.208088E-5f));
		assertThat(lps.getCVVolume(2, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-2.4318695E-5f));
		assertThat(lps.getCVVolume(2, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(9.536743E-7f));

		assertThat(lps.getCVVolume(3, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(0.0013506413f));
		assertThat(lps.getCVVolume(3, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(4.787445E-4f));
		assertThat(lps.getCVVolume(3, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(3.9100647E-5f));
		assertThat(lps.getCVVolume(3, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-1.4305115E-6f));

		assertThat(lps.getCVVolume(4, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-7.891655E-5f));
		assertThat(lps.getCVVolume(4, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(2.7656555E-5f));
		assertThat(lps.getCVVolume(4, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(4.196167E-5f));
		assertThat(lps.getCVVolume(4, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-1.0967255E-5f));

		assertThat(lps.getCVVolume(5, U75TO125, WHOLE_STEM_VOL, LayerType.PRIMARY), is(1.8835068E-5f));
		assertThat(lps.getCVVolume(5, U125TO175, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-1.0085106E-4f));
		assertThat(lps.getCVVolume(5, U175TO225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(1.6236305E-4f));
		assertThat(lps.getCVVolume(5, OVER225, WHOLE_STEM_VOL, LayerType.PRIMARY), is(-7.390976E-6f));

		assertThat(lps.getCVBasalArea(1, OVER225, LayerType.PRIMARY), is(1.4913082E-4f));
		assertThat(lps.getCVBasalArea(1, U125TO175, LayerType.PRIMARY), is(-5.034916E-5f));
		assertThat(lps.getCVBasalArea(1, U175TO225, LayerType.PRIMARY), is(-7.482059E-5f));
		assertThat(lps.getCVBasalArea(1, U75TO125, LayerType.PRIMARY), is(-2.397038E-5f));

		assertThat(lps.getCVBasalArea(2, OVER225, LayerType.PRIMARY), is(-2.193451E-5f));
		assertThat(lps.getCVBasalArea(2, U125TO175, LayerType.PRIMARY), is(5.4836273E-6f));
		assertThat(lps.getCVBasalArea(2, U175TO225, LayerType.PRIMARY), is(9.596348E-6f));
		assertThat(lps.getCVBasalArea(2, U75TO125, LayerType.PRIMARY), is(6.660819E-6f));

		assertThat(lps.getCVBasalArea(3, OVER225, LayerType.PRIMARY), is(9.918213E-5f));
		assertThat(lps.getCVBasalArea(3, U125TO175, LayerType.PRIMARY), is(-1.5150756E-5f));
		assertThat(lps.getCVBasalArea(3, U175TO225, LayerType.PRIMARY), is(-7.9244375E-5f));
		assertThat(lps.getCVBasalArea(3, U75TO125, LayerType.PRIMARY), is(-4.341826E-6f));

		assertThat(lps.getCVBasalArea(4, OVER225, LayerType.PRIMARY), is(1.9073486E-4f));
		assertThat(lps.getCVBasalArea(4, U125TO175, LayerType.PRIMARY), is(-8.2850456E-5f));
		assertThat(lps.getCVBasalArea(4, U175TO225, LayerType.PRIMARY), is(-5.2928925E-5f));
		assertThat(lps.getCVBasalArea(4, U75TO125, LayerType.PRIMARY), is(-5.531311E-5f));

		assertThat(lps.getCVBasalArea(5, OVER225, LayerType.PRIMARY), is(1.2397766E-4f));
		assertThat(lps.getCVBasalArea(5, U125TO175, LayerType.PRIMARY), is(-3.7431717E-5f));
		assertThat(lps.getCVBasalArea(5, U175TO225, LayerType.PRIMARY), is(-7.364154E-5f));
		assertThat(lps.getCVBasalArea(5, U75TO125, LayerType.PRIMARY), is(-1.289323E-5f));

		assertThat(lps.getCVQuadraticMeanDiameter(1, OVER225, LayerType.PRIMARY), is(0.0072517395f));
		assertThat(lps.getCVQuadraticMeanDiameter(1, U125TO175, LayerType.PRIMARY), is(-0.014289856f));
		assertThat(lps.getCVQuadraticMeanDiameter(1, U175TO225, LayerType.PRIMARY), is(-0.04478264f));
		assertThat(lps.getCVQuadraticMeanDiameter(1, U75TO125, LayerType.PRIMARY), is(-0.020475388f));

		assertThat(lps.getCVQuadraticMeanDiameter(2, OVER225, LayerType.PRIMARY), is(6.942749E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(2, U125TO175, LayerType.PRIMARY), is(-2.0217896E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(2, U175TO225, LayerType.PRIMARY), is(6.008148E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(2, U75TO125, LayerType.PRIMARY), is(-1.2207031E-4f));

		assertThat(lps.getCVQuadraticMeanDiameter(3, OVER225, LayerType.PRIMARY), is(3.7002563E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(3, U125TO175, LayerType.PRIMARY), is(-0.008190155f));
		assertThat(lps.getCVQuadraticMeanDiameter(3, U175TO225, LayerType.PRIMARY), is(-0.0019168854f));
		assertThat(lps.getCVQuadraticMeanDiameter(3, U75TO125, LayerType.PRIMARY), is(-0.008534431f));

		assertThat(lps.getCVQuadraticMeanDiameter(4, OVER225, LayerType.PRIMARY), is(-0.0010547638f));
		assertThat(lps.getCVQuadraticMeanDiameter(4, U125TO175, LayerType.PRIMARY), is(-7.696152E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(4, U175TO225, LayerType.PRIMARY), is(-0.0012798309f));
		assertThat(lps.getCVQuadraticMeanDiameter(4, U75TO125, LayerType.PRIMARY), is(1.7547607E-4f));

		assertThat(lps.getCVQuadraticMeanDiameter(5, OVER225, LayerType.PRIMARY), is(-2.3651123E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(5, U125TO175, LayerType.PRIMARY), is(9.880066E-4f));
		assertThat(lps.getCVQuadraticMeanDiameter(5, U175TO225, LayerType.PRIMARY), is(-0.005466461f));
		assertThat(lps.getCVQuadraticMeanDiameter(5, U75TO125, LayerType.PRIMARY), is(-7.972717E-4f));

		assertThat(lps.getCVSmall(1, BASAL_AREA), is(-2.1831444E-7f));
		assertThat(lps.getCVSmall(1, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(1, LOREY_HEIGHT), is(0.0f));
		assertThat(lps.getCVSmall(1, WHOLE_STEM_VOLUME), is(0.0f));

		assertThat(lps.getCVSmall(2, BASAL_AREA), is(-4.496146E-5f));
		assertThat(lps.getCVSmall(2, QUAD_MEAN_DIAMETER), is(0.0023670197f));
		assertThat(lps.getCVSmall(2, LOREY_HEIGHT), is(1.3113013E-6f));
		assertThat(lps.getCVSmall(2, WHOLE_STEM_VOLUME), is(0.0010289619f));

		assertThat(lps.getCVSmall(3, BASAL_AREA), is(4.9466034E-6f));
		assertThat(lps.getCVSmall(3, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(3, LOREY_HEIGHT), is(-1.5556934E-5f));
		assertThat(lps.getCVSmall(3, WHOLE_STEM_VOLUME), is(0.0f));

		assertThat(lps.getCVSmall(4, BASAL_AREA), is(0.0f));
		assertThat(lps.getCVSmall(4, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(4, LOREY_HEIGHT), is(0.0f));
		assertThat(lps.getCVSmall(4, WHOLE_STEM_VOLUME), is(0.0f));

		assertThat(lps.getCVSmall(5, BASAL_AREA), is(3.4208642E-6f));
		assertThat(lps.getCVSmall(5, QUAD_MEAN_DIAMETER), is(0.0f));
		assertThat(lps.getCVSmall(5, LOREY_HEIGHT), is(-5.7758567E-5f));
		assertThat(lps.getCVSmall(5, WHOLE_STEM_VOLUME), is(0.0f));
	}
}
