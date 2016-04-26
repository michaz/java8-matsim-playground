/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TrajectoryChart.java
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

import cdr.Sighting;
import enrichtraces.DistanceCalculator;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;

import java.util.Objects;

public class DistanceFromHomeChart extends LineChart<Number, Number> {

    public static class ActivityData {

        private final SimpleDoubleProperty travelTimeFromPrevious;
        private final SimpleDoubleProperty travelTimeToNext;
        private Group node;
        private final SimpleObjectProperty<Sighting> sighting;

        public ActivityData(Sighting sighting) {
            this.sighting = new SimpleObjectProperty<>(sighting);
            this.travelTimeToNext = new SimpleDoubleProperty();
            travelTimeFromPrevious = new SimpleDoubleProperty();
        }

        public void setNode(Group node) {
            this.node = node;
        }

        public Group getNode() {
            return node;
        }

        public Observable sightingProperty() {
            return sighting;
        }

        public SimpleDoubleProperty travelTimeToNextProperty() {
            return travelTimeToNext;
        }

        public SimpleDoubleProperty travelTimeFromPreviousProperty() {
            return travelTimeFromPrevious;
        }
    }

    private ObservableList<Data<Number, Number>> horizontalMarkers;
    private ObservableList<Data<Number, Number>> verticalMarkers;
    private ObservableList<ActivityData> activities;

    final DoubleProperty markerTime = new SimpleDoubleProperty();
    public final ListProperty<Sighting> sparse = new SimpleListProperty<>(FXCollections.observableArrayList());
    public final ListProperty<Sighting> dense = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final DistanceCalculator distanceCalculator;
    final ObjectProperty<ObservableList<XYChart.Data<Number, Number>>> sparseXYData;
    final ObjectProperty<ObservableList<XYChart.Data<Number, Number>>> denseXYData;
    private boolean denseVisible = true;
    private final XYChart.Series<Number, Number> denseDataSeries;

