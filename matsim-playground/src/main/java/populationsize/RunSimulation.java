package populationsize;

import cadyts.CadytsModule;
import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.SimResults;
import cdr.*;
import clones.ClonesConfigGroup;
import clones.ClonesModule;
import com.google.inject.Key;
import com.google.inject.name.Names;
import enrichtraces.TrajectoryReEnricherModule;
import enrichtraces.TrajectoryReEnricherMonitoring;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EnumMap;

public class RunSimulation {

	enum HistogramBin {
		B0, B20000, B40000, B60000, B80000, B100000, B120000, B140000, B160000, B180000, B200000, B220000, B240000, B260000;
	}


	public static void main(String[] args) {
		String alternative = args[0];
		String baseRunDir = args[1];
		String sightingsDir = args[2];
		String output = args[3];

		RunResource baseRun = new RunResource(baseRunDir);

		final double cadytsWeight = 100.0;
		int lastIteration = 100;
		double cloneFactor;
		if (alternative.equals("full-procedure") || alternative.equals("full-procedure-with-histogram")) {
			cloneFactor = 1.0;
		} else if (alternative.equals("clone")) {
			cloneFactor = 3.0;
		} else {
			throw new RuntimeException();
		}

		final Config config = MultiRateRunResource.phoneConfig(lastIteration, cloneFactor);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output);

		Scenario baseScenario = baseRun.getConfigAndNetwork();
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		scenario.setNetwork(baseScenario.getNetwork());

		final Sightings allSightings = new SightingsImpl();
		new SightingsReader(allSightings).read(IOUtils.getInputStream(sightingsDir + "/sightings.txt"));
		final ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();

		PopulationFromSightings.createPopulationWithRandomRealization(scenario, allSightings, linkToZoneResolver);

		final Counts allCounts = new Counts();
		new CountsReaderMatsimV1(allCounts).parse(sightingsDir + "/all_counts.xml.gz");
		final Counts someCounts = new Counts();
		new CountsReaderMatsimV1(someCounts).parse(sightingsDir + "/calibration_counts.xml.gz");

