/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Main.java
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

import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
//        MultiRateRunResource experiment = new MultiRateRunResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin2-original-count-locations");
        MultiRateRunResource experiment = new MultiRateRunResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin2");

//        experiment.getBaseRun().getLastIteration().postExperiencedPlans();
        Stream.<String>builder().add("0").add("5").build().parallel().forEach(experiment::twoRates);
//        Stream.<String>builder().add("0").add("5").build().parallel().forEach(r -> experiment.simulateRate(r, 3, 1.0));
//        experiment.simulateRate("5", 3, 100.0);
//        experiment.simulateRate("0", 3, 100.0);

    }
}
