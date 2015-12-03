package populationsize;

import cadyts.CadytsModule;
import cdr.*;
import clones.ClonesConfigGroup;
import clones.ClonesModule;
import enrichtraces.TrajectoryReEnricherModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

public class RunSimulation {

	public static void main(String[] args) {
		String alternative = args[0];
		String baseRunDir = args[1];
		String sightingsDir = args[2];
		String output = args[3];

		RunResource baseRun = new RunResource(baseRunDir);

		final double cadytsWeight = 100.0;
		int lastIteration = 100;
		double cloneFactor;
		if (alternative.equals("full-procedure")) {
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
				if (alternative.equals("full-procedure")) {
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
						PlanStrategy reEnrich = controler.getInjector().getPlanStrategies().get("ReRealize");
						reEnrich.init(controler.getInjector().getInstance(ReplanningContext.class));
						scenario.getPopulation().getPersons().values().forEach(reEnrich::run);
						reEnrich.finish();
					}
				});
			}
		});
		CadytsAndCloneScoringFunctionFactory factory = new CadytsAndCloneScoringFunctionFactory();
		factory.setCadytsweight(cadytsWeight);
		controler.setScoringFunctionFactory(factory);
		controler.run();
	}

}
