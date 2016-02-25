package inject;

import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.mobsim.framework.Mobsim;

import javax.inject.Inject;

public class ControllerImpl implements ControlerI {

	@Inject
	PrepareForSim prepareForSim;
	@Inject
	Mobsim mobsim;
	@Inject
	PlansScoring scoring;
	@Inject
	PlansReplanning replanning;
	@Inject
	TerminationCriterion terminationCriterion;

	@Override
	public void run() {

	}
}
