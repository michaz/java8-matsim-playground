package fx;

import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import org.matsim.analysis.LegHistogram;

import java.util.List;

public class LegHistogramPane {

	public static BorderPane createAnimatedChart(List<LegHistogram> legHistograms) {

		//defining the axes
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setAnimated(false);
		final NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("hour");
		yAxis.setAnimated(false);

		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(30);
		yAxis.setLowerBound(0);
		yAxis.setUpperBound(1000);
		//creating the chart
		final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);


		lineChart.setTitle("Leg Histogram");

		XYChart.Series<Number, Number> departures = new XYChart.Series<>();
		departures.setName("departures");

		XYChart.Series<Number, Number> arrivals = new XYChart.Series<>();
		arrivals.setName("arrivals");

		XYChart.Series<Number, Number> stuck = new XYChart.Series<>();
		stuck.setName("stuck");

		XYChart.Series<Number, Number> enRoute = new XYChart.Series<>();
		enRoute.setName("en route");

		lineChart.getData().add(departures);
		lineChart.getData().add(arrivals);
		lineChart.getData().add(stuck);
		lineChart.getData().add(enRoute);



		Slider slider = new Slider(0, (legHistograms.size() - 1) * 10, 0);
		slider.setBlockIncrement(10);
		slider.setMajorTickUnit(10);
		slider.setMinorTickCount(0);
		slider.setSnapToTicks(true);
		slider.setShowTickLabels(true);
		slider.valueProperty().addListener((observable, oldValue, newValue) -> {
			int iteration = newValue.intValue();
			LegHistogram legHistogram = legHistograms.get(iteration /10);
			fillSeries(departures, legHistogram.getDepartures());


			fillSeries(arrivals, legHistogram.getArrivals());


			fillSeries(stuck, legHistogram.getStuck());

			int[] enRouteA = new int[legHistogram.getDepartures().length];
			int enRouteM = 0;
			for (int i = 0; i < legHistogram.getDepartures().length; i++) {
				enRouteM = enRouteM + legHistogram.getDepartures()[i] - legHistogram.getArrivals()[i];
				enRouteA[i] = enRouteM;
			}


			fillSeries(enRoute, enRouteA);
		});
		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(lineChart);
		borderPane.setBottom(slider);
		borderPane.setPadding(new Insets(15,12,15,12));
		return borderPane;
	}

	private static void fillSeries(XYChart.Series<Number, Number> series, int[] data) {
		int bin = 0;
		if (series.getData().isEmpty()) {
			for (int n : data) {
				series.getData().add(new XYChart.Data<>(bin * (300.0/60.0/60.0), n));
				bin++;
			}
		} else {
			for (int n : data) {
				series.getData().get(bin).setYValue(n);
				bin++;
			}
		}


	}

}
