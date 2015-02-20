/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CharyparNagelCadytsScoringFunctionFactory.java
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

package bluetoothpaper;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import playground.mzilske.cadyts.CadytsScoring;
import playground.mzilske.clones.CloneService;

import javax.inject.Inject;

class CharyparNagelCadytsScoringFunctionFactory implements ScoringFunctionFactory {

    @Inject
    Config config;

    @Inject
    CloneService cloneService;

    @Inject
    AnalyticalCalibrator cadyts;

    @Inject
    PlansTranslator ptStep;

    @Override
    public ScoringFunction createNewScoringFunction(final Person person) {
        SumScoringFunction sumScoringFunction = new SumScoringFunction();
        CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, ptStep, cadyts);
        sumScoringFunction.addScoringFunction(scoringFunction);

        // prior
        sumScoringFunction.addScoringFunction(cloneService.createNewScoringFunction(person));
        return sumScoringFunction;
    }

}
