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

import cdr.Sighting;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

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

    PolynomialSplineFunction getInterpolation(List<Sighting> o2) {
        if (o2.size() > 1) {
            return new LinearInterpolator().interpolate(times(o2).toArray(), dists(o2).toArray());
        } else {
            return new PolynomialSplineFunction(
                    new double[]{0.0, 24.0},
                    new PolynomialFunction[]{new PolynomialFunction(new double[]{0})});
        }
    }

    public void sortDenseByProximityToSparse(final List<Sighting> sparseTrace, List<Map.Entry<Id, List<Sighting>>> denseTraces) {
        Collections.sort(denseTraces, new Comparator<Map.Entry<Id, List<Sighting>>>() {
            ConcurrentHashMap<Map.Entry<Id, List<Sighting>>, Double> cache = new ConcurrentHashMap<>();

            @Override
            public int compare(Map.Entry<Id, List<Sighting>> o1, Map.Entry<Id, List<Sighting>> o2) {
                return Double.compare(cache.computeIfAbsent(o1, this::euclideanDistance), cache.computeIfAbsent(o2, this::euclideanDistance));
            }

            private double euclideanDistance(Map.Entry<Id, List<Sighting>> denseTrace) {
                PolynomialSplineFunction distanceFromHomeInterpolationSparse = getInterpolation(sparseTrace);
                PolynomialSplineFunction distanceFromHomeInterpolationDense = getInterpolation(denseTrace.getValue());
                double[] times = sparseTrace.stream().mapToDouble(Event::getTime).sorted().toArray();
                double[] ysSparse = new double[times.length];
                double[] ysDense = new double[times.length];
                for (int i=0;i<times.length; ++i) {
                    double x = times[i];
                    ysSparse[i] = evaluateContinuingLeftAndRight(distanceFromHomeInterpolationSparse, x);
                    ysDense[i] = evaluateContinuingLeftAndRight(distanceFromHomeInterpolationDense, x);
                }
                double result = 0.0;
                for (int i=0; i<times.length-1; ++i) {
                    double deltaT = times[i+1] - times[i];
                    double a, b, aPlus, bPlus;
                    if (ysSparse[i] > ysDense[i]) {
                        b = ysSparse[i]; a = ysDense[i];
                        bPlus = ysSparse[i+1]; aPlus = ysDense[i+1];
                    } else {
                        b = ysDense[i]; a = ysSparse[i];
                        bPlus = ysDense[i+1]; aPlus = ysSparse[i+1];
                    }
                    if (aPlus > bPlus) {
                        result += deltaT * (((b-a)*(b-a) + (aPlus-bPlus)*(aPlus-bPlus)) / 2*(aPlus-a-bPlus+b));
                    } else {
                        result += deltaT * (((b-a)+(bPlus-aPlus)) / 2);
                    }
                }
                return result;
            }
        });
    }

    private double evaluateContinuingLeftAndRight(PolynomialSplineFunction function, double x) {
        if (x < function.getKnots()[0]) {
            return function.value(function.getKnots()[0]);
        } else if (x > function.getKnots()[function.getN()]) {
            return function.value(function.getKnots()[function.getN()]);
        }
        return function.value(x);
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
