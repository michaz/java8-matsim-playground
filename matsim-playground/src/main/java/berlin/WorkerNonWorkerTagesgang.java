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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.utils.objectattributes.ObjectAttributes;

import javax.inject.Inject;

public class WorkerNonWorkerTagesgang implements StartupListener, IterationStartsListener, IterationEndsListener {

    public static final String BASE_RUN_PERSON_ATTRIBUTES = "baseRunPersonAttributes";
    private final OutputDirectoryHierarchy controlerIO;
    private final Scenario scenario;
    private final EventsManager eventsManager;
    private BasicEventHandler eventHandler;
    private LegHistogram workersLegHistogram;
    private LegHistogram nonWorkersLegHistogram;

    @Inject
    WorkerNonWorkerTagesgang(OutputDirectoryHierarchy controlerIO, Scenario scenario, EventsManager eventsManager) {
        this.controlerIO = controlerIO;
        this.scenario = scenario;
        this.eventsManager = eventsManager;
    }

    @Override
    public void notifyStartup(StartupEvent event) {

    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() % 100 == 0) {
            ObjectAttributes baseRunPersonAttributes = (ObjectAttributes) scenario.getScenarioElement(BASE_RUN_PERSON_ATTRIBUTES);
            Predicate predicate = personId -> {
                Id<Person> originalId = resolvePerson(personId);
                return (Boolean) baseRunPersonAttributes.getAttribute(originalId.toString(), "isWorker");
            };
            final EventsManager workersEventsManager = EventsUtils.createEventsManager(scenario.getConfig());
            final EventsManager nonWorkersEventsManager = EventsUtils.createEventsManager(scenario.getConfig());
            eventHandler = new BasicEventHandler() {
                @Override
                public void handleEvent(Event event) {
                    if (event instanceof HasPersonId) {
                        Id<Person> personId = ((HasPersonId) event).getPersonId();
                        if (predicate.test(personId)) {
                            workersEventsManager.processEvent(event);
                        } else {
                            nonWorkersEventsManager.processEvent(event);
                        }
                    } else {
                        workersEventsManager.processEvent(event);
                        nonWorkersEventsManager.processEvent(event);
                    }
                }

                @Override
                public void reset(int iteration) {

                }
            };
            eventsManager.addHandler(eventHandler);
            workersLegHistogram = new LegHistogram(300);
            nonWorkersLegHistogram = new LegHistogram(300);
            workersEventsManager.addHandler(workersLegHistogram);
            nonWorkersEventsManager.addHandler(nonWorkersLegHistogram);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration() % 100 == 0) {
            workersLegHistogram.write(controlerIO.getIterationPath(event.getIteration()) + "/leghistogram-workers.txt");
            nonWorkersLegHistogram.write(controlerIO.getIterationPath(event.getIteration()) + "/leghistogram-nonworkers.txt");
            eventsManager.removeHandler(eventHandler);
        }
    }

    interface Predicate {
        boolean test(Id<Person> personId);
    }

    private static Id<Person> resolvePerson(Id<Person> personId) {
        String id = personId.toString();
        String originalId;
        if (id.startsWith("I"))
            originalId = id.substring(id.indexOf("_")+1);
        else
            originalId = id;
        return Id.createPersonId(originalId);
    }

}
