/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * StockLineChartApp.java
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
import javafx.application.Application;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableListValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.matsim.api.core.v01.Id;
import playground.mzilske.cdr.Sighting;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.populationsize.ExperimentResource;
import playground.mzilske.populationsize.MultiRateRunResource;
import playground.mzilske.populationsize.RegimeResource;

import java.util.*;
import java.util.stream.Collectors;


/**
 * A simulated stock line chart.
 */
public class TrajectorySimilarityApp extends Application {

    private DistanceCalculator distanceCalculator;

    final DoubleProperty markerTime = new SimpleDoubleProperty();

    public static XYChart.Series<Number, Number> createLocationMarker(final DistanceCalculator distanceCalculator, final ObservableListValue<Sighting> selectedItemProperty, final DoubleProperty markerTime1) {
        XYChart.Series<Number, Number> marker = new XYChart.Series<>();
        marker.dataProperty().bind(new ObjectBinding<ObservableList<XYChart.Data<Number, Number>>>() {
            {
                bind(selectedItemProperty, markerTime1);
            }

            @Override
            protected ObservableList<XYChart.Data<Number, Number>> computeValue() {
                ObservableList<XYChart.Data<Number, Number>> result = FXCollections.observableArrayList();
                if (selectedItemProperty.get() != null) {
                    ObservableList<Sighting> data = selectedItemProperty.get();
                    PolynomialSplineFunction interpolationX = new LinearInterpolator().interpolate(DistanceCalculator.times(data).toArray(), distanceCalculator.xs(data).toArray());
                    PolynomialSplineFunction interpolationY = new LinearInterpolator().interpolate(DistanceCalculator.times(data).toArray(), distanceCalculator.ys(data).toArray());
                    if (interpolationX.isValidPoint(markerTime1.doubleValue()) && interpolationY.isValidPoint(markerTime1.doubleValue())) {
                        XYChart.Data<Number, Number> markerDataPoint = new XYChart.Data<>(interpolationX.value(markerTime1.doubleValue()), interpolationY.value(markerTime1.doubleValue()));
                        result.add(markerDataPoint);
                    }
                }
                return result;
            }
        });
        return marker;
    }


    // 23188441,11163731

    public Parent createContent() {
        final Map<Id, List<Sighting>> dense = new HashMap<>();
        final Map<Id, List<Sighting>> sparse = new HashMap<>();
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource regime = experiment.getRegime("uncongested3");
        MultiRateRunResource multiRateRun = regime.getMultiRateRun("randomcountlocations100.0");
        Sightings sightings = multiRateRun.getSightings("5");



        for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
            if (entry.getValue().size() > 20) {
                dense.put(entry.getKey(), entry.getValue());
            } else {
                sparse.put(entry.getKey(), entry.getValue());
            }
        }

        distanceCalculator = new DistanceCalculator(multiRateRun.getBaseRun().getConfigAndNetwork().getNetwork());
        ListView<Map.Entry<Id, List<Sighting>>> sparseView = createLeftView(() -> sparse);
        ListView<Map.Entry<Id, List<Sighting>>> denseView = createRightView(() -> dense, sparseView.getSelectionModel().selectedItemProperty());

        XYChart.Series<Number, Number> sparsePath = new XYChart.Series<>();
        sparsePath.setName("Sparse Path");

        XYChart.Series<Number, Number> densePath = new XYChart.Series<>();
        densePath.setName("Dense Path");

        LineChart<Number, Number> chart = new TrajectoryChart();
        chart.getData().add(sparsePath);
        chart.getData().add(densePath);
        chart.getData().add(createLocationMarker(distanceCalculator, new MyBinding2(sparseView.getSelectionModel().selectedItemProperty()), markerTime));
        chart.getData().add(createLocationMarker(distanceCalculator, new MyBinding2(denseView.getSelectionModel().selectedItemProperty()), markerTime));
        chart.setOnMouseClicked(click -> {
            if (click.getClickCount() == 2) {
                new TrajectoryEnrichmentApp(distanceCalculator, sparseView.getSelectionModel().getSelectedItem().getValue(), denseView.getSelectionModel().getSelectedItem().getValue()).run();
            }
        });

