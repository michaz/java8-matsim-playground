package fx;

import cdr.Sighting;
import cdr.Sightings;
import enrichtraces.DistanceCalculator;
import javafx.application.Application;
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
		Id<Person> personId = Id.createPersonId("23138184");
		chart.sparse.setAll(sparse.get(personId));
		chart.chart.titleProperty().set(String.format("Individual %s, dist %d", personId.toString(), (int) distanceCalculator.distance(sparse.get(personId))));
		Scene scene = new Scene(chart.chart);

		primaryStage.setScene(scene);
		primaryStage.show();


		WritableImage snapShot = chart.chart.snapshot(null, null);
		ImageIO.write(SwingFXUtils.fromFXImage(snapShot, null), "png", new File("output/test.png"));

	}

}