    public DistanceFromHomeChart(DistanceCalculator distanceCalculator) {
        super(new NumberAxis(0, 24, 3), new NumberAxis());

        horizontalMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.YValueProperty()});
        horizontalMarkers.addListener((InvalidationListener) observable -> layoutPlotChildren());
        verticalMarkers = FXCollections.observableArrayList(data -> new Observable[] {data.XValueProperty()});
        verticalMarkers.addListener((InvalidationListener)observable -> layoutPlotChildren());
        activities = FXCollections.observableArrayList(data -> new Observable[] {data.sightingProperty()});
        activities.addListener((InvalidationListener)observable -> layoutPlotChildren());

        this.distanceCalculator = distanceCalculator;
        final NumberAxis yAxis = (NumberAxis) getYAxis();

        getXAxis().setLabel("time [h]");
        ((NumberAxis) getXAxis()).setForceZeroInRange(false);
        getXAxis().setOnMouseMoved(mouseEvent -> markerTime.setValue(getXAxis().getValueForDisplay(mouseEvent.getX()).doubleValue() * 60.0 * 60.0));
        yAxis.setLabel("distance [m]");

        XYChart.Series<Number, Number> sparseDataSeries = new XYChart.Series<>();
        sparseXYData = sparseDataSeries.dataProperty();
        sparseXYData.bind(new MyBinding(sparse));
        denseDataSeries = new XYChart.Series<>();
        denseXYData = denseDataSeries.dataProperty();
        denseXYData.bind(new MyBinding(dense));


        getStylesheets().add(TrajectorySimilarityApp.class.getResource("trajectory-similarity.css").toExternalForm());
        setAnimated(false);
        setLegendVisible(false);
        getData().add(sparseDataSeries);
        getData().add(denseDataSeries);
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
            if (trace.get() != null) {
                for (Sighting sighting : trace.get()) {
                    double timeInHours = sighting.getTime() / (60.0 * 60.0);
                    double y = distanceCalculator.distance(trace.get().get(0), sighting);
                    result.add(new XYChart.Data<>(timeInHours, y));
                }
            }
            return result;
        }
    }

    void setDenseVisible(boolean denseVisible) {
        this.denseVisible = denseVisible;
        denseDataSeries.getNode().setVisible(this.denseVisible);
//      denseDataSeries.dataProperty().addListener(observable -> {
        denseDataSeries.getData().forEach(data -> data.getNode().setVisible(denseVisible));
//      });
    }

    public void addHorizontalValueMarker(Data<Number, Number> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (horizontalMarkers.contains(marker)) return;
        Line line = new Line();
        marker.setNode(line );
        getPlotChildren().add(line);
        horizontalMarkers.add(marker);
    }

    public void removeHorizontalValueMarker(Data<Number, Number> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        horizontalMarkers.remove(marker);
    }

    public void addVerticalValueMarker(Data<Number, Number> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (verticalMarkers.contains(marker)) return;
        Line line = new Line();
        marker.setNode(line );
        getPlotChildren().add(line);
        verticalMarkers.add(marker);
    }

    public void addActivity(ActivityData activityData) {
        Group group = new Group();
        activityData.setNode(group);
        getPlotChildren().add(group);
        activities.add(activityData);
    }


    public void removeVerticalValueMarker(Data<Number, Number> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        verticalMarkers.remove(marker);
    }


    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        for (Data<Number, Number> horizontalMarker : horizontalMarkers) {
            Line line = (Line) horizontalMarker.getNode();
            line.setStartX(0);
            line.setEndX(getBoundsInLocal().getWidth());
            line.setStartY(getYAxis().getDisplayPosition(horizontalMarker.getYValue()) + 0.5); // 0.5 for crispness
            line.setEndY(line.getStartY());
            line.toFront();
        }
        for (Data<Number, Number> verticalMarker : verticalMarkers) {
            Line line = (Line) verticalMarker.getNode();
            line.setStartX(getXAxis().getDisplayPosition(verticalMarker.getXValue()) + 0.5);  // 0.5 for crispness
            line.setEndX(line.getStartX());
            line.setStartY(0d);
            line.setEndY(getBoundsInLocal().getHeight());
            line.toFront();
        }
        for (int i = 0; i < activities.size(); i++) {
            ActivityData activity = activities.get(i);
            Group group = activity.getNode();
            group.getChildren().clear();
            Line line = new Line();
            double timeInHours = activity.sighting.get().getTime() / (60.0 * 60.0);
            line.setStartX(getXAxis().getDisplayPosition(timeInHours) + 0.5);  // 0.5 for crispness
            line.setEndX(line.getStartX());
            double distanceFromHome = distanceCalculator.distance(sparse.get().get(0), activity.sighting.get());
            line.setStartY(getYAxis().getDisplayPosition(distanceFromHome)-2.0);
            line.setEndY(getYAxis().getDisplayPosition(distanceFromHome)+2.0);
            group.getChildren().add(line);
            if (i > 0) {
                ActivityData previousActivity = activities.get(i-1);
                double previousTimeInHours = previousActivity.sighting.get().getTime() / (60.0 * 60.0);
                double previousDistanceFromHome = distanceCalculator.distance(sparse.get().get(0), previousActivity.sighting.get());
                Polyline cone = new Polyline(getXAxis().getDisplayPosition(previousTimeInHours), getYAxis().getDisplayPosition(previousDistanceFromHome),
                        getXAxis().getDisplayPosition(timeInHours - (activity.travelTimeFromPrevious.get() / (60.0 * 60.0))), getYAxis().getDisplayPosition(previousDistanceFromHome),
                        getXAxis().getDisplayPosition(timeInHours), getYAxis().getDisplayPosition(distanceFromHome));
                group.getChildren().add(cone);
            }
            if (i < activities.size()-1) {
                ActivityData nextActivity = activities.get(i+1);
                double nextTimeInHours = nextActivity.sighting.get().getTime() / (60.0 * 60.0);
                double nextDistanceFromHome = distanceCalculator.distance(sparse.get().get(0), nextActivity.sighting.get());
                Polyline cone = new Polyline(getXAxis().getDisplayPosition(timeInHours), getYAxis().getDisplayPosition(distanceFromHome),
                        getXAxis().getDisplayPosition(timeInHours + (activity.travelTimeToNext.get() / (60.0 * 60.0))), getYAxis().getDisplayPosition(nextDistanceFromHome),
                        getXAxis().getDisplayPosition(nextTimeInHours), getYAxis().getDisplayPosition(nextDistanceFromHome));
                group.getChildren().add(cone);
            }
            group.toFront();
        }
    }


}
