package mapmatching;

import de.bmw.hmm.MostLikelySequence;
import de.bmw.hmm.TimeStep;
import de.bmw.offline_map_matching.map_matcher.OfflineMapMatcher;
import de.bmw.offline_map_matching.map_matcher.SpatialMetrics;
import de.bmw.offline_map_matching.map_matcher.TemporalMetrics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MapMatching {

	static class MyTransitRouteStop {
		TransitRouteStop stop;
		int index;
	}

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("output/wurst/network.xml");
		new NetworkCleaner().run(scenario.getNetwork());
		new TransitScheduleReader(scenario).readFile("output/wurst/transit-schedule.xml");
		FreespeedTravelTimeAndDisutility travelCosts = new FreespeedTravelTimeAndDisutility(0.0, 0.0, -1.0);
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(scenario.getNetwork(), travelCosts, travelCosts);
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				System.out.println(route.getId());
				List<TimeStep<Link, MyTransitRouteStop>> timeSteps = new ArrayList<>();
				int i=0;
				for (TransitRouteStop transitRouteStop : route.getStops()) {
					MyTransitRouteStop state = new MyTransitRouteStop();
					state.stop = transitRouteStop;
					state.index = i++;
					Collection<Node> nearestNodes = ((NetworkImpl) scenario.getNetwork()).getNearestNodes(transitRouteStop.getStopFacility().getCoord(), 200.0);
					Collection<Link> nearestLinks = nearestNodes.stream().flatMap(node -> node.getInLinks().values().stream()).collect(Collectors.toList());
					timeSteps.add(new TimeStep<Link, MyTransitRouteStop>(state, nearestLinks));
				}
				TemporalMetrics<MyTransitRouteStop> temporalMetrics = new TemporalMetrics<MyTransitRouteStop>() {
					@Override
					public double timeDifference(MyTransitRouteStop o1, MyTransitRouteStop o2) {
						double v = (o2.index - o1.index) * 120.0;
						return v;
					}
				};
				SpatialMetrics<Link, MyTransitRouteStop> spatialMetrics = new SpatialMetrics<Link, MyTransitRouteStop>() {
					@Override
					public double measurementDistance(Link node, MyTransitRouteStop o) {
						return CoordUtils.calcEuclideanDistance(node.getCoord(), o.stop.getStopFacility().getCoord());
					}

					@Override
					public double linearDistance(MyTransitRouteStop o, MyTransitRouteStop o1) {
						return CoordUtils.calcEuclideanDistance(o.stop.getStopFacility().getCoord(), o1.stop.getStopFacility().getCoord());
					}

					@Override
					public Double routeLength(Link node1, Link node2) {
						LeastCostPathCalculator.Path path = router.calcLeastCostPath(node1.getToNode(), node2.getToNode(), 0.0, null, null);
						double dist = 0.0;
						for (Link link : path.links) {
							dist += link.getLength();
						}
						return dist;
					}
				};
				MostLikelySequence<Link, MyTransitRouteStop> seq = OfflineMapMatcher.computeMostLikelySequence(timeSteps, temporalMetrics, spatialMetrics);
				if (!seq.isBroken) {
					if (!seq.sequence.isEmpty()) {
						List<Id<Link>> linkIds = new ArrayList<>();
						Link link = seq.sequence.get(0);
						for (int j=1; j<seq.sequence.size(); j++) {
							linkIds.add(link.getId());
							Link nextLink = seq.sequence.get(j);
							LeastCostPathCalculator.Path path = router.calcLeastCostPath(link.getToNode(), nextLink.getFromNode(), 0.0, null, null);
							linkIds.addAll(path.links.stream().map(Link::getId).collect(Collectors.toList()));
							link = nextLink;
						}
						linkIds.add(link.getId());
						route.setRoute(RouteUtils.createNetworkRoute(linkIds, scenario.getNetwork()));
					}
				}
				System.out.println(seq.isBroken);
				System.out.println(seq.sequence);
				System.out.printf("%d -> %d\n", timeSteps.size(), seq.sequence.size());
			}
		}
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("output/wurst/matched-transit-schedule.xml");
	}

}
