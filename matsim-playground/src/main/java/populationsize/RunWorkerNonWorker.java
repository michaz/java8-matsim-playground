package populationsize;

import berlin.CountWorkers;
import cdr.*;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import java.io.File;
import java.util.*;

public class RunWorkerNonWorker {

	public static void main(String[] args) {
		String input = args[0];
		String output = args[1];
		RunResource baseRun = new RunResource(input);
		final Scenario baseScenario = baseRun.getLastIteration().getExperiencedPlansAndNetwork();
		final double lightUserRate = 5.0;
		List<Person> persons = new ArrayList<>(baseScenario.getPopulation().getPersons().values());
		Collections.shuffle(persons, new Random(42));
		ObjectAttributes personAttributes = baseScenario.getPopulation().getPersonAttributes();
		for (Person person : baseScenario.getPopulation().getPersons().values()) {
			if ((Boolean) personAttributes.getAttribute(person.getId().toString(), CountWorkers.IS_WORKER)) {
				personAttributes.putAttribute(person.getId().toString(), "phonerate", 50.0);
			} else {
				personAttributes.putAttribute(person.getId().toString(), "phonerate", lightUserRate);
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
		ObjectAttributesXmlWriter writer = new ObjectAttributesXmlWriter(personAttributes) ;
		writer.setPrettyPrint(true);
		writer.writeFile(output + "/personAttributes.xml.gz");
		new SightingsWriter(sightings).write(output + "/sightings.txt");
		final Counts allCounts = CompareMain.volumesToCounts(baseScenario.getNetwork(), groundTruthVolumes, 1.0);
		allCounts.setYear(2012);
		new CountsWriter(allCounts).write(output + "/all_counts.xml.gz");
		final Counts someCounts = filterCounts(allCounts);
		someCounts.setYear(2012);
		new CountsWriter(someCounts).write(output + "/calibration_counts.xml.gz");
	}

	private static Counts filterCounts(Counts<Link> allCounts) {
		Counts<Link> someCounts = new Counts<>();
		for (Map.Entry<Id<Link>, Count<Link>> entry : allCounts.getCounts().entrySet()) {
			if (Math.random() < 0.05) {
				someCounts.getCounts().put(entry.getKey(), entry.getValue());
			}
		}
		return someCounts;
	}

}
