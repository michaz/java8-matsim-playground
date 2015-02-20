/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CumulativeDistanceChart.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package fx;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import playground.mzilske.cdr.Sighting;

public class CumulativeDistanceChart {

    final DoubleProperty markerTime = new SimpleDoubleProperty();
    final ListProperty<Sighting> sparse = new SimpleListProperty<>(FXCollections.observableArrayList());
    final ListProperty<Sighting> dense = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final DistanceCalculator distanceCalculator;
    final LineChart<Number, Number> chart;
    final ObjectProperty<ObservableList<XYChart.Data<Number, Number>>> sparseXYData;
    final ObjectProperty<ObservableList<XYChart.Data<Number, Number>>> denseXYData;

    public CumulativeDistanceChart(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
        NumberAxis xAxis;
        xAxis = new NumberAxis(0, 24, 3);
        final NumberAxis yAxis = new NumberAxis();

        xAxis.setLabel("time [h]");
        xAxis.setForceZeroInRange(false);
        xAxis.setOnMouseMoved(mouseEvent -> markerTime.setValue(xAxis.getValueForDisplay(mouseEvent.getX()).doubleValue() * 60.0 * 60.0));
        yAxis.setLabel("distance [m]");
        chart = new LineChart<>(xAxis, yAxis);

        XYChart.Series<Number, Number> sparseDataSeries = new XYChart.Series<>();
        sparseXYData = sparseDataSeries.dataProperty();
        sparseXYData.bind(new MyBinding(sparse));
        XYChart.Series<Number, Number> denseDataSeries = new XYChart.Series<>();
        denseXYData = denseDataSeries.dataProperty();
        denseXYData.bind(new MyBinding(dense));


        chart.getStylesheets().add(TrajectorySimilarityApp.class.getResource("StockLineChart.css").toExternalForm());
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.getData().add(sparseDataSeries);
        chart.getData().add(denseDataSeries);
    }

    private class MyBinding extends ObjectBinding<ObservableList<XYChart.Data<Number, Number>>> {
        private ListProperty<Sighting> trace;

        public MyBinding(ListProperty<Sighting> trace) {
            this.trace = trace;
            bind(trace);
        }

        @Override
        protected ObservableList<XYChart.Data<Number, Number>> computeValue() {
            ObservableList<XYChart.Data<Number, Number>> result = FXCollections.observableArrayList();
            double dist = 0.0;
            if (trace.get() != null) {
                for (int i=0; i<trace.get().size(); ++i) {
                    Sighting sighting = trace.get().get(i);
                    double timeInHours = sighting.getTime() / (60.0 * 60.0);
                    if (i>0) {
                        dist += distanceCalculator.distance(trace.get().get(i-1), sighting);
                    }
                    result.add(new XYChart.Data<>(timeInHours, dist));
                }
            }
            return result;
        }
    }


}
