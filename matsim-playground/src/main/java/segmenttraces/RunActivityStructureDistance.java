package segmenttraces;

import cdr.Sighting;
import cdr.Sightings;
import cdr.SightingsImpl;
import cdr.SightingsReader;
import enrichtraces.DistanceCalculator;
import fx.DistanceFromHomeChart;
import fx.TrajectorySimilarityApp;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import populationsize.IterationResource;
import populationsize.RunResource;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static segmenttraces.ActivityTimelineChart.getExperiencedPlans;

public class RunActivityStructureDistance extends Application {

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
		Random random = MatsimRandom.getRandom();
		final Sightings sightings = new SightingsImpl();
		boolean isHeadless = getParameters().getNamed().get("headless") != null;
		new SightingsReader(sightings).read(IOUtils.getInputStream(getParameters().getNamed().get("sightingsDir") + "/sightings.txt"));
		RunResource baseRun = new RunResource(getParameters().getNamed().get("baseRunDir"));
		RunResource run = new RunResource(getParameters().getNamed().get("runDir"));
		String output = getParameters().getNamed().get("output");
		IterationResource iteration = run.getIteration(0);
		Scenario baseScenario = baseRun.getConfigAndNetwork();
		TripRouter tripRouter = TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(baseScenario).get();
		Network network = baseScenario.getNetwork();
		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(baseRun.getLastIteration(), network);
		DistanceCalculator distanceCalculator = new DistanceCalculator(network);
		Parent node;
		if (isHeadless) {
			VBox vBox = new VBox();
			for (Id<Person> personId : RunActivityStructure.getAgentIds(population.keySet(), random)) {
				vBox.getChildren().add(createChart(personId, distanceCalculator, originalPopulation, population, sightings, tripRouter));
			}
			node = vBox;
		} else {
			final ListView<Id<Person>> listView = new ListView<>();
			listView.setItems(FXCollections.observableArrayList(population.keySet()));
			listView.setCellFactory(list -> new ListCell<Id<Person>>() {
				@Override
				protected void updateItem(Id<Person> personId, boolean empty) {
					super.updateItem(personId, empty);
					if (!empty) {
						setGraphic(createChart(personId, distanceCalculator, originalPopulation, population, sightings, tripRouter));
					}
				}
			});
			node = listView;
		}
		Scene scene = new Scene(node);
		primaryStage.setScene(scene);
		primaryStage.show();
		if (isHeadless) {
			ImageIO.write(SwingFXUtils.fromFXImage(node.snapshot(null, null), null), "png", new File(output));
			primaryStage.close();
		}
	}

	private DistanceFromHomeChart createChart(Id<Person> personId, DistanceCalculator distanceCalculator, Map<Id<Person>, Plan> originalPopulation, Map<Id<Person>, Plan> population, Sightings sightings, TripRouter tripRouter) {
		DistanceFromHomeChart chart = new DistanceFromHomeChart(distanceCalculator);
		chart.getStylesheets().add(TrajectorySimilarityApp.class.getResource("activity-structure-distance.css").toExternalForm());
		chart.sparse.setAll(plan2sightings(originalPopulation.get(personId), personId));
		chart.dense.setAll(plan2sightings(population.get(personId), personId));
		for (int i = 0; i < sightings.getSightingsPerPerson().get(personId).size(); i++) {
			Sighting sighting = sightings.getSightingsPerPerson().get(personId).get(i);
			DistanceFromHomeChart.ActivityData activityData = new DistanceFromHomeChart.ActivityData(sighting);
			if (i > 0) {
				Sighting previousSighting = sightings.getSightingsPerPerson().get(personId).get(i - 1);
				List<? extends PlanElement> trip = tripRouter.calcRoute("car", new SightingWrapperFacility(previousSighting), new SightingWrapperFacility(sighting),  sighting.getTime(), null);
				activityData.travelTimeFromPreviousProperty().set(TripStructureUtils.getLegs(trip).stream().mapToDouble(Leg::getTravelTime).sum());
			}
			if (i < sightings.getSightingsPerPerson().get(personId).size() - 1) {
				Sighting nextSighting = sightings.getSightingsPerPerson().get(personId).get(i + 1);
				List<? extends PlanElement> trip = tripRouter.calcRoute("car", new SightingWrapperFacility(sighting), new SightingWrapperFacility(nextSighting), sighting.getTime(), null);
				activityData.travelTimeToNextProperty().set(TripStructureUtils.getLegs(trip).stream().mapToDouble(Leg::getTravelTime).sum());
			}
			chart.addActivity(activityData);
		}
		chart.titleProperty().set(String.format("Individual %s, dist %d vs %d",
				personId.toString(),
				(int) distanceCalculator.distance(plan2sightings(originalPopulation.get(personId), personId)),
				(int) distanceCalculator.distance(plan2sightings(population.get(personId), personId))));
		((ValueAxis) chart.getXAxis()).setLowerBound(9.0);
//		chart.setPrefSize(1000, 330);
		return chart;
	}

}
