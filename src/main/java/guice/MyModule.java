package guice;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.counts.Counts;


class MyModule extends AbstractModule {
	@Override
    public void install() {
        install(new ControlerDefaultsModule());
        install(new ControlerDefaultCoreListenersModule());
        Scenario scenario = ScenarioUtils.createScenario(getConfig());
        scenario.addScenarioElement(Counts.ELEMENT_NAME, new Counts<Link>());
        install(new ScenarioByInstanceModule(scenario));
        bind(OutputDirectoryHierarchy.class).asEagerSingleton();
        bind(IterationStopWatch.class).asEagerSingleton();
        bind(ControlerI.class).to(Controler.class).asEagerSingleton();
    }
}
