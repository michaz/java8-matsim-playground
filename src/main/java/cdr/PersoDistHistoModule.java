package cdr;


import com.google.common.eventbus.Subscribe;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ExperiencedPlanElementsModule;
import org.matsim.core.scoring.ExperiencedPlanElementsService;
import org.matsim.core.scoring.PersonExperiencedLeg;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import util.FileIO;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class PersoDistHistoModule extends AbstractModule {

	public static void main(String[] args) {
		String runDir = args[0];
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(runDir);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		Scenario scenario = ScenarioUtils.createScenario(config);
		int iterationNumber = Integer.parseInt(args[1]);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(runDir + "/output_network.xml.gz");
		new MatsimPopulationReader(scenario).readFile(runDir + "/output_plans.xml.gz");
		com.google.inject.Injector injector = Injector.createInjector(config,
				new ReplayEvents.Module(),
				new PersoDistHistoModule(),
				new ExperiencedPlanElementsModule(),
				new ScenarioByInstanceModule(scenario),
				new EventsManagerModule(),
				new AbstractModule() {
					@Override
					public void install() {
						bind(OutputDirectoryHierarchy.class).asEagerSingleton();
					}
				});
				ReplayEvents instance = injector.getInstance(ReplayEvents.class);
		instance.playEventsFile(runDir + "/ITERS/it."+iterationNumber+"/"+iterationNumber+".events.xml.gz", iterationNumber);
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(PersoDistHistoControlerListener.class).asEagerSingleton();
	}

	private static class PersoDistHistoControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener {

		@Inject Population population;
		@Inject ExperiencedPlanElementsService experiencedPlanElementsService;
		@Inject OutputDirectoryHierarchy controlerIO;

		private HashMap<Id<Person>, Double> distances;

		@Override
		public void notifyStartup(StartupEvent startupEvent) {
			experiencedPlanElementsService.register(this);
		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
			distances = new HashMap<>();
			for (Id<Person> personId : population.getPersons().keySet()) {
				distances.put(personId, 0.0);
			}
		}

		@Subscribe
		public void addLeg(PersonExperiencedLeg leg) {
			distances.put(leg.getAgentId(), distances.get(leg.getAgentId()) + leg.getLeg().getRoute().getDistance());
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			FileIO.writeToFile(controlerIO.getIterationPath(iterationEndsEvent.getIteration())+"/perso-dist-histo.txt", pw -> {
				pw.printf("person\tdistance\n");
				for (Map.Entry<Id<Person>, Double> entry : distances.entrySet()) {
					String personId = entry.getKey().toString();
					Double distance = entry.getValue();
					pw.printf("%s\t%.2f\n", personId, distance);
				}
			});
		}
	}

}
