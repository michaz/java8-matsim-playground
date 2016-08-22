/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CountWokers.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import javax.inject.Inject;


public class CountWorkers implements StartupListener {

    private static final Logger logger = Logger.getLogger(CountWorkers.class);
    public static final String IS_WORKER = "isWorker";

    @Inject Scenario scenario;
    @Inject Population population;

    @Override
    public void notifyStartup(StartupEvent event) {
        int nWorkers = 0;
        for (Person person : population.getPersons().values()) {
            if (isWorker(person)) {
                nWorkers++;
                population.getPersonAttributes().putAttribute(person.getId().toString(), IS_WORKER, true);
            } else {
                population.getPersonAttributes().putAttribute(person.getId().toString(), IS_WORKER, false);
            }
        }
        logger.info(String.format("Workers: %d, non-workers: %d", nWorkers, population.getPersons().size() - nWorkers));

        // We are the base run, so we add our person attributes as base run person attributes.
        scenario.addScenarioElement(WorkerNonWorkerTagesgang.BASE_RUN_PERSON_ATTRIBUTES, population.getPersonAttributes());
    }

    public static boolean isWorker(Person person) {
        for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
            if (pe instanceof Activity) {
                if (((Activity) pe).getType().equals("work")) {
                    return true;
                }
            }
        }
        return false;
    }

}
