/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CadytsAndCloneScoringFunctionFactory.java
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

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import playground.mzilske.cadyts.CadytsScoring;
import playground.mzilske.clones.CloneService;

import javax.inject.Inject;

class CadytsAndCloneScoringFunctionFactory implements ScoringFunctionFactory {

    @Inject
    Config config;

    @Inject
    Scenario scenario;

    @Inject
    AnalyticalCalibrator cadyts;

    @Inject
    PlansTranslator ptStep;

    @Inject
    CloneService cloneService;

    private double cadytsweight = 1.0;
    private CharyparNagelScoringParameters params;

    @Override
    public ScoringFunction createNewScoringFunction(final Person person) {
//        if (this.params == null) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
//            this.params = new CharyparNagelScoringParameters(scenario.getConfig().planCalcScore());
//        }

        SumScoringFunction sumScoringFunction = new SumScoringFunction();
        CadytsScoring<Link> scoringFunction = new CadytsScoring<Link>(person.getSelectedPlan(), config, ptStep, cadyts);
        scoringFunction.setWeight(cadytsweight);
        sumScoringFunction.addScoringFunction(scoringFunction);

        // prior
        sumScoringFunction.addScoringFunction(cloneService.createNewScoringFunction(person));

        scenario.getConfig().planCalcScore();
//        sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));
//        sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(this.params, scenario.getNetwork()));
//        sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
//        sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));
        return sumScoringFunction;
    }

    public void setCadytsweight(double cadytsweight) {
        this.cadytsweight = cadytsweight;
    }


}
