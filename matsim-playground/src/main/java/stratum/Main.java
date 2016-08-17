/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * main.java
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

package stratum;

import cadyts.CadytsModule;
import cdr.*;
import clones.ClonesConfigGroup;
import clones.ClonesModule;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.counts.Count;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import populationsize.CadytsAndCloneScoringFunctionFactory;
import util.IterationSummaryFileControlerListener;
import util.StreamingOutput;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {

    public static void main(String[] args) {
        run("output/illustrative/8-count", Arrays.asList(8));
        run("output/illustrative/18-count", Arrays.asList(18));
    }

    private static void run(final String outputDirectory, List<Integer> countHours) {
        AbstractModule phoneModule = new AbstractModule() {
            @Override
            public void install() {
                bind(ZoneTracker.LinkToZoneResolver.class).to(MyLinkToZoneResolver.class);
                bind(CallBehavior.class).to(MyCallBehavior.class);
            }
        };

        Scenario groundTruth = new OneWorkplaceOneStratumUnderestimated().get();
        groundTruth.getConfig().controler().setOutputDirectory(outputDirectory + "-orig");
        groundTruth.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        Controler controler = new Controler(groundTruth);
        controler.setModules(
                new ControlerDefaultsModule(),
                new CollectSightingsModule(),
                phoneModule);
        controler.run();

        final VolumesAnalyzer groundTruthVolumes = controler.getVolumes();

        final Counts allCounts = CompareMain.volumesToCounts(groundTruth.getNetwork(), groundTruthVolumes, 1.0);
        final Counts calibrationCounts = filterCounts(allCounts, countHours);

        Scenario cdrScenario = new ScenarioReconstructor(groundTruth.getNetwork(), (Sightings) groundTruth.getScenarioElement("sightings"), new MyLinkToZoneResolver()).get();
        cdrScenario.addScenarioElement(Counts.ELEMENT_NAME, allCounts);
        cdrScenario.addScenarioElement("calibrationCounts", calibrationCounts);
        cdrScenario.getConfig().controler().setOutputDirectory(outputDirectory);
        cdrScenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(cdrScenario.getConfig(), ClonesConfigGroup.NAME, ClonesConfigGroup.class);
        clonesConfig.setCloneFactor(2.0);

        Controler controler2 = new Controler(cdrScenario);
        controler2.setModules(
                new ControlerDefaultsModule(),
                phoneModule,
                new CadytsModule(),
                new ClonesModule(),
                new AbstractModule() {
                    @Override
                    public void install() {
                        addControlerListenerBinding().toProvider(MyControlerListenerProvider.class);
                    }
                });
        CadytsAndCloneScoringFunctionFactory scoringFunctionFactory = new CadytsAndCloneScoringFunctionFactory();
        controler2.setScoringFunctionFactory(scoringFunctionFactory);
        controler2.run();
    }

    private static Counts filterCounts(Counts<Link> allCounts, List<Integer> countHours) {
        Counts<Link> someCounts = new Counts();
        for (Map.Entry<Id<Link>, Count<Link>> entry : allCounts.getCounts().entrySet()) {
            String linkId = entry.getKey().toString();
            if (linkId.equals("1") || linkId.equals("21")) {
                Count count = someCounts.createAndAddCount(Id.create(linkId, Link.class), "wurst");
                for (Map.Entry<Integer, Volume> volume : entry.getValue().getVolumes().entrySet()) {
                    if (countHours.contains(volume.getKey())) {
                        count.createVolume(volume.getKey(), volume.getValue().getValue());
                    }
                }
            }
        }
        return someCounts;
    }

	private static class MyControlerListenerProvider implements Provider<ControlerListener> {
		@Inject
		Scenario scenario;
		@Inject
		OutputDirectoryHierarchy controlerIO;
		@Inject
		VolumesAnalyzer volumesAnalyzer;
		@Override
		public ControlerListener get() {
			Map<String, IterationSummaryFileControlerListener.Writer> things = new HashMap<>();
			things.put("linkstats.txt", new IterationSummaryFileControlerListener.Writer() {
				@Override
				public StreamingOutput notifyStartup(StartupEvent event) {
					return new StreamingOutput() {
						@Override
						public void write(PrintWriter pw) throws IOException {
							pw.printf("%s\t%s\t%s\t%s\t%s\n",
									"iteration",
									"link",
									"hour",
									"sim.volume",
									"count.volume");
						}
					};
				}

				@Override
				public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
					CountsComparisonAlgorithm countsComparisonAlgorithm = new CountsComparisonAlgorithm(volumesAnalyzer, (Counts) scenario.getScenarioElement("counts"), scenario.getNetwork(), 1.0);
					countsComparisonAlgorithm.run();
					final List<CountSimComparison> comparison = countsComparisonAlgorithm.getComparison();
					return new StreamingOutput() {
						@Override
						public void write(PrintWriter pw) throws IOException {
							for (CountSimComparison countLink : comparison) {
								pw.printf("%d\t%s\t%d\t%f\t%f\n",
										event.getIteration(),
										countLink.getId().toString(),
										countLink.getHour(),
										countLink.getSimulationValue(),
										countLink.getCountValue());
							}
						}
					};
				}
			});
			return new IterationSummaryFileControlerListener(controlerIO, things);
		}
	}
}
