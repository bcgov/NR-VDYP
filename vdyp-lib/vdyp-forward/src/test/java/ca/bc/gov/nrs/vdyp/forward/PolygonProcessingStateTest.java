package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.controlMapHasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.common.ControlKey;
import ca.bc.gov.nrs.vdyp.common.Utils;
import ca.bc.gov.nrs.vdyp.forward.Bank.CopyMode;
import ca.bc.gov.nrs.vdyp.forward.test.VdypForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.GenusDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSite;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

class PolygonProcessingStateTest {

	private ForwardControlParser parser;
	private Map<String, Object> controlMap;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@BeforeEach
	void before() throws IOException, ResourceParseException {

		parser = new ForwardControlParser();
		controlMap = VdypForwardTestUtils.parse(parser, "VDYP.CTR");
		assertThat(
				controlMap,
				(Matcher) controlMapHasEntry(
						ControlKey.SP0_DEF, allOf(instanceOf(List.class), hasItem(instanceOf(GenusDefinition.class)))
				)
		);
	}

	@Test
	void testConstruction() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank pps = new Bank(pLayer, Utils.getBec(polygon.getBiogeoclimaticZone(), controlMap), s -> true);

		int nSpecies = pLayer.getSpecies().size();

		assertThat(pps, notNullValue());
		assertThat(pps.yearsAtBreastHeight.length, is(nSpecies + 1));
		assertThat(pps.ageTotals.length, is(nSpecies + 1));
		assertThat(pps.dominantHeights.length, is(nSpecies + 1));
		assertThat(pps.percentagesOfForestedLand.length, is(nSpecies + 1));
		assertThat(pps.siteIndices.length, is(nSpecies + 1));
		assertThat(pps.sp64Distributions.length, is(nSpecies + 1));
		assertThat(pps.speciesIndices.length, is(nSpecies + 1));
		assertThat(pps.speciesNames.length, is(nSpecies + 1));
		assertThat(pps.yearsToBreastHeight.length, is(nSpecies + 1));
		assertThat(pps.getNSpecies(), is(nSpecies));

		assertThat(pps.basalAreas.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.basalAreas[i].length, is(UtilizationClass.values().length));
		}
		assertThat(pps.closeUtilizationVolumes.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.closeUtilizationVolumes[i].length, is(UtilizationClass.values().length));
		}
		assertThat(pps.cuVolumesMinusDecay.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.cuVolumesMinusDecay[i].length, is(UtilizationClass.values().length));
		}
		assertThat(pps.cuVolumesMinusDecayAndWastage.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.cuVolumesMinusDecayAndWastage[i].length, is(UtilizationClass.values().length));
		}

		assertThat(pps.loreyHeights.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.loreyHeights[i].length, is(2));
		}
		assertThat(pps.quadMeanDiameters.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.quadMeanDiameters[i].length, is(UtilizationClass.values().length));
		}
		assertThat(pps.treesPerHectare.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.treesPerHectare[i].length, is(UtilizationClass.values().length));
		}
		assertThat(pps.wholeStemVolumes.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(pps.wholeStemVolumes[i].length, is(UtilizationClass.values().length));
		}
	}

	@Test
	void testSetCopy() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank pps = new Bank(pLayer, Utils.getBec(polygon.getBiogeoclimaticZone(), controlMap), s -> true);

		verifyProcessingStateMatchesLayer(pps, pLayer);

		Bank ppsCopy = pps.copy();

		verifyProcessingStateMatchesLayer(ppsCopy, pLayer);
	}

	@Test
	void testRemoveSmallLayers() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank1 = new Bank(
				pLayer, Utils.getBec(polygon.getBiogeoclimaticZone(), controlMap),
				s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= 0.5
		);

		// the filter should have removed genus B (index 3) since it's ALL basal area is below 0.5
		assertThat(bank1.getNSpecies(), is(pLayer.getSpecies().size() - 1));
		assertThat(bank1.speciesIndices, is(new int[] { 0, 4, 5, 8, 15 }));

		Bank bank2 = new Bank(
				pLayer, Utils.getBec(polygon.getBiogeoclimaticZone(), controlMap),
				s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= 100.0
		);

		// the filter should have removed all genera.
		assertThat(bank2.getNSpecies(), is(0));
		assertThat(bank2.speciesIndices, is(new int[] { 0 }));
	}

	@Test
	void testCopyConstructor() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow();

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank pps = new Bank(pLayer, Utils.getBec(polygon.getBiogeoclimaticZone(), controlMap), s -> true);

		Bank ppsCopy = new Bank(pps, CopyMode.CopyAll);

		verifyProcessingStateMatchesLayer(ppsCopy, pLayer);
	}

	private void verifyProcessingStateMatchesLayer(Bank pps, VdypLayer layer) {

		List<String> sortedSpIndices = layer.getSpecies().keySet().stream().sorted().toList();

		for (int i = 0; i < sortedSpIndices.size(); i++) {

			int arrayIndex = i + 1;

			VdypSpecies species = layer.getSpecies().get(sortedSpIndices.get(i));
			verifyProcessingStateSpeciesMatchesSpecies(pps, arrayIndex, species);
		}

		verifyProcessingStateSpeciesUtilizationsMatchesUtilizations(pps, 0, layer);
	}

	private void verifyProcessingStateSpeciesUtilizationsMatchesUtilizations(
			Bank pps, int spIndex, VdypUtilizationHolder uh
	) {

		for (UtilizationClass uc : UtilizationClass.values()) {
			assertThat(pps.basalAreas[spIndex][uc.index + 1], is(uh.getBaseAreaByUtilization().get(uc)));
			assertThat(pps.closeUtilizationVolumes[spIndex][uc.index + 1], is(uh.getCloseUtilizationVolumeByUtilization().get(uc)));
			assertThat(pps.cuVolumesMinusDecay[spIndex][uc.index + 1], is(uh.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc)));
			assertThat(pps.cuVolumesMinusDecayAndWastage[spIndex][uc.index + 1], is(uh.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc)));
			if (uc.index <= 0) {
				assertThat(pps.loreyHeights[spIndex][uc.index + 1], is(uh.getLoreyHeightByUtilization().get(uc)));
			}
			assertThat(pps.quadMeanDiameters[spIndex][uc.index + 1], is(uh.getQuadraticMeanDiameterByUtilization().get(uc)));
			assertThat(pps.treesPerHectare[spIndex][uc.index + 1], is(uh.getTreesPerHectareByUtilization().get(uc)));
			assertThat(pps.wholeStemVolumes[spIndex][uc.index + 1], is(uh.getWholeStemVolumeByUtilization().get(uc)));
		}
	}

	private void verifyProcessingStateSpeciesMatchesSpecies(Bank pps, int index, VdypSpecies species) {
		VdypSite site = species.getSite().get();
		
		assertThat(pps.ageTotals[index], is(site.getAgeTotal().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
		assertThat(pps.dominantHeights[index], is(site.getHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
		assertThat(pps.siteIndices[index], is(site.getSiteIndex().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
		assertThat(pps.sp64Distributions[index], is(species.getSpeciesDistributions()));
		assertThat(pps.speciesIndices[index], is(species.getGenusIndex()));
		assertThat(pps.speciesNames[index], is(species.getGenus()));
		assertThat(pps.yearsToBreastHeight[index], is(site.getYearsToBreastHeight().orElse(VdypEntity.MISSING_FLOAT_VALUE)));
		
		verifyProcessingStateSpeciesUtilizationsMatchesUtilizations(
				pps, index, species
		);
	}
}
