package segmenttraces;

import cdr.Sighting;
import enrichtraces.DistanceCalculator;
import fx.DistanceFromHomeChart;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.chart.ValueAxis;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.Time;
import populationsize.ExperimentResource;
import populationsize.IterationResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static segmenttraces.ActivityTimelineChart.getExperiencedPlans;

public class RunActivityTimeline extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	private static List<Sighting> plan2sightings(Plan plan, Id<Person> agentId) {
		List<Sighting> result = new ArrayList<>();
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (Time.UNDEFINED_TIME != act.getStartTime()) {
					Sighting s = new Sighting(agentId, (long) act.getStartTime(), act.getLinkId().toString());
					result.add(s);
				}
				if (Time.UNDEFINED_TIME != act.getEndTime()) {
					Sighting s = new Sighting(agentId, (long) act.getEndTime(), act.getLinkId().toString());
					result.add(s);
				}
			}
		}
		for (Sighting sighting : result) {
			System.out.format("%s %s\n", Time.writeTime(sighting.getTime(), ':'), sighting.getCellTowerId());
		}
		return result;
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource uncongested = experiment.getRegime("uncongested3");
		MultiRateRunResource multiRateRun = uncongested.getMultiRateRun("onlyheavyusers-noenrichment-segmentation10minutes");
		IterationResource iteration = multiRateRun.getRateRun("50.0", "1").getIteration(0);
		Network network = uncongested.getBaseRun().getConfigAndNetwork().getNetwork();
		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(uncongested.getBaseRun().getLastIteration(), network);
		DistanceCalculator distanceCalculator = new DistanceCalculator(multiRateRun.getBaseRun().getConfigAndNetwork().getNetwork());
		final VBox vBox = new VBox();
		for (Id personId : RunActivityTimelineFigures.getAgentIds()) {
			DistanceFromHomeChart chart = new DistanceFromHomeChart(distanceCalculator);
			chart.sparse.setAll(plan2sightings(originalPopulation.get(personId), personId));
			chart.dense.setAll(plan2sightings(population.get(personId), personId));
			chart.chart.titleProperty().set(String.format("Individual %s, dist %d vs %d",
					personId.toString(),
					(int) distanceCalculator.distance(plan2sightings(originalPopulation.get(personId), personId)),
					(int) distanceCalculator.distance(plan2sightings(population.get(personId), personId))));
			((ValueAxis) chart.chart.getXAxis()).setLowerBound(9.0);
			chart.chart.setPrefSize(1000, 330);
			vBox.getChildren().add(chart.chart);
		}
		Scene scene = new Scene(vBox);
		primaryStage.setScene(scene);
		primaryStage.show();
		ImageIO.write(SwingFXUtils.fromFXImage(vBox.snapshot(null, null), null), "png", new File("output/activity-structure-distance.png"));
	}

}
