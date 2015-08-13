package segmenttraces;

import cdr.Sighting;
import cdr.ZoneTracker;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.UndirectedSubgraph;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class HelloLatitude {

	private static final String COORDINATE_SYSTEM = TransformationFactory.DHDN_GK4;
	private static CoordinateTransformation t = TransformationFactory.getCoordinateTransformation("WGS84", COORDINATE_SYSTEM);

	private Network network;
	private ZoneTracker.LinkToZoneResolver zones;

	public HelloLatitude(Network network) {
		this.network = network;
		this.zones = zones;
	}

	public Plan getLatitude(List<Sighting> inLocations) {
		List<Sighting> locations = new ArrayList<>(inLocations);
		sortLocations(locations);
		filterLocations(locations);
		System.out.println("Before segmentation: " + locations.size());
		List<List<Sighting>> segmentation = segmentLocations(locations);
		System.out.println("After segmentation: " + segmentation.size());

		List<Segment> segments = classifySegments(segmentation);
		List<SignificantLocation> significantLocations = findSignificantLocations(segments);

//		createLinks(scenario, segmentation);
		return createActivities(segments, significantLocations);
	}

	List<SignificantLocation> findSignificantLocations(List<Segment> segments) {
		List<SignificantLocation> result = new ArrayList<SignificantLocation>();
		
		
		List<Segment> significantActivities = new ArrayList<Segment>();
		for (Segment segment : segments) {
			if (segment.isSignificant) {
				significantActivities.add(segment);
			}
		}
		
		
		// Idiotische Art, in quadratischer Zeit den euklidischen MST zu berechnen.
		// Das natürlich beizeiten durch O(nlogn)-Algorithmus ersetzen.
		UndirectedGraph<Segment, DefaultWeightedEdge> g;
		g = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		for (Segment segment : significantActivities) {
			g.addVertex(segment);
		}
		for (int i=0; i < significantActivities.size(); ++i) {
			for (int j=i+1; j< significantActivities.size(); ++j) {
				Segment v1 = significantActivities.get(i);
				Segment v2 = significantActivities.get(j);
				Graphs.addEdge(g, v1, v2, CoordUtils.calcDistance(centroid(v1.locations), centroid(v2.locations)));
			}
		}
		KruskalMinimumSpanningTree<Segment, DefaultWeightedEdge> mst = new KruskalMinimumSpanningTree<>(g);

		Set<DefaultWeightedEdge> edges = mst.getEdgeSet();
		System.out.println("Weights:");
		for (DefaultWeightedEdge edge : edges) {
			System.out.println(g.getEdgeWeight(edge));
		}

		Set<DefaultWeightedEdge> shortEdges = new HashSet<>();
		for (DefaultWeightedEdge edge : edges) {
			if (g.getEdgeWeight(edge) < 50) {
				shortEdges.add(edge);
			}
		}

		UndirectedSubgraph<Segment, DefaultWeightedEdge> subgraph = new UndirectedSubgraph<>(g, g.vertexSet(), shortEdges);
		ConnectivityInspector<Segment, DefaultWeightedEdge> connectivityInspector = new ConnectivityInspector<>(subgraph);
		List<Set<Segment>> clusters = connectivityInspector.connectedSets();
		int locationId = 0;
		for (Set<Segment> cluster : clusters) {
			System.out.println(cluster);
			SignificantLocation significantLocation = new SignificantLocation();
			significantLocation.id = locationId++;
			result.add(significantLocation);
			for (Segment activity : cluster) {
				activity.atLocation = significantLocation;
			}
			
		}
		
		return result;
	}

	private static List<Segment> classifySegments(List<List<Sighting>> segmentation) {
		List<Segment> result = new ArrayList<Segment>();
		for (List<Sighting> locations : segmentation) {
			Segment segment = new Segment();
			segment.locations = locations;
			double start = locations.get(0).getTime();
			double end = locations.get(locations.size() - 1).getTime();
			double durationInMinutes = (end - start) / 60.0;
			segment.isSignificant = durationInMinutes > 1;
			result.add(segment);
		}
		return result;
	}

	List<List<Sighting>> segmentLocations(List<Sighting> locations) {
		List<List<Sighting>> result = new ArrayList<>();
		List<Sighting> segment = new ArrayList<>();
		result.add(segment);
		for (Sighting location : locations) {
			if (!clusterCriterion(segment, location)) {
				segment = new ArrayList<>();
				result.add(segment);
			}
			Iterator<Sighting> locationsInSegment = segment.iterator();
			while (locationsInSegment.hasNext()) {
				Sighting locationInSegment = locationsInSegment.next();
				if (liesCompletelyIn(location, locationInSegment)) {
					locationsInSegment.remove();
				}
			}
			segment.add(location);
		}
		return result;
	}

	boolean liesCompletelyIn(Sighting location, Sighting locationInSegment) {
		if (CoordUtils.calcDistance(getCoord(location), getCoord(locationInSegment)) < (locationInSegment.getAccuracy() - location.getAccuracy())) {
			return true;
		} else {
			return false;
		}
	}

	boolean clusterCriterion(List<Sighting> segment, Sighting location) {
		if (segment.isEmpty()) return true;
		return CoordUtils.calcDistance(getCoord(segment.get(0)), getCoord(location)) - segment.get(0).getAccuracy() - location.getAccuracy() <= 70.0;
	}

	private static void sortLocations(List<Sighting> locations) {
		Collections.sort(locations, (arg0, arg1) -> Double.compare(arg0.getTime(), arg1.getTime()));
	}

	private static void filterLocations(List<Sighting> locations) {
	}

	Plan createActivities(List<Segment> segments, List<SignificantLocation> significantLocations) {
		Plan pl = new PlanImpl();
		int nAct = 0;
		Activity prev = null;
		List<Segment> planElement = new ArrayList<Segment>();
		for (Segment segment : segments) {
			List<Sighting> locations = segment.locations;
			double startTime = locations.get(0).getTime();
			double endTime = locations.get(locations.size() - 1).getTime();
			if(segment.isSignificant) {
				Coord coord = centroid(segment.locations);
				if (prev != null) {
					Leg leg = new LegImpl("unknown");
					leg.setTravelTime(startTime - prev.getEndTime());
					pl.addLeg(leg);
				}
				planElement.clear();
				
				Activity activity = new ActivityImpl("unknown", coord);
				activity.setStartTime(startTime);
				activity.setEndTime(endTime);
				pl.addActivity(activity);
				System.out.println(activity);
				prev = activity;
			} else {
				planElement.add(segment);
			}
		}
		return pl;
	}

	private static int instantToMatsim(Instant start) {
		return LocalTime.from(start.atZone(ZoneId.of("UTC"))).toSecondOfDay();
	}

	double measure(List<Segment> planElement) {
		double result = 0.0;
		Coord prev = null;
		for (Segment segment : planElement) {
			Coord sis = centroid(segment.locations);
			if (prev != null) {
				result += CoordUtils.calcDistance(prev, sis);
			}
			prev = sis;
		}
		return result;
	}

	Coord getCoord(Sighting location) {
		return network.getLinks().get(Id.createLinkId(location.getCellTowerId())).getCoord();
	}



	private static double accuracy(List<Location> segment) {
		double result = 0;
		for (Location location : segment) {
			result += location.getAccuracy();
		}
		return result / segment.size();
	}

	Coord centroid(List<Sighting> segment) {
		CoordImpl result = new CoordImpl(0,0);
		for (Sighting location : segment) {
			Coord coord = getCoord(location);
			result.setXY(result.getX() + coord.getX(), result.getY() + coord.getY());
		}
		result.setXY(result.getX() / segment.size(), result.getY() / segment.size());
		return result;
	}

}
