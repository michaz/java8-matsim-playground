package clones;

import cdr.PersoDistHistoModule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Inject;

public class ClonesTest {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void testClonesModule() {
		Config config = matsimTestUtils.loadConfig("test/input/equil/clones-test-config.xml");
		config.global().setRandomSeed(368);
		config.controler().setOutputDirectory("output/clonesTest");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.2);
		ClonesConfigGroup clonesConfigGroup = ConfigUtils.addOrGetModule(config, ClonesConfigGroup.NAME, ClonesConfigGroup.class);
		clonesConfigGroup.setCloneFactor(3.0);
		Scenario equil100 = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(equil100);
		controler.addOverridingModule(new PersoDistHistoModule());
		controler.addOverridingModule(new ClonesModule());
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CloneService cloneService;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(cloneService.createNewScoringFunction(person));
				return sumScoringFunction;
			}
		});
		controler.run();
		System.out.println(MatsimRandom.getRandom().nextLong());
	}

}
