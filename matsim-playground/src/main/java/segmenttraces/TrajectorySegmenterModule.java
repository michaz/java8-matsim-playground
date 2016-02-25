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

import cdr.PopulationFromSightings;
import cdr.Sighting;
import cdr.Sightings;
import cdr.ZoneTracker;
import clones.CloneService;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TrajectorySegmenterModule extends AbstractModule {

    @Override
    public void install() {
        addPlanStrategyBinding("ReRealize").toProvider(TrajectorySegmenterProvider.class);
    }

    static class TrajectorySegmenterProvider implements Provider<PlanStrategy> {
        private Scenario scenario;
        private Sightings sightings;
        private ZoneTracker.LinkToZoneResolver zones;
        private CloneService cloneService;
		private Provider<TripRouter> tripRouterProvider;

		@Inject
		TrajectorySegmenterProvider(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver zones, CloneService cloneService, Provider<TripRouter> tripRouterProvider) {
            this.scenario = scenario;
            this.sightings = sightings;
            this.zones = zones;
            this.cloneService = cloneService;
			this.tripRouterProvider = tripRouterProvider;
		}

        @Override
        public PlanStrategy get() {
            PlanStrategyImpl planStrategy = new PlanStrategyImpl(new RandomPlanSelector<>());
            planStrategy.addStrategyModule(new TrajectorySegmenter(scenario, sightings, zones, cloneService));
            planStrategy.addStrategyModule(new ReRoute(scenario, tripRouterProvider));
            return planStrategy;
        }
    }

    static class TrajectorySegmenter extends AbstractMultithreadedModule {

		private Sightings sightings;
		private Scenario scenario;
		private ZoneTracker.LinkToZoneResolver zones;
		private CloneService cloneService;
		final Map<Id, List<Sighting>> dense;
		private final Predicate<List<Sighting>> isDense = trace -> trace.size() > 20;

		TrajectorySegmenter(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver zones, CloneService cloneService) {
			super(scenario.getConfig().global());
			this.sightings = sightings;
			this.scenario = scenario;
			this.zones = zones;
			this.cloneService = cloneService;
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
				Plan newPlan = PopulationFromSightings.createPlanWithSegmentedActivities(scenario, zones, originalTrace);
				plan.getPlanElements().clear();
				((PlanImpl) plan).copyFrom(newPlan);
				plan.getPlanElements().stream().filter(pe -> pe instanceof Leg).forEach(pe -> ((Leg) pe).setMode("car"));
			};
		}

	}
}
