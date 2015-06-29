package fx;

import cdr.Sighting;
import cdr.Sightings;
import enrichtraces.DistanceCalculator;
import javafx.application.Application;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import populationsize.ExperimentResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunTrajectorySimilarityFigures extends Application {

	public static final Id<Person> SPARSE_ID = Id.createPersonId("23138184");
	public static final Id<Person> DENSE_ID = Id.createPersonId("16155871");

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		final Map<Id, List<Sighting>> dense = new HashMap<>();
		final Map<Id, List<Sighting>> sparse = new HashMap<>();
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource regime = experiment.getRegime("uncongested3");
		MultiRateRunResource multiRateRun = regime.getMultiRateRun("realcountlocations100.0");
		Sightings sightings = multiRateRun.getSightings("5");



		for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
			if (entry.getValue().size() > 20) {
				dense.put(entry.getKey(), entry.getValue());
			} else {
				sparse.put(entry.getKey(), entry.getValue());
			}
		}

		DistanceCalculator distanceCalculator = new DistanceCalculator(multiRateRun.getBaseRun().getConfigAndNetwork().getNetwork());
		DistanceFromHomeChart chart = new DistanceFromHomeChart(distanceCalculator);
		chart.sparse.setAll(sparse.get(SPARSE_ID));
		chart.chart.titleProperty().set(String.format("Individual %s, dist %d", SPARSE_ID.toString(), (int) distanceCalculator.distance(sparse.get(SPARSE_ID))));
		Scene scene = new Scene(chart.chart);

		primaryStage.setScene(scene);
		primaryStage.show();


		ImageIO.write(SwingFXUtils.fromFXImage(chart.chart.snapshot(null, null), null), "png", new File("output/sparse-trace.png"));

		chart.chart.titleProperty().set(String.format("Individual %s, dist %d", DENSE_ID.toString(), (int) distanceCalculator.distance(dense.get(DENSE_ID))));
		chart.dense.setAll(dense.get(DENSE_ID));
		ImageIO.write(SwingFXUtils.fromFXImage(chart.chart.snapshot(null, null), null), "png", new File("output/sparse-and-dense-trace.png"));

		TrajectoryEnrichmentApp trajectoryEnrichmentApp = new TrajectoryEnrichmentApp(distanceCalculator, sparse.get(SPARSE_ID), dense.get(DENSE_ID));
		trajectoryEnrichmentApp.enrich();
		TrajectoryChart beforeChart = trajectoryEnrichmentApp.createChart(true);
		TrajectoryChart afterChart = trajectoryEnrichmentApp.createChart(false);
		primaryStage.setScene(new Scene(beforeChart));
		ImageIO.write(SwingFXUtils.fromFXImage(beforeChart.snapshot(null, null), null), "png", new File("output/sparse-trace-xy.png"));
		primaryStage.setScene(new Scene(afterChart));
		ImageIO.write(SwingFXUtils.fromFXImage(afterChart.snapshot(null, null), null), "png", new File("output/enriched-trace-xy.png"));



		primaryStage.close();
	}

}
