/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CloneServiceImpl.java
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

package clones;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.SumScoringFunction;

import javax.inject.Inject;

class CloneServiceImpl implements CloneService {

    private final double clonefactor;
    private final Population population;
    private final double beta;

    @Inject
    CloneServiceImpl(Config config, Population population) {
        this.clonefactor = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class).getCloneFactor();
        this.population = population;
        this.beta = config.planCalcScore().getBrainExpBeta();
    }

    @Override
    public Id<Person> resolveParentId(Id<Person> cloneId) {
        String id = cloneId.toString();
        String originalId;
        if (id.startsWith("I"))
            originalId = id.substring(id.indexOf("_") + 1);
        else
            originalId = id;
        return Id.create(originalId, Person.class);
    }

    @Override
    public SumScoringFunction.BasicScoring createNewScoringFunction(Person person) {
        return new CloneScoring(person);
    }

    @Override
    public boolean isActive(Id<Person> personId) {
        return !ClonesControlerListener.EMPTY_CLONE_PLAN.equals(population.getPersons().get(personId).getSelectedPlan().getType());
    }

    private class CloneScoring implements SumScoringFunction.BasicScoring {
        private Person person;

        public CloneScoring(Person person) {
            this.person = person;
        }

        @Override
        public void finish() {}

        @Override
        public double getScore() {
            if (clonefactor > 1.0 && !ClonesControlerListener.EMPTY_CLONE_PLAN.equals(person.getSelectedPlan().getType())) {
                return scoreOffset();
            } else {
                return 0.0;
            }
        }

        private double scoreOffset() {
            long nEmptyClonePlans = person.getPlans().stream().filter(p -> ClonesControlerListener.EMPTY_CLONE_PLAN.equals(p.getType())).count();
            return - Math.log((clonefactor - 1.0) * (person.getPlans().size() - (double) nEmptyClonePlans) / (double) nEmptyClonePlans) / beta;
        }

    }

}
