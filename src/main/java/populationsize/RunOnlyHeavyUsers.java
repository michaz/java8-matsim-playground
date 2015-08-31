/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main2.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package populationsize;


import cdr.*;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.analysis.VolumesAnalyzerModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

import java.io.File;
import java.util.*;

public class RunOnlyHeavyUsers {

	public static void main(String[] args) {
		String input = args[0];
		String output = args[1];
		RunResource baseRun = new RunResource(input);
		final Scenario baseScenario = baseRun.getLastIteration().getExperiencedPlansAndNetwork();
		List<Person> persons = new ArrayList<>(baseScenario.getPopulation().getPersons().values());
		for (Person person : persons) {
			person.getCustomAttributes().put("phonerate", 50.0);
		}
		ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();
		ReplayEvents.Results results = ReplayEvents.run(
				baseScenario,
				baseRun.getLastIteration().getEventsFileName(),
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
		final Counts someCounts = filterCounts(input, allCounts);
		someCounts.setYear(2012);
		new CountsWriter(someCounts).write(output + "/calibration_counts.xml.gz");
	}

	static Counts filterCounts(String input, Counts allCounts) {
		Counts someCounts = new Counts();
		final Counts originalCounts = new Counts();
		new CountsReaderMatsimV1(originalCounts).parse(input + "/output_counts.xml");
		for (Map.Entry<Id<Link>, Count> entry : allCounts.getCounts().entrySet()) {
			if (originalCounts.getCounts().keySet().contains(entry.getKey())) {
				someCounts.getCounts().put(entry.getKey(), entry.getValue());
			}
		}
		return someCounts;
	}

}
