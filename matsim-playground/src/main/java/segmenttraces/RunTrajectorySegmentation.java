package segmenttraces;

import cdr.Sighting;
import cdr.Sightings;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import populationsize.ExperimentResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class RunTrajectorySegmentation {

	public void start() throws IOException {
		try (PrintWriter fw = new PrintWriter("output/histogram.txt"); PrintWriter fw2 = new PrintWriter("output/scatter.txt")) {
			final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
			final RegimeResource regime = experiment.getRegime("uncongested3");
			MultiRateRunResource multiRateRun = regime.getMultiRateRun("realcountlocations100.0");
			Sightings sightings = multiRateRun.getSightings("5");
			Scenario scenario = multiRateRun.getBaseRun().getConfigAndNetwork();

			HelloLatitude helloLatitude = new HelloLatitude(scenario.getNetwork());
			for (Map.Entry<Id, List<Sighting>> sightingList : sightings.getSightingsPerPerson().entrySet()) {
				Person p = scenario.getPopulation().getFactory().createPerson(sightingList.getKey());
				Plan plan = helloLatitude.getLatitude(sightingList.getValue());
				p.addPlan(plan);
				scenario.getPopulation().addPerson(p);
			}

			Scenario baseScenario = multiRateRun.getBaseRun().getOutputScenario();
			for (Map.Entry<Id<Person>, ? extends Person> person : baseScenario.getPopulation().getPersons().entrySet()) {
				long nRealActivities = person.getValue().getSelectedPlan().getPlanElements()
						.stream().filter(pe -> pe instanceof Activity).count();
				Person reconstructedPerson = scenario.getPopulation().getPersons().get(person.getKey());
				long nReconstructedActivities = reconstructedPerson == null ? 0 :
						reconstructedPerson.getSelectedPlan().getPlanElements()
								.stream().filter(pe -> pe instanceof Activity).count();
				List<Sighting> personSightings = sightings.getSightingsPerPerson().get(person.getKey());
				long nSightings = personSightings == null ? 0 : personSightings.size();
				fw2.printf("%d %d %d\n",nRealActivities, nReconstructedActivities, nSightings);
			}
		}
	}

	public static void main(String[] args) throws IOException {
		new RunTrajectorySegmentation().start();
	}

}
