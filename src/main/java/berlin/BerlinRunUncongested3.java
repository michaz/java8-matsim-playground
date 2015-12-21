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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

public class BerlinRunUncongested3 {

	public static void main(String[] args) {
		String configFile = args[0];
		String outputDirectory = args[1];

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputDirectory);
		config.counts().setOutputFormat("kml");
		config.counts().setWriteCountsInterval(1);
		config.counts().setAverageCountsOverIterations(1);
		config.controler().setLastIteration(0);
		config.qsim().setFlowCapFactor(100);
		config.qsim().setStorageCapFactor(100);
		config.qsim().setRemoveStuckVehicles(false);
		config.planCalcScore().setWriteExperiencedPlans(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controller = new Controler(scenario);
		controller.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controller.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent shutdownEvent) {
				new CountsWriter((Counts) scenario.getScenarioElement(Counts.ELEMENT_NAME))
						.write(controller.getInjector().getInstance(OutputDirectoryHierarchy.class).getOutputFilename("output_counts.xml"));
			}
		});
		controller.run();
	}

}