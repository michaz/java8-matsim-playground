/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * BerlinRunUncongested2.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import javax.inject.Inject;

public class BerlinRunCongested3 {

	public static void main(String[] args) {
		double sample = Double.parseDouble(args[0]);
		String configFile = args[1];
		String outputDirectory = args[2];

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputDirectory);
		config.counts().setOutputFormat("kml");
		config.counts().setWriteCountsInterval(1);
		config.counts().setAverageCountsOverIterations(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.qsim().setFlowCapFactor(sample * config.qsim().getFlowCapFactor());
		config.qsim().setStorageCapFactor(sample * config.qsim().getFlowCapFactor());
		config.controler().setLastIteration(30);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.qsim().setRemoveStuckVehicles(false);
		config.planCalcScore().setWriteExperiencedPlans(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().getPersons().keySet().removeIf(id -> MatsimRandom.getRandom().nextDouble() > sample);

		final Controler controller = new Controler(scenario);
		controller.addControlerListener(new ShutdownListener() {

			@Inject
			OutputDirectoryHierarchy controlerIO;

			@Override
			public void notifyShutdown(ShutdownEvent shutdownEvent) {
				new CountsWriter((Counts) scenario.getScenarioElement(Counts.ELEMENT_NAME))
						.write(controlerIO.getOutputFilename("output_counts.xml"));
			}
		});
		controller.run();
	}

}