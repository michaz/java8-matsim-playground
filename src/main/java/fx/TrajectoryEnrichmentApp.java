/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TrajectoryEnrichmentApp.java
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

import enrichtraces.DistanceCalculator;
import enrichtraces.TrajectoryEnricher;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import playground.mzilske.cdr.Sighting;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrajectoryEnrichmentApp implements Runnable {

    private final DistanceCalculator network;
    ObservableList<Sighting> sparse;
    private ObservableList<Sighting> dense;
    private final TrajectoryEnricher trajectoryEnricher;

    public TrajectoryEnrichmentApp(DistanceCalculator network, List<Sighting> sparse, List<Sighting> dense) {
        this.network = network;
        this.sparse = FXCollections.observableArrayList(sparse);
        this.dense = FXCollections.observableArrayList(dense);
        trajectoryEnricher = new TrajectoryEnricher(network, this.sparse, this.dense);
    }

    @Override
    public void run() {
        Stage stage1 = new Stage();
        stage1.setScene(new Scene(createContent()));
        stage1.show();
        Stage stage2 = new Stage();
        stage2.setScene(new Scene(createContent2()));
        stage2.show();
    }

    private Parent createContent() {
        TrajectoryChart chart = new TrajectoryChart();
        XYChart.Series<Number, Number> sparsePath = new XYChart.Series<>();
        sparsePath.setName("Sparse Path");
        XYChart.Series<Number, Number> densePath = new XYChart.Series<>();
        densePath.setName("Dense Path");
        chart.getData().add(sparsePath);
        chart.getData().add(densePath);
        chart.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                trajectoryEnricher.drehStreckAll();
            }
        });
//        chart.getData().add(TrajectorySimilarityApp.createLocationMarker(network, new SimpleListProperty<>(sparse), distanceFromHomeChart.markerTime));
//        chart.getData().add(TrajectorySimilarityApp.createLocationMarker(network, new SimpleListProperty<>(dense), distanceFromHomeChart.markerTime));
        sparsePath.dataProperty().bind(new MyBinding(sparse));
        densePath.dataProperty().bind(new MyBinding(dense));
        ListView<Sighting> sparseSightingsListView = new ListView<>();
        sparseSightingsListView.setCellFactory(l -> new SightingsCell());
        sparseSightingsListView.itemsProperty().bind(new SimpleListProperty<>(sparse));
        sparseSightingsListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                int firstIndex = sparseSightingsListView.getSelectionModel().getSelectedIndex();
                trajectoryEnricher.drehStreck(firstIndex);
            }
        });
        ListView<Sighting> denseSightingsListView = new ListView<>();
        denseSightingsListView.itemsProperty().bind(new SimpleListProperty<>(dense));
        denseSightingsListView.setCellFactory(l -> new SightingsCell());
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(chart);
//        borderPane.setTop(cumulativeDistanceChart.chart);
//        borderPane.setBottom(distanceFromHomeChart.chart);
//        borderPane.setRight(sparseSightingsListView);
//        borderPane.setLeft(denseSightingsListView);
        return borderPane;
    }

    private Parent createContent2() {
        DistanceFromHomeChart distanceFromHomeChart = new DistanceFromHomeChart(network);
        distanceFromHomeChart.sparse.set(sparse);
        distanceFromHomeChart.dense.set(dense);
        CumulativeDistanceChart cumulativeDistanceChart = new CumulativeDistanceChart(network);
        cumulativeDistanceChart.sparse.set(sparse);
        cumulativeDistanceChart.dense.set(dense);
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(cumulativeDistanceChart.chart);
        borderPane.setBottom(distanceFromHomeChart.chart);
        return borderPane;
    }


    class MyBinding extends ObjectBinding<ObservableList<XYChart.Data<Number, Number>>> {
        private ObservableList<Sighting> sightings;

        private MyBinding(ObservableList<Sighting> sightings) {
            this.sightings = sightings;
            bind(this.sightings);
        }

        @Override
        protected ObservableList<XYChart.Data<Number, Number>> computeValue() {
            List<XYChart.Data<Number, Number>> collect =
                    sightings.stream()
                    .map(s -> new XYChart.Data<Number, Number>(network.getCoord(s).getX(), network.getCoord(s).getY())).collect(Collectors.toList());
            return FXCollections.observableList(collect);
        }
    }

    private class SightingsCell extends javafx.scene.control.ListCell<Sighting> {
        @Override
        protected void updateItem(Sighting item, boolean empty) {
            super.updateItem(item, empty);
            if (!empty) {
                setText(Time.writeTime(item.getTime()));
            }
        }
    }
}
