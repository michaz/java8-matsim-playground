package guice;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;


class MyModule extends AbstractModule {
	@Override
    public void install() {
        install(new ControlerDefaultsModule());
        install(new ControlerDefaultCoreListenersModule());
        install(new ScenarioByInstanceModule(ScenarioUtils.createScenario(getConfig())));
        bind(OutputDirectoryHierarchy.class).asEagerSingleton();
        bind(IterationStopWatch.class).asEagerSingleton();
        bind(ControlerI.class).to(Controler.class).asEagerSingleton();
    }
}
