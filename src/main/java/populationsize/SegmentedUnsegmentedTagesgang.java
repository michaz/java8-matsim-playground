package populationsize;

import fx.LegHistogramPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.matsim.analysis.LegHistogram;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class SegmentedUnsegmentedTagesgang extends Application {

	public static void main(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource uncongested = experiment.getRegime("uncongested3");
		stage.setTitle("Leg Histogram");
		List<LegHistogram> legHistograms = new ArrayList<>();
		legHistograms.add(getLegHistogram(uncongested.getBaseRun().getLastIteration()));
		legHistograms.add(getLegHistogram(uncongested.getMultiRateRun("trajectoryenrichment100.0random").getRateRun("5.0", "1").getIteration(0)));
		legHistograms.add(getLegHistogram(uncongested.getMultiRateRun("trajectoryenrichment100.0randomlatitude").getRateRun("5.0", "1").getIteration(0)));
		legHistograms.add(getLegHistogram(uncongested.getMultiRateRun("trajectoryenrichment100.0randomlatitude2").getRateRun("5.0", "1").getIteration(0)));
		Scene scene = new Scene(LegHistogramPane.createAnimatedChart(legHistograms), 800, 600);
		scene.getStylesheets().add("fx/leghistogram.css");
		stage.setScene(scene);
		stage.show();
	}

	private LegHistogram getLegHistogram(IterationResource iteration) {
		LegHistogram legHistogram = new LegHistogram(300);
		ReplayEvents.run(ScenarioUtils.createScenario(ConfigUtils.createConfig()), iteration.getEventsFileName(), new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(legHistogram);
			}
		});
		return legHistogram;
	}

}
