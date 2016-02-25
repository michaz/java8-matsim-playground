package enrichtraces;

import cdr.Sighting;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.geom.util.AffineTransformationBuilder;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.matsim.api.core.v01.Coord;

import java.util.List;
import java.util.stream.Collectors;

public class TrajectoryEnricher {
    private DistanceCalculator network;
    private List<Sighting> sparse;
    private List<Sighting> dense;

    public TrajectoryEnricher(DistanceCalculator network, List<Sighting> sparse, List<Sighting> dense) {

        this.network = network;
        this.sparse = sparse;
        this.dense = dense;
    }

    public void drehStreckAll() {
        for (int i=0; i<sparse.size(); i++) {
            drehStreck(i);
        }
    }

    public void drehStreckSome() {
        double d = Math.random();
        for (int i=0; i<sparse.size(); i++) {
            if (Math.random() < d) {
                drehStreck(i);
            }
        }
    }

    public void drehStreck(int firstIndex) {
        Sighting a = sparse.get(firstIndex);
        Sighting b = sparse.get((firstIndex + 1) % sparse.size());
        double[] times = DistanceCalculator.times(dense).toArray();
        PolynomialSplineFunction interpolationX = new LinearInterpolator().interpolate(times, network.xs(dense).toArray());
        PolynomialSplineFunction interpolationY = new LinearInterpolator().interpolate(times, network.ys(dense).toArray());
        AffineTransformation transformation = getAffineTransformation(a, b, interpolationX, interpolationY);
        List<Sighting> newSightings = dense.stream()
                .filter(s -> s.getTime() > a.getTime())
                .filter(s -> s.getTime() < b.getTime() || b.getTime() < a.getTime() /* rollover */)
                .map(s -> {
                    Coordinate coordinate = new Coordinate(network.getCoord(s).getX(), network.getCoord(s).getY());
                    coordinate = transformation.transform(coordinate, coordinate);
                    return new Sighting(a.getAgentId(), (long) s.getTime(), network.locateInCell(new Coord(coordinate.x, coordinate.y)));
                }).collect(Collectors.toList());
        sparse.addAll(firstIndex+1, newSightings);
    }

    private AffineTransformation getAffineTransformation(Sighting a, Sighting b, PolynomialSplineFunction interpolationX, PolynomialSplineFunction interpolationY) {
        Coordinate dest0 = new Coordinate(network.getCoord(a).getX(), network.getCoord(a).getY());
        Coordinate dest1 = new Coordinate(network.getCoord(b).getX(), network.getCoord(b).getY());
        Coordinate dest2 = turnedLeftAround(dest0, dest1);
        double aTime = projectIntoRange(a.getTime(), interpolationX);
        double bTime = projectIntoRange(b.getTime(), interpolationX);
        Coordinate src0 = new Coordinate(interpolationX.value(aTime), interpolationY.value(aTime));
        Coordinate src1 = new Coordinate(interpolationX.value(bTime), interpolationY.value(bTime));
        Coordinate src2 = turnedLeftAround(src0, src1);
        AffineTransformation transformation = new AffineTransformationBuilder(src0, src1, src2, dest0, dest1, dest2)
                .getTransformation();
        if (transformation == null) { // is not solvable - create a translation instead
            // TODO: Add random rotation?
            dest0 = new Coordinate(network.getCoord(a).getX(), network.getCoord(a).getY());
            dest1 = new Coordinate(dest0.x + 1000, dest0.y + 1000);
            dest2 = turnedLeftAround(dest0, dest1);
            src0 = new Coordinate(interpolationX.value(aTime), interpolationY.value(aTime));
            src1 = new Coordinate(src0.x + 1000, src0.y + 1000);
            src2 = turnedLeftAround(src0, src1);
            transformation = new AffineTransformationBuilder(src0, src1, src2, dest0, dest1, dest2)
                    .getTransformation();
        }
        return transformation;
    }

    private double projectIntoRange(double time, PolynomialSplineFunction interpolationX) {
        if (time < interpolationX.getKnots()[0]) {
            return interpolationX.getKnots()[0];
        } else if (time > interpolationX.getKnots()[interpolationX.getN()]) {
            return interpolationX.getKnots()[interpolationX.getN()];
        } else {
            return time;
        }
    }

    private Coordinate turnedLeftAround(Coordinate source1, Coordinate source2) {
        return new Coordinate(source2.x - source2.y + source1.y, source2.y + source2.x - source1.x);
    }



}
