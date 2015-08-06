/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TrajectoryReRealizerModules.java
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

package segmenttraces;

import cdr.Sightings;
import cdr.ZoneTracker;
import clones.CloneService;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import javax.inject.Inject;
import javax.inject.Provider;

public class TrajectoryReEnricherAndSegmenterModule extends AbstractModule {

    @Override
    public void install() {
        addPlanStrategyBinding("ReEnrichAndSegment").toProvider(TrajectoryReEnricherProvider.class);
    }

    static class TrajectoryReEnricherProvider implements Provider<PlanStrategy> {
        private Scenario scenario;
        private Sightings sightings;
        private ZoneTracker.LinkToZoneResolver zones;
        private CloneService cloneService;

        @Inject
        TrajectoryReEnricherProvider(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver zones, CloneService cloneService) {
            this.scenario = scenario;
            this.sightings = sightings;
            this.zones = zones;
            this.cloneService = cloneService;
        }

        @Override
        public PlanStrategy get() {
            PlanStrategyImpl planStrategy = new PlanStrategyImpl(new RandomPlanSelector<>());
            planStrategy.addStrategyModule(new TrajectoryReEnricherAndSegmenter(scenario, sightings, zones, cloneService));
            planStrategy.addStrategyModule(new ReRoute(scenario));
            return planStrategy;
        }
    }
}
