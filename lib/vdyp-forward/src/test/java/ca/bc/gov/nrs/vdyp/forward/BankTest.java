package ca.bc.gov.nrs.vdyp.forward;

import static ca.bc.gov.nrs.vdyp.test.VdypMatchers.controlMapHasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
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
import ca.bc.gov.nrs.vdyp.forward.test.ForwardTestUtils;
import ca.bc.gov.nrs.vdyp.io.parse.common.ResourceParseException;
import ca.bc.gov.nrs.vdyp.model.GenusDefinitionMap;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.UtilizationClass;
import ca.bc.gov.nrs.vdyp.model.UtilizationVector;
import ca.bc.gov.nrs.vdyp.model.VdypEntity;
import ca.bc.gov.nrs.vdyp.model.VdypLayer;
import ca.bc.gov.nrs.vdyp.model.VdypSpecies;
import ca.bc.gov.nrs.vdyp.model.VdypUtilizationHolder;

class BankTest {

	private ForwardControlParser parser;
	private Map<String, Object> controlMap;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@BeforeEach
	void before() throws IOException, ResourceParseException {

		parser = new ForwardControlParser();
		controlMap = ForwardTestUtils.parse(parser, "VDYP.CTR");
		assertThat(controlMap, (Matcher) controlMapHasEntry(ControlKey.SP0_DEF, instanceOf(GenusDefinitionMap.class)));
	}

	@Test
	void testConstruction() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow(() -> new AssertionError("No polygons defined"));

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		int nSpecies = pLayer.getSpecies().size();

		assertThat(bank, notNullValue());
		assertThat(bank.yearsAtBreastHeight.length, is(nSpecies + 1));
		assertThat(bank.ageTotals.length, is(nSpecies + 1));
		assertThat(bank.dominantHeights.length, is(nSpecies + 1));
		assertThat(bank.percentagesOfForestedLand.length, is(nSpecies + 1));
		assertThat(bank.siteIndices.length, is(nSpecies + 1));
		assertThat(bank.sp64Distributions.length, is(nSpecies + 1));
		assertThat(bank.speciesIndices.length, is(nSpecies + 1));
		assertThat(bank.speciesNames.length, is(nSpecies + 1));
		assertThat(bank.yearsToBreastHeight.length, is(nSpecies + 1));
		assertThat(bank.getNSpecies(), is(nSpecies));

		assertThat(bank.basalAreas.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.basalAreas[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.closeUtilizationVolumes.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.closeUtilizationVolumes[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.cuVolumesMinusDecay.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.cuVolumesMinusDecay[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.cuVolumesMinusDecayAndWastage.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.cuVolumesMinusDecayAndWastage[i].length, is(UtilizationClass.values().length));
		}

		assertThat(bank.loreyHeights.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.loreyHeights[i].length, is(2));
		}
		assertThat(bank.quadMeanDiameters.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.quadMeanDiameters[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.treesPerHectare.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.treesPerHectare[i].length, is(UtilizationClass.values().length));
		}
		assertThat(bank.wholeStemVolumes.length, is(nSpecies + 1));
		for (int i = 0; i < nSpecies + 1; i++) {
			assertThat(bank.wholeStemVolumes[i].length, is(UtilizationClass.values().length));
		}
	}

	@Test
	void testSetCopy() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow(() -> new AssertionError("No polygons defined"));

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		pLayer = ForwardTestUtils.normalizeLayer(pLayer);
		verifyBankMatchesLayer(bank, pLayer);

		Bank ppsCopy = bank.copy();