		scenario.addScenarioElement(Counts.ELEMENT_NAME, allCounts);
		scenario.addScenarioElement("calibrationCounts", someCounts);

		ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class);
		clonesConfig.setCloneFactor(cloneFactor);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new CadytsModule());
				install(new ClonesModule());
				if (alternative.equals("full-procedure") || alternative.equals("full-procedure-with-histogram")) {
					install(new TrajectoryReEnricherModule());
				} else if (alternative.equals("clone")) {
					install(new TrajectoryReRealizerModule());
				} else {
					throw new RuntimeException();
				}
				install(new AbstractModule() {
					@Override
					public void install() {
						bind(ZoneTracker.LinkToZoneResolver.class).toInstance(linkToZoneResolver);
						bind(Sightings.class).toInstance(allSightings);
					}
				});
				addControlerListenerBinding().toInstance((IterationStartsListener) startupEvent -> {
					if (startupEvent.getIteration() == 0) {
//						scenario.getPopulation().getPersons().values().forEach(p -> {
//							p.setSelectedPlan(null);
//							p.getPlans().clear();
//						});
						PlanStrategy reEnrich = controler.getInjector().getInstance(Key.get(PlanStrategy.class, Names.named("ReRealize")));
						reEnrich.init(controler.getInjector().getInstance(ReplanningContext.class));
						scenario.getPopulation().getPersons().values().forEach(reEnrich::run);
						reEnrich.finish();
					}
				});
			}
		});
		if (alternative.equals("full-procedure-with-histogram")) {
			controler.addControlerListener((StartupListener) startupEvent -> {
				AnalyticalCalibrator<HistogramBin> calibrator = new AnalyticalCalibrator<>(startupEvent.getServices().getConfig().controler().getOutputDirectory() + "/cadyts-histogram.txt", MatsimRandom.getRandom().nextLong(), 24*60*60);
				calibrator.setStatisticsFile(startupEvent.getServices().getControlerIO().getOutputFilename("histogram-calibration-stats.txt"));
				calibrator.addMeasurement(HistogramBin.B0, 0, 24*60*60, 0.1 * 6308, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B20000, 0, 24*60*60, 0.1 *5601, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B40000, 0, 24*60*60, 0.1 *2930, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B60000, 0, 24*60*60, 0.1 *1510, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B80000, 0, 24*60*60, 0.1 *757, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B100000, 0, 24*60*60, 0.1 *427, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B120000, 0, 24*60*60, 0.1 *278, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B140000, 0, 24*60*60, 0.1 *143, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B160000, 0, 24*60*60, 0.1 *119, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B180000, 0, 24*60*60, 0.1 *80, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B200000, 0, 24*60*60, 0.1 *63, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B220000, 0, 24*60*60, 0.1 *53, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B240000, 0, 24*60*60, 0.1 *27, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B260000, 0, 24*60*60, 0.1 *20, SingleLinkMeasurement.TYPE.COUNT_VEH);
				startupEvent.getServices().addControlerListener((BeforeMobsimListener) beforeMobsimEvent -> {
					for (Person person : beforeMobsimEvent.getServices().getScenario().getPopulation().getPersons().values()) {
						double totalPlannedDistance = 0.0;
						PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
						for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
							if (planElement instanceof Leg) {
								totalPlannedDistance += ((Leg) planElement).getRoute().getDistance();
							}
						}
						if (totalPlannedDistance < 260000) {
							planBuilder.addTurn(HistogramBin.values()[(int) (totalPlannedDistance / 20000.0)], 0);
						}
						calibrator.addToDemand(planBuilder.getResult());
					}
				});
				TObjectDoubleHashMap<Id<Person>> distances = new TObjectDoubleHashMap<>();
				startupEvent.getServices().getInjector().getInstance(EventsToLegs.class).addLegHandler(new EventsToLegs.LegHandler() {
					@Override
					public void handleLeg(PersonExperiencedLeg personExperiencedLeg) {
						double distance = personExperiencedLeg.getLeg().getRoute().getDistance();
						distances.adjustOrPutValue(personExperiencedLeg.getAgentId(), distance, distance);
					}
				});
				startupEvent.getServices().addControlerListener((AfterMobsimListener) afterMobsimEvent -> {
					EnumMap<HistogramBin, Integer> frequencies = new EnumMap<>(HistogramBin.class);
					for (HistogramBin bin : HistogramBin.values()) {
						frequencies.put(bin, 0);
					}
					distances.forEachValue(v -> {
						if (v < 260000) {
							HistogramBin bin = HistogramBin.values()[(int) (v / 20000.0)];
							frequencies.put(bin, frequencies.get(bin) + 1);
						}
						return true;
					});
					calibrator.afterNetworkLoading(new SimResults<HistogramBin>() {
						@Override
						public double getSimValue(HistogramBin histogramBin, int startTime_s, int endTime_s, SingleLinkMeasurement.TYPE type) {
							return frequencies.get(histogramBin);
						}
					});
					distances.forEachEntry((personId, v) -> {
						PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
						if (v < 260000) {
							planBuilder.addTurn(HistogramBin.values()[(int) (v / 20000.0)], 0);
						}
						double offset = calibrator.calcLinearPlanEffect(planBuilder.getResult());
						afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, cadytsWeight * offset));
						return true;
					});
					distances.clear();
				});
				if (alternative.equals("full-procedure") || alternative.equals("full-procedure-with-histogram")) {
					try {
						PrintWriter printWriter = new PrintWriter(startupEvent.getServices().getControlerIO().getOutputFilename("distance-before-after.txt"));
						printWriter.printf("before after\n");
						startupEvent.getServices().getInjector().getInstance(TrajectoryReEnricherMonitoring.class).lengthBeforeVsAfter()
								.subscribe(dp -> printWriter.printf("%f %f\n", dp.lengthBefore, dp.lengthAfter));
						startupEvent.getServices().addControlerListener((ShutdownListener) shutdownEvent -> printWriter.close());
					} catch (FileNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		CadytsAndCloneScoringFunctionFactory factory = new CadytsAndCloneScoringFunctionFactory();
		factory.setCadytsweight(cadytsWeight);
		controler.setScoringFunctionFactory(factory);
		controler.run();
	}
}
