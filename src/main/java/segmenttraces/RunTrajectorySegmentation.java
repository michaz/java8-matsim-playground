package segmenttraces;

import cdr.Sighting;
import cdr.Sightings;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import populationsize.ExperimentResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunTrajectorySegmentation {

	public void start() throws IOException {
		try (FileWriter fw = new FileWriter("output/histogram.txt"); FileWriter fw2 = new FileWriter("output/scatter.txt")) {
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
		}
	}

	private void compareWithTruth(Sightings sightings) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile("/Users/michaelzilske/shared-svn/studies/countries/de/berlin/plans/baseplan_car_only.xml.gz");
		System.out.println(scenario.getPopulation().getPersons().size());
		System.out.println(sightings.getSightingsPerPerson().size());
	}


	public static void main(String[] args) throws IOException {
		new RunTrajectorySegmentation().start();
	}

}