        final SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(sparseView, denseView, chart);
        splitPane.setDividerPositions(0.3, 0.6);
        sparseView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                distanceCalculator.sortDenseByProximityToSparse(newValue.getValue(), denseView.getItems());
            }
        });
        sparsePath.dataProperty().bind(new MyBinding(sparseView.getSelectionModel().selectedItemProperty()));
        densePath.dataProperty().bind(new MyBinding(denseView.getSelectionModel().selectedItemProperty()));
        return splitPane;
    }




    private ListView<Map.Entry<Id, List<Sighting>>> createLeftView(Sightings sightings) {
        final ListView<Map.Entry<Id, List<Sighting>>> listView = new ListView<>();
        ObservableList<Map.Entry<Id, List<Sighting>>> items = FXCollections.observableArrayList(sightings.getSightingsPerPerson().entrySet());
        listView.setItems(items);
        listView.setCellFactory(new Callback<ListView<Map.Entry<Id, List<Sighting>>>, ListCell<Map.Entry<Id, List<Sighting>>>>() {
            @Override
            public ListCell<Map.Entry<Id, List<Sighting>>> call(ListView<Map.Entry<Id, List<Sighting>>> param) {
                return new ListCell<Map.Entry<Id, List<Sighting>>>() {
                    @Override
                    protected void updateItem(Map.Entry<Id, List<Sighting>> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            DistanceFromHomeChart chart = new DistanceFromHomeChart(distanceCalculator);
                            chart.sparse.setAll(item.getValue());
                            chart.chart.titleProperty().set(String.format("Individual %s, dist %d", item.getKey().toString(), (int) distanceCalculator.distance(item.getValue())));
                            chart.markerTime.addListener((observable, oldValue, newValue) -> {
                                if (newValue != null) {
                                    markerTime.set(newValue.doubleValue());
                                }
                            });
                            setGraphic(chart.chart);
                        }
                    }
                };
            }
        });
        return listView;
    }

    private ListView<Map.Entry<Id, List<Sighting>>> createRightView(Sightings sightings, ReadOnlyObjectProperty<Map.Entry<Id, List<Sighting>>> selectedSparseTrajectory) {
        final ListView<Map.Entry<Id, List<Sighting>>> listView = new ListView<>();
        ObservableList<Map.Entry<Id, List<Sighting>>> items = FXCollections.observableArrayList(sightings.getSightingsPerPerson().entrySet());
        listView.setItems(items);
        listView.setCellFactory(new Callback<ListView<Map.Entry<Id, List<Sighting>>>, ListCell<Map.Entry<Id, List<Sighting>>>>() {
            @Override
            public ListCell<Map.Entry<Id, List<Sighting>>> call(ListView<Map.Entry<Id, List<Sighting>>> param) {
                return new ListCell<Map.Entry<Id, List<Sighting>>>() {
                    @Override
                    protected void updateItem(Map.Entry<Id, List<Sighting>> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            DistanceFromHomeChart chart = new DistanceFromHomeChart(distanceCalculator);
                            chart.sparse.bind(new ObjectBinding<ObservableList<Sighting>>() {
                                {
                                    bind(selectedSparseTrajectory);
                                }

                                @Override
                                protected ObservableList<Sighting> computeValue() {
                                    if (selectedSparseTrajectory.get() != null) {
                                        return FXCollections.observableList(selectedSparseTrajectory.get().getValue());
                                    } else {
                                        return FXCollections.observableArrayList();
                                    }
                                }
                            });
                            chart.chart.titleProperty().set(String.format("Individual %s, dist %d", item.getKey().toString(), (int) distanceCalculator.distance(item.getValue())));
                            chart.dense.setAll(item.getValue());
                            chart.markerTime.addListener((observable, oldValue, newValue) -> {
                                if (newValue != null) {
                                    markerTime.set(newValue.doubleValue());
                                }
                            });
                            setGraphic(chart.chart);
                        }
                    }
                };
            }
        });
        return listView;
    }



    class MyBinding extends ObjectBinding<ObservableList<XYChart.Data<Number, Number>>> {
        private ReadOnlyObjectProperty<Map.Entry<Id, List<Sighting>>> sightings;

        private MyBinding(ReadOnlyObjectProperty<Map.Entry<Id, List<Sighting>>> sightings) {
            this.sightings = sightings;
            bind(this.sightings);
        }

        @Override
        protected ObservableList<XYChart.Data<Number, Number>> computeValue() {
            if (sightings.get() == null) {
                return FXCollections.observableArrayList();
            } else {
                List<XYChart.Data<Number, Number>> collect =
                        sightings.get().getValue().stream()
                                .map(s -> new XYChart.Data<Number, Number>(distanceCalculator.getCoord(s).getX(), distanceCalculator.getCoord(s).getY())).collect(Collectors.toList());
                return FXCollections.observableList(collect);
            }
        }
    }

    class MyBinding2 extends ListBinding<Sighting> {
        private ReadOnlyObjectProperty<Map.Entry<Id, List<Sighting>>> sightings;

        private MyBinding2(ReadOnlyObjectProperty<Map.Entry<Id, List<Sighting>>> sightings) {
            this.sightings = sightings;
            bind(this.sightings);
        }

        @Override
        protected ObservableList<Sighting> computeValue() {
            if (sightings.get() == null) {
                return null;
            } else {
                return FXCollections.observableList(sightings.get().getValue());
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }

    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}
