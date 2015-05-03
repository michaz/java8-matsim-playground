/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DistanceCalculator.java
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

package enrichtraces;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableListValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.mzilske.cdr.Sighting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.DoubleStream;

public class DistanceCalculator {

    private Network network;

    public DistanceCalculator(Network network) {
        this.network = network;
    }


    public Coord getCoord(Sighting a) {
        return network.getLinks().get(Id.createLinkId(a.getCellTowerId())).getCoord();
    }

    public double distance(Sighting a, Sighting b) {
        return CoordUtils.calcDistance(
                getCoord(a),
                getCoord(b));
    }

    public double distance(List<Sighting> sightings) {
        return sightings.stream()
                .map(sighting -> new BetweenSightings(sighting, sighting, 0.0))
                .reduce((a, b) -> new BetweenSightings(a.a, b.b, a.dist + b.dist + distance(a.b, b.a)))
                .get().dist;
    }

    public String locateInCell(Coord newCoord) {
        return ((NetworkImpl) network).getNearestLinkExactly(newCoord).getId().toString();
    }

    public java.util.stream.DoubleStream xs(List<Sighting> value) {
        return value.stream().mapToDouble(sighting -> getCoord(sighting).getX());
    }

    public java.util.stream.DoubleStream ys(List<Sighting> value) {
        return value.stream().mapToDouble(sighting -> getCoord(sighting).getY());
    }

    java.util.stream.DoubleStream dists(List<Sighting> value) {
        Sighting home = value.get(0);
        return value.stream().mapToDouble(sighting -> distance(home, sighting));
    }

    PolynomialSplineFunction getInterpolation(Map.Entry<Id, List<Sighting>> o2) {
        return new LinearInterpolator().interpolate(times(o2.getValue()).toArray(), dists(o2.getValue()).toArray());
    }

    public XYChart.Series<Number, Number> createLocationMarker(final ObservableListValue<Sighting> selectedItemProperty, final DoubleProperty markerTime1) {
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
                    PolynomialSplineFunction interpolationX = new LinearInterpolator().interpolate(DistanceCalculator.times(data).toArray(), xs(data).toArray());
                    PolynomialSplineFunction interpolationY = new LinearInterpolator().interpolate(DistanceCalculator.times(data).toArray(), ys(data).toArray());
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

    public void sortDenseByProximityToSparse(final List<Sighting> sparseTrace, List<Map.Entry<Id, List<Sighting>>> denseTraces) {
        Collections.sort(denseTraces, new Comparator<Map.Entry<Id, List<Sighting>>>() {
            ConcurrentHashMap<Map.Entry<Id, List<Sighting>>, Double> cache = new ConcurrentHashMap<>();

            @Override
            public int compare(Map.Entry<Id, List<Sighting>> o1, Map.Entry<Id, List<Sighting>> o2) {
                return Double.compare(cache.computeIfAbsent(o1, this::euclideanDistance), cache.computeIfAbsent(o2, this::euclideanDistance));
            }

            private double euclideanDistance(Map.Entry<Id, List<Sighting>> denseTrace) {
                PolynomialSplineFunction distanceFromHomeInterpolation = getInterpolation(denseTrace);
                Sighting home = sparseTrace.get(0);
                DoubleStream ysSparse = sparseTrace.stream().filter(sighting -> distanceFromHomeInterpolation.isValidPoint(sighting.getTime())).mapToDouble(sighting -> distance(home, sighting));
                DoubleStream ysDense = sparseTrace.stream().filter(sighting -> distanceFromHomeInterpolation.isValidPoint(sighting.getTime())).mapToDouble(sighting -> distanceFromHomeInterpolation.value(sighting.getTime()));
                return new EuclideanDistance().compute(ysSparse.toArray(), ysDense.toArray());
            }
        });
    }

    private static class BetweenSightings {
        private BetweenSightings(Sighting a, Sighting b, double dist) {
            this.a = a;
            this.b = b;
            this.dist = dist;
        }
        Sighting a;
        Sighting b;
        double dist;
    }

    public static java.util.stream.DoubleStream times(List<Sighting> value) {
        return value.stream().mapToDouble(Event::getTime);
    }
}
