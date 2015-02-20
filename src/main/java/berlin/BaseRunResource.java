/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RunResource.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.mzilske.ant2014.FileIO;
import playground.mzilske.ant2014.IterationResource;
import playground.mzilske.ant2014.StreamingOutput;

import java.io.IOException;
import java.io.PrintWriter;

public class BaseRunResource {

    public String getWd() {
        return wd;
    }

    private String wd;
	private String runId;

	public BaseRunResource(String wd, String runId) {
		this.wd = wd;
		this.runId = runId;
	}

	public IterationResource getIteration(int iteration) {
		return new IterationResource(wd + "/ITERS/it." + iteration + "/", runId, iteration);
	}
	
	public IterationResource getLastIteration() {
		Config config = getOutputConfig();
		return getIteration(config.controler().getLastIteration());
	}

	public Config getOutputConfig() {
		final Config config = ConfigUtils.loadConfig(wd + "/config.xml");
		return config;
	}
	
	public Scenario getConfigAndNetwork() {
		Scenario baseScenario = ScenarioUtils.createScenario(getOutputConfig());
		new MatsimNetworkReader(baseScenario).readFile("/Users/michaelzilske/shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz");
		return baseScenario;
	}

    public void cloneStatistics() {
        String filename = wd + "/clone-statistics.txt";
        final Config config = getOutputConfig();
        FileIO.writeToFile(filename, new StreamingOutput() {
            @Override
            public void write(PrintWriter pw) throws IOException {
                pw.printf("iteration\tnonemptyplans\n");
                for (int i=config.controler().getFirstIteration(); i<config.controler().getLastIteration(); i++) {
                    int nNonEmptyPlans = 0;
                    IterationResource iteration = getIteration(i);
                    Population plans = iteration.getPlans();
                    for (Person person : plans.getPersons().values()) {
                        Plan plan = person.getSelectedPlan();
                        if (!plan.getPlanElements().isEmpty()) {
                            nNonEmptyPlans++;
                        }

                    }
                    pw.printf("%d\t%d\n", i, nNonEmptyPlans);
                    pw.flush();
                }
            }
        });
    }
	
	public Scenario getScenario() {
		Scenario scenario = getConfigAndNetwork();
        new TransitScheduleReader(scenario).readFile("/Users/michaelzilske/shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz");
		new MatsimPopulationReader(scenario).readFile("/Users/michaelzilske/shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/scenario/plans.xml.gz");
//		new VehicleReaderV1(scenario.getVehicles()).readFile(wd + "/" + runPrefix() + "output_vehicles.xml.gz");
		return scenario;
	}

	private String runPrefix() {
		if (runId == null) {
			return "";
		} else {
			return runId + ".";
		}
	}

    public void writeToFile(String s, StreamingOutput so) {
        FileIO.writeToFile(wd + "/" + s, so);
    }

}
