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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.chart.NumberAxis;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;

import java.util.function.Predicate;

public class TrajectoryChart extends NonReorderingLineChart<Number, Number> {

    private Series<Number, Number> extraSeries = new Series<>();
    private Series<Number, Number> paddingPoints = new Series<>();

    DoubleProperty minX = new SimpleDoubleProperty();
    DoubleProperty maxX = new SimpleDoubleProperty();
    DoubleProperty minY = new SimpleDoubleProperty();
    DoubleProperty maxY = new SimpleDoubleProperty();


    public TrajectoryChart() {
        super(new NumberAxis(), new NumberAxis());
        getStylesheets().add(TrajectorySimilarityApp.class.getResource("trajectory-xy.css").toExternalForm());
        setAnimated(false);
        setLegendVisible(false);
        NumberAxis xAxis = (NumberAxis) getXAxis();
        NumberAxis yAxis = (NumberAxis) getYAxis();
        xAxis.setLabel("x [m]");
        xAxis.setForceZeroInRange(false);
        yAxis.setLabel("y [m]");
        yAxis.setForceZeroInRange(false);
        getData().add(extraSeries);
        getData().add(paddingPoints);
        Data<Number, Number> xPaddingPoint = new Data<>();
        DoubleBinding xExtension = maxX.subtract(minX);
        DoubleBinding yExtension = maxY.subtract(minY);
        NumberBinding maxExtension = Bindings.max(xExtension, yExtension);
        xPaddingPoint.XValueProperty().bind(minX.add(maxExtension));
        xPaddingPoint.YValueProperty().bind(minY.add(maxExtension));
        paddingPoints.getData().add(xPaddingPoint);
    }

    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        for (Data item : extraSeries.getData()) {
            double radius = (Double) item.getExtraValue();
            Ellipse ellipse = (Ellipse) item.getNode();
            ellipse.setRadiusX(Math.abs(((NumberAxis) getXAxis()).getScale()) * radius);
            ellipse.setRadiusY(Math.abs(((NumberAxis) getYAxis()).getScale() * radius));
        }
    }

    @Override
    protected void dataItemAdded(Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
        if (series == extraSeries) {
            Ellipse node = new Ellipse();
            node.setStroke(Color.AQUAMARINE);
            node.setFill(Color.TRANSPARENT);
            item.setNode(node);
            getPlotChildren().add(node);
        } else if (series == paddingPoints) {
               // Nothing
        } else {
            super.dataItemAdded(series, itemIndex, item);
            minX.set(getData().stream().filter(Predicate.isEqual(paddingPoints).negate()).flatMap(s -> s.getData().stream()).mapToDouble(d -> d.getXValue().doubleValue()).min().getAsDouble());
            maxX.set(getData().stream().filter(Predicate.isEqual(paddingPoints).negate()).flatMap(s -> s.getData().stream()).mapToDouble(d -> d.getXValue().doubleValue()).max().getAsDouble());
            minY.set(getData().stream().filter(Predicate.isEqual(paddingPoints).negate()).flatMap(s -> s.getData().stream()).mapToDouble(d -> d.getYValue().doubleValue()).min().getAsDouble());
            maxY.set(getData().stream().filter(Predicate.isEqual(paddingPoints).negate()).flatMap(s -> s.getData().stream()).mapToDouble(d -> d.getYValue().doubleValue()).max().getAsDouble());
        }
    }

    public void circle(double x, double y, double distanceFromHome) {
        extraSeries.getData().add(new Data<>(x, y, distanceFromHome));
    }
}
