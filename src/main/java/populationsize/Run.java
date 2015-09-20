/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main2.java
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

package populationsize;


public class Run {

	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource congested = experiment.getRegime("uncongested3");
//		congested.getMultiRateRun("onlyheavyusers-noenrichment-segmentation").twoRatesRandom("50.0"); // meaning one rate
//        congested.getMultiRateRun("onlyheavyusers-noenrichment-segmentation").simulateRate("50.0", 1, 100.0);
//		congested.getMultiRateRun("onlyheavyusers-noenrichment-segmentation").persodisthisto();
	}
}
