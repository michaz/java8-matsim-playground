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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.AffineTransformationBuilder;
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
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import playground.mzilske.cdr.Sighting;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrajectoryEnrichmentApp implements Runnable {

    private final DistanceCalculator network;
    private ObservableList<Sighting> sparse;
    private ObservableList<Sighting> dense;

    public TrajectoryEnrichmentApp(DistanceCalculator network, List<Sighting> sparse, List<Sighting> dense) {
        this.network = network;
        this.sparse = FXCollections.observableArrayList(sparse);
        this.dense = FXCollections.observableArrayList(dense);
    }

    @Override
    public void run() {
        Stage stage = new Stage();
        stage.setScene(new Scene(createContent()));
        stage.show();
    }

    private Parent createContent() {
        DistanceFromHomeChart distanceFromHomeChart = new DistanceFromHomeChart(network);
        distanceFromHomeChart.sparse.set(sparse);
        distanceFromHomeChart.dense.set(dense);
        CumulativeDistanceChart cumulativeDistanceChart = new CumulativeDistanceChart(network);
        cumulativeDistanceChart.sparse.set(sparse);
        cumulativeDistanceChart.dense.set(dense);
        TrajectoryChart chart = new TrajectoryChart();
        XYChart.Series<Number, Number> sparsePath = new XYChart.Series<>();
        sparsePath.setName("Sparse Path");
        XYChart.Series<Number, Number> densePath = new XYChart.Series<>();
        densePath.setName("Dense Path");
        chart.getData().add(sparsePath);
        chart.getData().add(densePath);
        chart.getData().add(network.createLocationMarker(new SimpleListProperty<>(sparse), distanceFromHomeChart.markerTime));
        chart.getData().add(network.createLocationMarker(new SimpleListProperty<>(dense), distanceFromHomeChart.markerTime));
        sparsePath.dataProperty().bind(new MyBinding(sparse));
        densePath.dataProperty().bind(new MyBinding(dense));
        IntStream.range(0, densePath.getData().size()).forEach(i -> {
            densePath.getData().get(i).getNode().setOnMouseClicked(e -> {
                wurst(distanceFromHomeChart, chart, i);
            });
            distanceFromHomeChart.denseXYData.get().get(i).getNode().setOnMouseClicked( e -> {
                wurst(distanceFromHomeChart, chart, i);
            });
            cumulativeDistanceChart.denseXYData.get().get(i).getNode().setOnMouseClicked( e -> {
                wurst2(cumulativeDistanceChart, chart, i);
            });
        });
        ListView<Sighting> sparseSightingsListView = new ListView<>();
        sparseSightingsListView.setCellFactory(l -> new SightingsCell());
        sparseSightingsListView.itemsProperty().bind(new SimpleListProperty<>(sparse));
        sparseSightingsListView.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                int firstIndex = sparseSightingsListView.getSelectionModel().getSelectedIndex();
                if (firstIndex < sparseSightingsListView.getItems().size()-1) {
                    Sighting a = sparseSightingsListView.getItems().get(firstIndex);
                    Sighting b = sparseSightingsListView.getItems().get(firstIndex+1);
                    PolynomialSplineFunction interpolationX = new LinearInterpolator().interpolate(TrajectorySimilarityApp.times(dense).toArray(), network.xs(dense).toArray());
                    PolynomialSplineFunction interpolationY = new LinearInterpolator().interpolate(TrajectorySimilarityApp.times(dense).toArray(), network.ys(dense).toArray());
                    if (interpolationX.isValidPoint(a.getTime()) && interpolationY.isValidPoint(a.getTime())
                        && interpolationX.isValidPoint(b.getTime()) && interpolationY.isValidPoint(b.getTime())) {
                        Coordinate dest0 = new Coordinate(network.getCoord(a).getX(), network.getCoord(a).getY());
                        Coordinate dest1 = new Coordinate(network.getCoord(b).getX(), network.getCoord(b).getY());
                        Coordinate dest2 = turnedLeftAround(dest0, dest1);
                        Coordinate src0 = new Coordinate(interpolationX.value(a.getTime()), interpolationY.value(a.getTime()));
                        Coordinate src1 = new Coordinate(interpolationX.value(b.getTime()), interpolationY.value(b.getTime()));
                        Coordinate src2 = turnedLeftAround(src0, src1);
                        AffineTransformation transformation = new AffineTransformationBuilder(src0, src1, src2, dest0, dest1, dest2)
                                .getTransformation();
                        if (transformation != null) { // is solvable
                            List<Sighting> newSightings = dense.stream()
                                    .filter(s -> s.getTime() > a.getTime() && s.getTime() < b.getTime())
                                    .map(s -> {
                                        Coordinate coordinate = new Coordinate(network.getCoord(s).getX(), network.getCoord(s).getY());
                                        coordinate = transformation.transform(coordinate, coordinate);
                                        return new Sighting(a.getAgentId(), (long) s.getTime(), network.locateInCell(new CoordImpl(coordinate.x, coordinate.y)));
                                    }).collect(Collectors.toList());
                            sparse.addAll(firstIndex+1, newSightings);
                        }
                    }
                }
            }
        });
        ListView<Sighting> denseSightingsListView = new ListView<>();
        denseSightingsListView.itemsProperty().bind(new SimpleListProperty<>(dense));
        denseSightingsListView.setCellFactory(l -> new SightingsCell());
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(chart);
        borderPane.setTop(cumulativeDistanceChart.chart);
        borderPane.setBottom(distanceFromHomeChart.chart);
        borderPane.setRight(sparseSightingsListView);
        borderPane.setLeft(denseSightingsListView);
        return borderPane;
    }

    private Coordinate turnedLeftAround(Coordinate source1, Coordinate source2) {
        return new Coordinate(source2.x - source2.y + source1.y, source2.y + source2.x - source1.x);
    }

    private void wurst(DistanceFromHomeChart distanceFromHomeChart, TrajectoryChart chart, int i) {
        double distanceFromHome = distanceFromHomeChart.denseXYData.get().get(i).YValueProperty().get().doubleValue();
        chart.circle(network.getCoord(sparse.get(0)).getX(), network.getCoord(sparse.get(0)).getY(), distanceFromHome);
        Coord homeCoord = network.getCoord(sparse.get(0));
        double arc = Math.random() * 2 * Math.PI;
        double x = Math.cos(arc);
        double y = - Math.sin(arc);
        Sighting sightingToInsertFrom = dense.get(i);
        Coord newCoord = new CoordImpl(x * distanceFromHome + homeCoord.getX(), y * distanceFromHome + homeCoord.getY());
        String newCellTowerId = network.locateInCell(newCoord);
        IntStream.range(0, sparse.size())
                .filter(j -> sparse.get(j).getTime() >= sightingToInsertFrom.getTime())
                .findFirst()
                .ifPresent(j -> {
                    if (sparse.get(j).getTime() != sightingToInsertFrom.getTime()) {
                        Sighting sightingToInsert = new Sighting(sparse.stream().findAny().get().getAgentId(), (int) sightingToInsertFrom.getTime(), newCellTowerId);
                        sparse.add(j, sightingToInsert);
                    }
                });
    }

    private void wurst2(CumulativeDistanceChart cumulativeDistanceChart, TrajectoryChart chart, int i) {
        Sighting sightingToInsertFrom = dense.get(i);
        double cumulativeDistance = cumulativeDistanceChart.denseXYData.get().get(i).YValueProperty().get().doubleValue();
        IntStream.range(1, sparse.size())
                .filter(j -> sparse.get(j).getTime() >= sightingToInsertFrom.getTime())
                .findFirst()
                .ifPresent(j -> {
                    if (sparse.get(j).getTime() != sightingToInsertFrom.getTime()) {
                        double cumulativeDistanceUpToThen = cumulativeDistanceChart.sparseXYData.get().get(j-1).YValueProperty().get().doubleValue();
                        double r = Math.max(0.0, cumulativeDistance - cumulativeDistanceUpToThen);
                        double arc = Math.random() * 2 * Math.PI;
                        double x = Math.cos(arc);
                        double y = - Math.sin(arc);
                        Coord coordBefore = network.getCoord(sparse.get(j-1));
                        Coord newCoord = new CoordImpl(x * r + coordBefore.getX(), y * r + coordBefore.getY());
                        String newCellTowerId = network.locateInCell(newCoord);
                        Sighting sightingToInsert = new Sighting(sparse.stream().findAny().get().getAgentId(), (int) sightingToInsertFrom.getTime(), newCellTowerId);
                        sparse.add(j, sightingToInsert);
                        chart.circle(network.getCoord(sparse.get(j)).getX(), network.getCoord(sparse.get(j)).getY(), r);
                    }
                });
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
