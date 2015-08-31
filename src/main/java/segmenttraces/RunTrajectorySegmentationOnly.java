package segmenttraces;

import cadyts.CadytsModule;
import cdr.*;
import clones.ClonesConfigGroup;
import clones.ClonesModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import populationsize.CadytsAndCloneScoringFunctionFactory;
import populationsize.MultiRateRunResource;
import populationsize.RunResource;

public class RunTrajectorySegmentationOnly {

	public static void main(String[] args) {
		String baseRunDir = args[0];
		String sightingsDir = args[1];
		String output = args[2];
		int cloneFactor = 1;
		final Config config = MultiRateRunResource.phoneConfig(cloneFactor);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(output);
		RunResource baseRun = new RunResource(baseRunDir);

		Scenario baseScenario = baseRun.getConfigAndNetwork();
		final ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario.setNetwork(baseScenario.getNetwork());

		final Sightings allSightings = new SightingsImpl();
		new SightingsReader(allSightings).read(IOUtils.getInputStream(sightingsDir + "/sightings.txt"));

		final ZoneTracker.LinkToZoneResolver linkToZoneResolver = new LinkIsZone();

		PopulationFromSightings.createPopulationWithRandomRealization(scenario, allSightings, linkToZoneResolver);


		ClonesConfigGroup clonesConfig = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class);
		clonesConfig.setCloneFactor(cloneFactor);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new ClonesModule());
				install(new TrajectoryReEnricherAndSegmenterModule());
				install(new AbstractModule() {
					@Override
					public void install() {
						bind(ZoneTracker.LinkToZoneResolver.class).toInstance(linkToZoneResolver);
						bind(Sightings.class).toInstance(allSightings);
					}
				});
				addControlerListenerBinding().toInstance((IterationStartsListener) startupEvent -> {
					if (startupEvent.getIteration() == 0) {
						PlanStrategy reEnrich = controler.getInjector().getPlanStrategies().get("ReEnrichAndSegment");
						reEnrich.init(controler.getInjector().getInstance(ReplanningContext.class));
						scenario.getPopulation().getPersons().values().forEach(reEnrich::run);
						reEnrich.finish();
					}
				});
			}
		});
		controler.run();
	}

}
