package fx;/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LineChartSample.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.matsim.analysis.LegHistogram;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LegHistogramApp extends Application {

    @Override
    public void start(Stage stage) {


        stage.setTitle("Leg Histogram");


        Scene scene = new Scene(createAnimatedChart(), 800, 600);
        scene.getStylesheets().add("fx/leghistogram.css");

        stage.setScene(scene);
        stage.show();
    }

    private BorderPane createAnimatedChart() {
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


        List<LegHistogram> legHistograms = new ArrayList<>();
        for (int iter : Arrays.asList(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)) {
            LegHistogram legHistogram = readLegHistogram(iter);
            legHistograms.add(legHistogram);

        }

        Slider slider = new Slider(0, 100, 0);
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

    private LegHistogram readLegHistogram(int iter) {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        LegHistogram legHistogram = new LegHistogram(300);
        eventsManager.addHandler(legHistogram);
        new MatsimEventsReader(eventsManager).readFile("/Users/michaelzilske/IDEAcheckout/playgrounds/mzilske/test/output/playground/mzilske/cdr/CDREquilTest/testOneWorkplaceAnytimeWithReplanning/output2/ITERS/it." + iter + "/" + iter + ".events.xml.gz");
        return legHistogram;
    }

    private void fillSeries(XYChart.Series<Number, Number> series, int[] data) {
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

    public static void main(String[] args) {
        Application.launch(args);
    }

}
