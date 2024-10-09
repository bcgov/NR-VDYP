package ca.bc.gov.nrs.vdyp.forward;

import java.util.Map;
import java.util.Optional;

import ca.bc.gov.nrs.vdyp.application.ProcessingException;
import ca.bc.gov.nrs.vdyp.application.VdypApplicationIdentifier;
import ca.bc.gov.nrs.vdyp.common.ComputationMethods;
import ca.bc.gov.nrs.vdyp.common.EstimationMethods;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMap;
import ca.bc.gov.nrs.vdyp.forward.controlmap.ForwardResolvedControlMapImpl;
import ca.bc.gov.nrs.vdyp.model.BecDefinition;
import ca.bc.gov.nrs.vdyp.model.LayerType;
import ca.bc.gov.nrs.vdyp.model.VdypPolygon;

public class ForwardProcessingState {

	/** The control map defining the context of the execution */
	public final ForwardResolvedControlMap fcm;

	/** The estimators instance used by this engine */
	final EstimationMethods estimators;

	/** The computation instance used by this engine */
	final ComputationMethods computers;

	/** The polygon on which the Processor is currently operating */
	private VdypPolygon polygon;

	/** The processing state of the primary layer of <code>polygon</code> */
	private LayerProcessingState plps;

	/** The processing state of the veteran layer of <code>polygon</code> */
	private Optional<LayerProcessingState> vlps;

	public ForwardProcessingState(Map<String, Object> controlMap) throws ProcessingException {
		this.fcm = new ForwardResolvedControlMapImpl(controlMap);
		this.estimators = new EstimationMethods(this.fcm);
		this.computers = new ComputationMethods(estimators, VdypApplicationIdentifier.VDYP_FORWARD);
	}

	public void setPolygon(VdypPolygon polygon) throws ProcessingException {

		this.polygon = polygon;

		this.plps = new LayerProcessingState(this, polygon.getLayers().get(LayerType.PRIMARY));
		if (polygon.getLayers().containsKey(LayerType.VETERAN)) {
			this.vlps = Optional.of(new LayerProcessingState(this, polygon.getLayers().get(LayerType.VETERAN)));
		} else {
			this.vlps = Optional.empty();
		}
	}

	/** @return the current polygon */
	public VdypPolygon getCurrentPolygon() {
		return polygon;
	}

	/** @return the compact form of the current polygon's identifier. Shortcut. */
	public String getCompactPolygonIdentifier() {
		return polygon.getPolygonIdentifier().toStringCompact();
	}

	/** @return the starting year of the current polygon. Shortcut. */
	public int getCurrentStartingYear() {
		return polygon.getPolygonIdentifier().getYear();
	}

	/** @return the bec zone of the current polygon. Shortcut. */
	public BecDefinition getCurrentBecZone() {
		return polygon.getBiogeoclimaticZone();
	}

	public LayerProcessingState getPrimaryLayerProcessingState() {
		return plps;
	}

	public Optional<LayerProcessingState> getVeteranLayerProcessingState() {
		return vlps;
	}

	public VdypPolygon updatePolygon() {

		polygon.getLayers().put(LayerType.PRIMARY, plps.updateLayerFromBank());
		vlps.ifPresent(vlps -> polygon.getLayers().put(LayerType.VETERAN, vlps.updateLayerFromBank()));

		return polygon;
	}
}