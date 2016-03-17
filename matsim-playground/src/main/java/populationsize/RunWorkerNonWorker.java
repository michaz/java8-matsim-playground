package populationsize;

import cdr.*;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RunWorkerNonWorker {

	public static void main(String[] args) {
		String input = args[0];
		String output = args[1];
		RunResource baseRun = new RunResource(input);
		final Scenario baseScenario = baseRun.getLastIteration().getExperiencedPlansAndNetwork();
		final double lightUserRate = 5.0;
		List<Person> persons = new ArrayList<>(baseScenario.getPopulation().getPersons().values());
		Collections.shuffle(persons, new Random(42));
		for (Person person : baseScenario.getPopulation().getPersons().values()) {
			if (CountWorkers.isWorker(person)) {
				person.getCustomAttributes().put("phonerate", 50.0);
			} else {
				person.getCustomAttributes().put("phonerate", lightUserRate);
			}
		}
		ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
		ReplayEvents.Results results = ReplayEvents.run(
				baseScenario.getConfig(),
				baseRun.getLastIteration().getEventsFileName(),
				new ScenarioByInstanceModule(baseScenario),
				new EventsManagerModule(),
				new VolumesAnalyzerModule(),
				new CollectSightingsModule(),
				new CallBehaviorModule(new OnlyBasedOnPhonerateAttribute(), linkToZoneResolver));
		final Sightings sightings = results.get(Sightings.class);
		final VolumesAnalyzer groundTruthVolumes = results.get(VolumesAnalyzer.class);
		new File(output).mkdirs();
		new SightingsWriter(sightings).write(output + "/sightings.txt");
		final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
		allCounts.setYear(2012);
		new CountsWriter(allCounts).write(output + "/all_counts.xml.gz");
		final Counts someCounts = RunOnlyHeavyUsers.filterCounts(input, allCounts);
		someCounts.setYear(2012);
		new CountsWriter(someCounts).write(output + "/calibration_counts.xml.gz");
	}

}
