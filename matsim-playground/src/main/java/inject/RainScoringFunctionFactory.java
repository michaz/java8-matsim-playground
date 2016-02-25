package inject;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class RainScoringFunctionFactory implements ScoringFunctionFactory {
	public RainScoringFunctionFactory(TripRouterImpl tripRouter) {

	}

	public RainScoringFunctionFactory() {

	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		return null;
	}
}
