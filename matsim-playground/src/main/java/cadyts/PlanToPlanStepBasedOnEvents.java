/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanToPlanStepBasedOnEvents.java
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

package cadyts;

import cadyts.demand.PlanBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.api.experimental.events.EventsManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
class PlanToPlanStepBasedOnEvents implements PlansTranslator, LinkLeaveEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler {

    private final Scenario scenario;

    private final Map<Id, PlanBuilder<Link>> driverAgents;

    @Inject
	PlanToPlanStepBasedOnEvents(final Scenario scenario, final EventsManager eventsManager) {
        eventsManager.addHandler(this);
        this.scenario = scenario;
        this.driverAgents = new HashMap<>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            driverAgents.put(person.getId(), new PlanBuilder<Link>());
        }
    }

    public final cadyts.demand.Plan<Link> getCadytsPlan(final Plan plan) {
        final cadyts.demand.Plan<Link> planSteps = driverAgents.get(plan.getPerson().getId()).getResult();
        return planSteps;
    }

    @Override
    public void reset(final int iteration) {
        this.driverAgents.clear();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            driverAgents.put(person.getId(), new PlanBuilder<Link>());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {

    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (!driverAgents.containsKey(event.getVehicleId())) return;
        PlanBuilder<Link> planBuilder = driverAgents.get(event.getVehicleId());
        Link link = this.scenario.getNetwork().getLinks().get(event.getLinkId());
        planBuilder.addTurn(link, (int) event.getTime());
    }

}
