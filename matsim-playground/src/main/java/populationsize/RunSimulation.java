package populationsize;

import cadyts.CadytsModule;
import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.PlanBuilder;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.supply.SimResults;
import cdr.*;
import clones.CloneService;
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
import java.util.HashMap;

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

		final double cadytsWeight = 0.0;
		int lastIteration = 100;
		double cloneFactor;
		if (alternative.equals("full-procedure") || alternative.equals("full-procedure-with-histogram")) {
			cloneFactor = 1.0;
		} else if (alternative.equals("clone") || alternative.equals("clone-with-histogram")) {
			cloneFactor = 3.0;
		} else {
			throw new RuntimeException(alternative);
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

//		PlanStrategy reEnrich = controler.getInjector().getInstance(Key.get(PlanStrategy.class, Names.named("ReRealize")));
//		reEnrich.init(controler.getInjector().getInstance(ReplanningContext.class));
//		scenario.getPopulation().getPersons().values().forEach(reEnrich::run);
//		reEnrich.finish();

		// TODO: replace with full procedure (like in commented out code above)
		// TODO: Otherwise, initial travel length distribution is biased because we start with non-enriched traces.
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
				install(new PersoDistHistoModule());
				if (alternative.equals("full-procedure") || alternative.equals("full-procedure-with-histogram")) {
					install(new TrajectoryReEnricherModule());
				} else if (alternative.equals("clone") || alternative.equals("clone-with-histogram")) {
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
			}
		});
		if (alternative.equals("full-procedure-with-histogram") || alternative.equals("clone-with-histogram")) {
			controler.addControlerListener((StartupListener) startupEvent -> {
				AnalyticalCalibrator<HistogramBin> calibrator = new AnalyticalCalibrator<>(startupEvent.getServices().getConfig().controler().getOutputDirectory() + "/cadyts-histogram.txt", MatsimRandom.getRandom().nextLong(), 24*60*60);
				calibrator.setStatisticsFile(startupEvent.getServices().getControlerIO().getOutputFilename("histogram-calibration-stats.txt"));
				calibrator.addMeasurement(HistogramBin.B0, 0, 24*60*60, 494, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B20000, 0, 24*60*60, 541, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B40000, 0, 24*60*60, 331, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B60000, 0, 24*60*60, 176, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B80000, 0, 24*60*60, 83, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B100000, 0, 24*60*60, 53, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B120000, 0, 24*60*60, 25, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B140000, 0, 24*60*60, 18, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B160000, 0, 24*60*60, 17, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B180000, 0, 24*60*60, 6, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B200000, 0, 24*60*60, 4, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B220000, 0, 24*60*60, 10, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B240000, 0, 24*60*60, 6, SingleLinkMeasurement.TYPE.COUNT_VEH);
				calibrator.addMeasurement(HistogramBin.B260000, 0, 24*60*60, 12, SingleLinkMeasurement.TYPE.COUNT_VEH);
				startupEvent.getServices().addControlerListener((BeforeMobsimListener) beforeMobsimEvent -> {
					CloneService cloneService = beforeMobsimEvent.getServices().getInjector().getInstance(CloneService.class);
					for (Person person : beforeMobsimEvent.getServices().getScenario().getPopulation().getPersons().values()) {
						PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
						if (cloneService.isActive(person.getId())) { // Inactive clones are not in a histogram bin
							double totalPlannedDistance = 0.0;
							for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
								if (planElement instanceof Leg) {
									totalPlannedDistance += ((Leg) planElement).getRoute().getDistance();
								}
							}
							planBuilder.addTurn(HistogramBin.values()[(int) (Math.min(totalPlannedDistance, 260000) / 20000.0)], 0);
						}
						calibrator.addToDemand(planBuilder.getResult());
					}
				});
				startupEvent.getServices().addControlerListener((AfterMobsimListener) afterMobsimEvent -> {
					PersoDistHistogram distService = afterMobsimEvent.getServices().getInjector().getInstance(PersoDistHistogram.class);
					EnumMap<HistogramBin, Integer> frequencies = new EnumMap<>(HistogramBin.class);
					for (HistogramBin bin : HistogramBin.values()) {
						frequencies.put(bin, 0);
					}
					HashMap<Id<Person>, Double> distances = distService.getDistances();
					distances.values().forEach(v -> {
						HistogramBin bin = HistogramBin.values()[(int) (Math.min(v, 260000) / 20000.0)];
						frequencies.put(bin, frequencies.get(bin) + 1);
					});
					calibrator.afterNetworkLoading(new SimResults<HistogramBin>() {
						@Override
						public double getSimValue(HistogramBin histogramBin, int startTime_s, int endTime_s, SingleLinkMeasurement.TYPE type) {
							return frequencies.get(histogramBin);
						}
					});
					distances.forEach((personId, v) -> {
						PlanBuilder<HistogramBin> planBuilder = new PlanBuilder<>();
						planBuilder.addTurn(HistogramBin.values()[(int) (Math.min(v, 260000) / 20000.0)], 0);
						double offset = calibrator.calcLinearPlanEffect(planBuilder.getResult());
						afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.UNDEFINED_TIME, personId, 100.0 * offset));
					});
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
