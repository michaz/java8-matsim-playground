package populationsize;

import cdr.CallBehavior;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;

class OnlyBasedOnPhonerateAttribute implements CallBehavior {
	@Override
	public boolean makeACall(ActivityEndEvent event) {
		return false;
	}

	@Override
	public boolean makeACall(ActivityStartEvent event) {
		return false;
	}

	@Override
	public boolean makeACallAtMorningAndNight(Id<Person> id) {
		return false;
	}
}
