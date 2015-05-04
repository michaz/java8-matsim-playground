/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TrajectoryReRealizer.java
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

package enrichtraces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.mzilske.cdr.PopulationFromSightings;
import playground.mzilske.cdr.Sighting;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.cdr.ZoneTracker;
import playground.mzilske.clones.CloneService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

class TrajectoryReEnricher extends AbstractMultithreadedModule {

    private final DistanceCalculator distanceCalculator;
    private Sightings sightings;
    private Scenario scenario;
    private ZoneTracker.LinkToZoneResolver zones;
    private CloneService cloneService;
    final Map<Id, List<Sighting>> dense;
    private final Predicate<List<Sighting>> isDense = trace -> trace.size() > 20;


    TrajectoryReEnricher(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver zones, CloneService cloneService) {
        super(scenario.getConfig().global());
        this.sightings = sightings;
        this.scenario = scenario;
        this.zones = zones;
        this.cloneService = cloneService;
        this.distanceCalculator = new DistanceCalculator(scenario.getNetwork());
        this.dense = new HashMap<>();
        sightings.getSightingsPerPerson().entrySet().stream().filter(entry -> isDense.test(entry.getValue()))
                .forEach(entry -> dense.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public PlanAlgorithm getPlanAlgoInstance() {
        return plan -> {
            Id personId = plan.getPerson().getId();
            Id originalPersonId = cloneService.resolveParentId(personId);
            List<Sighting> originalTrace = sightings.getSightingsPerPerson().get(originalPersonId);
            Plan newPlan;
            if (isDense.test(originalTrace)) {
                newPlan = PopulationFromSightings.createPlanWithRandomEndTimesInPermittedWindow(scenario, zones, originalTrace);
            } else {
                ArrayList<Sighting> newTrace = new ArrayList<>(originalTrace);
                ArrayList<Map.Entry<Id, List<Sighting>>> denseTraces = new ArrayList<>(dense.entrySet());
                distanceCalculator.sortDenseByProximityToSparse(newTrace, denseTraces);
                List<Sighting> wellFittingDenseTrace = denseTraces.get(0).getValue();
                new TrajectoryEnricher(distanceCalculator, newTrace, wellFittingDenseTrace).drehStreckAll();
                newPlan = PopulationFromSightings.createPlanWithRandomEndTimesInPermittedWindow(scenario, zones, newTrace);
            }
            plan.getPlanElements().clear();
            ((PlanImpl) plan).copyFrom(newPlan);
            plan.getPlanElements().stream().filter(pe -> pe instanceof Leg).forEach(pe -> ((Leg) pe).setMode("car"));
        };
    }

}
