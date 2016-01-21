package guice;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.grapher.*;
import com.google.inject.grapher.graphviz.ArrowType;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.*;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.MaskFunctor;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.facilities.ActivityFacilities;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JGraphTGrapher extends AbstractInjectorGrapher {

	private ListenableDirectedGraph<Node, Edge> g;
	private final Map<NodeId, Node> nodes = new HashMap<>();
	private Writer writer;
	private final NameFactory nameFactory = new ShortNameFactory();

	public JGraphTGrapher(GrapherParameters options, Writer writer) {
		super(options);
		this.writer = writer;
		g = new ListenableDirectedGraph<>(Edge.class);
	}

	@Override
	protected void reset() throws IOException {
		g = new ListenableDirectedGraph<>(Edge.class);
		nodes.clear();
	}

	@Override
	protected void newInterfaceNode(InterfaceNode node) throws IOException {
		g.addVertex(node);
		nodes.put(node.getId(), node);
	}

	@Override
	protected void newImplementationNode(ImplementationNode node) throws IOException {
		g.addVertex(node);
		nodes.put(node.getId(), node);
	}

	@Override
	protected void newInstanceNode(InstanceNode node) throws IOException {
		g.addVertex(node);
		nodes.put(node.getId(), node);
	}

	@Override
	protected void newDependencyEdge(DependencyEdge edge) throws IOException {
		g.addEdge(nodes.get(edge.getFromId()), nodes.get(edge.getToId()), edge);
	}

	@Override
	protected void newBindingEdge(BindingEdge edge) throws IOException {
		g.addEdge(nodes.get(edge.getFromId()), nodes.get(edge.getToId()), edge);
	}

	@Override
	protected void postProcess() throws IOException {
		DirectedMaskSubgraph<Node, Edge> filtered = new DirectedMaskSubgraph<>(g, new MaskFunctor<Node, Edge>() {
			@Override
			public boolean isEdgeMasked(Edge edge) {
				return false;
			}
			@Override
			public boolean isVertexMasked(Node node) {
				if (ConfigGroup.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (Network.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (Population.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (DumpDataAtEnd.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (OutputDirectoryHierarchy.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (MatsimServices.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (Injector.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (PopulationFactory.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (Scenario.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (Config.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (IterationStopWatch.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (EventsManager.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (ReplanningContext.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (PlansDumping.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (ActivityFacilities.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (EventsHandling.class.isAssignableFrom(node.getId().getKey().getTypeLiteral().getRawType())) {
					return true;
				}
				if (node.getId().getKey().getTypeLiteral().equals(new TypeLiteral<Set<MobsimListener>>(){})) {
					return true;
				}
				return false;
			}
		});
		ConnectivityInspector<Node, Edge> ci = new ConnectivityInspector<>(filtered);
		filtered = new DirectedMaskSubgraph<>(g, new MaskFunctor<Node, Edge>() {
			@Override
			public boolean isEdgeMasked(Edge edge) {
				return false;
			}
			@Override
			public boolean isVertexMasked(Node node) {
				Node vertex = nodes.get(NodeId.newTypeId(Key.get(ControlerI.class)));
				return !ci.connectedSetOf(vertex).contains(node);
			}
		});

		VertexNameProvider<Node> vertexIDProvider = new IntegerNameProvider<>();
		VertexNameProvider<Node> vertexLabelProvider = node -> {
			if (node instanceof InstanceNode) {
				return nameFactory.getInstanceName(((InstanceNode) node).getInstance());
			} else {
				return nameFactory.getClassName(node.getId().getKey());
			}
		};
		EdgeNameProvider<Edge> edgeLabelProvider = null;
		ComponentAttributeProvider<Node> vertexAttributeProvider = node -> {
			HashMap<String, String> atts = new HashMap<>();
			atts.put("shape", "box");
			if (node instanceof InterfaceNode) {
				atts.put("style", "dashed");
			}
			return atts;
		};
		ComponentAttributeProvider<Edge> edgeAttributeProvider = edge -> {
			HashMap<String, String> atts = new HashMap<>();
			if (edge instanceof BindingEdge) {
				atts.put("style", "dashed");
				switch (((BindingEdge) edge).getType()) {
					case NORMAL:
						atts.put("arrowhead", ArrowType.NORMAL_OPEN.toString());
						break;
					case PROVIDER:
						atts.put("arrowhead", ArrowType.NORMAL_OPEN.toString() + ArrowType.NORMAL_OPEN.toString());
						break;
					case CONVERTED_CONSTANT:
						atts.put("arrowhead", ArrowType.NORMAL_OPEN.toString() + ArrowType.DOT_OPEN.toString());
						break;
				}
			}
			return atts;
		};
		DOTExporter<Node, Edge> dotExporter = new DOTExporter<>(vertexIDProvider, vertexLabelProvider, edgeLabelProvider, vertexAttributeProvider, edgeAttributeProvider);
		dotExporter.export(writer, filtered);
	}
}