		verifyBankMatchesLayer(ppsCopy, pLayer);
	}

	@Test
	void testRemoveSmallLayers() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow(() -> new AssertionError("No polygons defined"));

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank1 = new Bank(
				pLayer, polygon.getBiogeoclimaticZone(),
				s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= 0.5
		);

		// the filter should have removed genus B (index 3) since it's ALL basal area is below 0.5
		assertThat(bank1.getNSpecies(), is(pLayer.getSpecies().size() - 1));
		assertThat(bank1.speciesIndices, is(new int[] { 0, 4, 5, 8, 15 }));

		Bank bank2 = new Bank(
				pLayer, polygon.getBiogeoclimaticZone(),
				s -> s.getBaseAreaByUtilization().get(UtilizationClass.ALL) >= 100.0
		);

		// the filter should have removed all genera.
		assertThat(bank2.getNSpecies(), is(0));
		assertThat(bank2.speciesIndices, is(new int[] { 0 }));
	}

	@Test
	void testCopyConstructor() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow(() -> new AssertionError("No polygons defined"));

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		Bank bankCopy = new Bank(bank);

		pLayer = ForwardTestUtils.normalizeLayer(pLayer);
		verifyBankMatchesLayer(bankCopy, pLayer);
	}

	@Test
	void testLayerUpdate() throws IOException, ResourceParseException, ProcessingException {

		ForwardDataStreamReader reader = new ForwardDataStreamReader(controlMap);

		var polygon = reader.readNextPolygon().orElseThrow(() -> new AssertionError("No polygons defined"));

		VdypLayer pLayer = polygon.getLayers().get(LayerType.PRIMARY);
		assertThat(pLayer, notNullValue());

		Bank bank = new Bank(pLayer, polygon.getBiogeoclimaticZone(), s -> true);

		pLayer = ForwardTestUtils.normalizeLayer(pLayer);

		verifyBankMatchesLayer(bank, pLayer);

		UtilizationVector uv = pLayer.getBaseAreaByUtilization();
		float newValue = uv.get(UtilizationClass.ALL) + 1.0f;
		uv.set(UtilizationClass.ALL, newValue);
		pLayer.setBaseAreaByUtilization(uv);

		bank.refreshBank(pLayer);

		verifyBankMatchesLayer(bank, pLayer);
	}

	private void verifyBankMatchesLayer(Bank lps, VdypLayer layer) {

		List<Integer> sortedSpIndices = layer.getSpecies().values().stream().map(s -> s.getGenusIndex()).sorted()
				.toList();

		int arrayIndex = 1;
		for (int i = 0; i < sortedSpIndices.size(); i++) {
			VdypSpecies genus = layer.getSpeciesByIndex(sortedSpIndices.get(i));

			verifyBankSpeciesMatchesSpecies(lps, arrayIndex, genus);

			verifyBankUtilizationsMatchesUtilizations(lps, arrayIndex, genus);

			arrayIndex += 1;
		}

		verifyBankUtilizationsMatchesUtilizations(lps, 0, layer);
	}

	private void verifyBankUtilizationsMatchesUtilizations(Bank lps, int spIndex, VdypUtilizationHolder u) {
		for (UtilizationClass uc : UtilizationClass.values()) {
			assertThat(lps.basalAreas[spIndex][uc.index + 1], is(u.getBaseAreaByUtilization().get(uc)));
			assertThat(
					lps.closeUtilizationVolumes[spIndex][uc.index + 1],
					is(u.getCloseUtilizationVolumeByUtilization().get(uc))
			);
			assertThat(
					lps.cuVolumesMinusDecay[spIndex][uc.index + 1],
					is(u.getCloseUtilizationVolumeNetOfDecayByUtilization().get(uc))
			);
			assertThat(
					lps.cuVolumesMinusDecayAndWastage[spIndex][uc.index + 1],
					is(u.getCloseUtilizationVolumeNetOfDecayAndWasteByUtilization().get(uc))
			);
			if (uc.index <= 0) {
				assertThat(lps.loreyHeights[spIndex][uc.index + 1], is(u.getLoreyHeightByUtilization().get(uc)));
			}
			assertThat(
					lps.quadMeanDiameters[spIndex][uc.index + 1], is(u.getQuadraticMeanDiameterByUtilization().get(uc))
			);
			assertThat(lps.treesPerHectare[spIndex][uc.index + 1], is(u.getTreesPerHectareByUtilization().get(uc)));
			assertThat(lps.wholeStemVolumes[spIndex][uc.index + 1], is(u.getWholeStemVolumeByUtilization().get(uc)));
		}
	}

	private void verifyBankSpeciesMatchesSpecies(Bank bank, int index, VdypSpecies species) {
		assertThat(bank.sp64Distributions[index], is(species.getSp64DistributionSet()));
		assertThat(bank.speciesIndices[index], is(species.getGenusIndex()));
		assertThat(bank.speciesNames[index], is(species.getGenus()));

		species.getSite().ifPresentOrElse(site -> {
			assertThat(bank.yearsAtBreastHeight[index], is(site.getYearsAtBreastHeight().get()));
			assertThat(bank.ageTotals[index], is(site.getAgeTotal().get()));
			assertThat(bank.dominantHeights[index], is(site.getHeight().get()));
			assertThat(bank.siteIndices[index], is(site.getSiteIndex().get()));
			assertThat(bank.yearsToBreastHeight[index], is(site.getYearsToBreastHeight().get()));
			site.getSiteCurveNumber().ifPresentOrElse(scn -> {
				assertThat(bank.siteCurveNumbers[index], is(scn));
			}, () -> {
				assertThat(bank.siteCurveNumbers[index], is(VdypEntity.MISSING_INTEGER_VALUE));
			});
			assertThat(bank.speciesNames[index], is(site.getSiteGenus()));
		}, () -> {
			assertThat(bank.yearsAtBreastHeight[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.ageTotals[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.dominantHeights[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.siteIndices[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.yearsToBreastHeight[index], is(VdypEntity.MISSING_FLOAT_VALUE));
			assertThat(bank.siteCurveNumbers[index], is(VdypEntity.MISSING_INTEGER_VALUE));
		});
	}
}
