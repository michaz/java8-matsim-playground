/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WorkerNonWorkerTagesgang.java
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

package berlin;

import org.matsim.analysis.LegHistogram;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import playground.mzilske.ant2014.IterationResource;
import playground.mzilske.ant2014.RunResource;

public class WorkerNonWorkerTagesgang {

    public static final String RATE = "5";

    interface Predicate {
        boolean test(Id<Person> personId);
    }

    public static void main(String[] args) {
        MultiRateRunResource experiment = new MultiRateRunResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin2-original-count-locations");
        final BaseRunResource baseRun = experiment.getBaseRun();
        final Scenario baseScenario = baseRun.getScenario();
        final RunResource run = experiment.getRateRun(RATE, "3_unc");



        Predicate predicate = personId -> isWorker(personId, baseScenario);

//        doIt(predicate, baseRun.getOutputConfig(), baseRun.getLastIteration());


        Config outputConfig = run.getOutputConfig();

        doIt(predicate, outputConfig, run.getLastIteration());
        doIt(predicate, outputConfig, run.getIteration(0));
    }

    private static void doIt(final Predicate isHeavyPhoner, Config outputConfig, IterationResource iteration) {
        final EventsManager workersEventsManager = EventsUtils.createEventsManager(outputConfig);
        final EventsManager nonWorkersEventsManager = EventsUtils.createEventsManager(outputConfig);
        final EventsManager allEventsManager = EventsUtils.createEventsManager(outputConfig);


        EventsManager eventsManager = EventsUtils.createEventsManager(outputConfig);
        eventsManager.addHandler(new BasicEventHandler() {
            @Override
            public void handleEvent(Event event) {
                if (event instanceof HasPersonId) {
                    Id<Person> personId = ((HasPersonId) event).getPersonId();
                    if (isTransitDriver(personId)) {

                    } else if (isHeavyPhoner.test(personId)) {
                        workersEventsManager.processEvent(event);
                        allEventsManager.processEvent(event);
                    } else {
                        nonWorkersEventsManager.processEvent(event);
                        allEventsManager.processEvent(event);
                    }
                } else {
                    workersEventsManager.processEvent(event);
                    nonWorkersEventsManager.processEvent(event);
                    allEventsManager.processEvent(event);
                }
            }

            private boolean isTransitDriver(Id<Person> personId) {
                return (personId.toString().startsWith("pt"));
            }

            @Override
            public void reset(int iteration) {

            }
        });

        LegHistogram workersLegHistogram = new LegHistogram(300);
        LegHistogram nonWorkersLegHistogram = new LegHistogram(300);
        LegHistogram allLegHistogram = new LegHistogram(300);
        workersEventsManager.addHandler(workersLegHistogram);
        nonWorkersEventsManager.addHandler(nonWorkersLegHistogram);
        allEventsManager.addHandler(allLegHistogram);

        new MatsimEventsReader(eventsManager).readFile(iteration.getEventsFileName());
        workersLegHistogram.write(iteration.getWd() + "/leghistogram-workers.txt");
        nonWorkersLegHistogram.write(iteration.getWd() + "/leghistogram-nonworkers.txt");
        allLegHistogram.write(iteration.getWd() + "leghistogram-all.txt");
    }

    private static boolean isWorker(Id<Person> personId, Scenario baseRun) {
        Id<Person> originalId = resolvePerson(personId);
        Person person = baseRun.getPopulation().getPersons().get(originalId);
        return playground.mzilske.populationsize.CountWorkers.isWorker(person);
    }

    private static Id<Person> resolvePerson(Id<Person> personId) {
        String id = personId.toString();
        String originalId;
        if (id.startsWith("I"))
            originalId = id.substring(id.indexOf("_") + 1);
        else
            originalId = id;
        return Id.createPersonId(originalId);
    }

}
