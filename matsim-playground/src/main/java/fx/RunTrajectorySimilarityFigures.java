package fx;

import cdr.Sighting;
import cdr.Sightings;
import cdr.SightingsImpl;
import cdr.SightingsReader;
import enrichtraces.DistanceCalculator;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import populationsize.RunResource;

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
		final Id<Person> SPARSE_ID = Id.createPersonId(getParameters().getNamed().get("sparse"));
		final Id<Person> DENSE_ID = Id.createPersonId(getParameters().getNamed().get("dense"));

		final String outputDir = getParameters().getNamed().get("output");
		new File(outputDir).mkdirs();

		final Sightings sightings = new SightingsImpl();
		new SightingsReader(sightings).read(IOUtils.getInputStream(getParameters().getNamed().get("sightingsDir") + "/sightings.txt"));

		RunResource baseRun = new RunResource(getParameters().getNamed().get("baseRunDir"));

		List<Sighting> sparseTrace = sightings.getSightingsPerPerson().get(SPARSE_ID);
		List<Sighting> denseTrace = sightings.getSightingsPerPerson().get(DENSE_ID);

		DistanceCalculator distanceCalculator = new DistanceCalculator(baseRun.getConfigAndNetwork().getNetwork());
		DistanceFromHomeChart distanceFromHomeChart = new DistanceFromHomeChart(distanceCalculator);
		distanceFromHomeChart.sparse.setAll(sparseTrace);
		distanceFromHomeChart.dense.setAll(denseTrace);
		distanceFromHomeChart.chart.titleProperty().set(String.format("Individual %s, dist %d", SPARSE_ID.toString(), (int) distanceCalculator.distance(sparseTrace)));
		distanceFromHomeChart.setDenseVisible(false);

		Scene scene = new Scene(distanceFromHomeChart.chart);

		primaryStage.setScene(scene);
		primaryStage.show();

		ImageIO.write(SwingFXUtils.fromFXImage(distanceFromHomeChart.chart.snapshot(null, null), null), "png", new File(outputDir+"/sparse-trace.png"));

		distanceFromHomeChart.chart.titleProperty().set(String.format("Individual %s, dist %d", DENSE_ID.toString(), (int) distanceCalculator.distance(denseTrace)));
		distanceFromHomeChart.setDenseVisible(true);
		ImageIO.write(SwingFXUtils.fromFXImage(distanceFromHomeChart.chart.snapshot(null, null), null), "png", new File(outputDir+"/sparse-and-dense-trace.png"));

		TrajectoryEnrichmentApp trajectoryEnrichmentApp = new TrajectoryEnrichmentApp(distanceCalculator, sparseTrace, denseTrace);
		trajectoryEnrichmentApp.enrich();

		distanceFromHomeChart.sparse.setAll(trajectoryEnrichmentApp.enriched);
		ImageIO.write(SwingFXUtils.fromFXImage(distanceFromHomeChart.chart.snapshot(null, null), null), "png", new File(outputDir+"/enriched-and-dense-trace.png"));

		CumulativeDistanceChart cumulativeDistanceChart = new CumulativeDistanceChart(distanceCalculator);
		cumulativeDistanceChart.sparse.setAll(trajectoryEnrichmentApp.enriched);
		cumulativeDistanceChart.dense.setAll(denseTrace);
		primaryStage.setScene(new Scene(cumulativeDistanceChart.chart));
		ImageIO.write(SwingFXUtils.fromFXImage(cumulativeDistanceChart.chart.snapshot(null, null), null), "png", new File(outputDir+"/enriched-and-dense-trace-cumulative.png"));

		TrajectoryChart beforeChart = trajectoryEnrichmentApp.createChart(true);
		TrajectoryChart afterChart = trajectoryEnrichmentApp.createChart(false);
		primaryStage.setScene(new Scene(beforeChart));
		ImageIO.write(SwingFXUtils.fromFXImage(beforeChart.snapshot(null, null), null), "png", new File(outputDir+"/sparse-trace-xy.png"));
		primaryStage.setScene(new Scene(afterChart));
		ImageIO.write(SwingFXUtils.fromFXImage(afterChart.snapshot(null, null), null), "png", new File(outputDir+"/enriched-trace-xy.png"));

		primaryStage.close();
	}

}
