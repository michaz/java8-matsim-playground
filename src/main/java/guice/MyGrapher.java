package guice;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.grapher.*;
import com.google.inject.grapher.graphviz.*;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.util.Types;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import javax.inject.Provider;
import java.io.PrintWriter;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MyGrapher extends AbstractInjectorGrapher {

	MyGrapher() {
		super(new GrapherParameters()
				.setAliasCreator(bindings -> {
							// Copied from ProviderAliasCreator
							List<Alias> allAliases = Lists.newArrayList();
							for (Binding<?> binding : bindings) {
								if (binding instanceof ProviderBinding) {
									allAliases.add(new Alias(NodeId.newTypeId(binding.getKey()),
											NodeId.newTypeId(((ProviderBinding<?>) binding).getProvidedKey())));
								}
							}
							allAliases.addAll(getStringMapBinderAliases(TravelTime.class, bindings));
							allAliases.addAll(getStringMapBinderAliases(TravelDisutilityFactory.class, bindings));
							allAliases.addAll(getStringMapBinderAliases(RoutingModule.class, bindings));
							allAliases.addAll(getStringMapBinderAliases(PlanStrategy.class, bindings));
							allAliases.addAll(getStringMapBinderAliases(Types.newParameterizedType(GenericPlanSelector.class, Plan.class, Person.class), bindings));
							allAliases.addAll(getMultibinderAliases(ControlerListener.class, bindings));
							allAliases.addAll(getMultibinderAliases(SnapshotWriter.class, bindings));
							allAliases.addAll(getMultibinderAliases(MobsimListener.class, bindings));
							allAliases.addAll(getMultibinderAliases(EventHandler.class, bindings));
							return allAliases;
						}
				));
	}

	static List<Alias> getMultibinderAliases(Type aClass, Iterable<Binding<?>> bindings) {
		List<Alias> aliases = Lists.newArrayList();
		NodeId toId = NodeId.newTypeId(Key.get(Types.setOf(aClass)));
		ParameterizedType comGoogleInjectProvider = Types.newParameterizedType(com.google.inject.Provider.class, aClass);
		ParameterizedType javaxInjectProvider = Types.newParameterizedType(Provider.class, aClass);
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.setOf(aClass))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.newParameterizedType(Collection.class, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, javaxInjectProvider))), toId));
		for (Binding<?> binding : bindings) {
			if (binding.getKey().getTypeLiteral().getType().equals(aClass) && binding.getKey().getAnnotationType() != null && binding.getKey().getAnnotationType().getName().equals("com.google.inject.multibindings.Element")) {
				aliases.add(new Alias(NodeId.newTypeId(binding.getKey()), toId));
			}
		}
		return aliases;
	}

	static List<Alias> getStringMapBinderAliases(Type aClass, Iterable<Binding<?>> bindings) {
		List<Alias> aliases = Lists.newArrayList();
		NodeId toId = NodeId.newTypeId(Key.get(Types.mapOf(String.class, aClass)));
		ParameterizedType comGoogleInjectProvider = Types.newParameterizedType(com.google.inject.Provider.class, aClass);
		ParameterizedType javaxInjectProvider = Types.newParameterizedType(Provider.class, aClass);
		ParameterizedType stringToComGoogleInjectProviderMapEntry = Types.newParameterizedTypeWithOwner(Map.class, Map.Entry.class, String.class, comGoogleInjectProvider);
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.setOf(stringToComGoogleInjectProviderMapEntry))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.mapOf(String.class, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.mapOf(String.class, javaxInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.mapOf(String.class, comGoogleInjectProvider))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.mapOf(String.class, aClass))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.setOf(stringToComGoogleInjectProviderMapEntry))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, Types.newParameterizedType(Provider.class, stringToComGoogleInjectProviderMapEntry)))), toId));
		aliases.add(new Alias(NodeId.newTypeId(Key.get(Types.newParameterizedType(Collection.class, Types.newParameterizedType(com.google.inject.Provider.class, stringToComGoogleInjectProviderMapEntry)))), toId));
		aliases.add(new Alias(NodeId.newInstanceId(Key.get(Types.newParameterizedType(Collection.class, Types.newParameterizedType(com.google.inject.Provider.class, stringToComGoogleInjectProviderMapEntry)))), toId));
		for (Binding<?> binding : bindings) {
			if (binding.getKey().getTypeLiteral().getType().equals(aClass) && binding.getKey().getAnnotationType() != null && binding.getKey().getAnnotationType().getName().equals("com.google.inject.multibindings.Element")) {
				aliases.add(new Alias(NodeId.newTypeId(binding.getKey()), toId));
				aliases.add(new Alias(NodeId.newInstanceId(binding.getKey()), toId));
			}
			if (binding.getKey().getTypeLiteral().getType().equals(stringToComGoogleInjectProviderMapEntry) && binding.getKey().getAnnotationType() != null && binding.getKey().getAnnotationType().getName().equals("com.google.inject.multibindings.Element")) {
				aliases.add(new Alias(NodeId.newTypeId(binding.getKey()), toId));
				aliases.add(new Alias(NodeId.newInstanceId(binding.getKey()), toId));
			}
		}
		return aliases;
	}

	private final Map<NodeId, GraphvizNode> nodes = Maps.newHashMap();
	private final List<GraphvizEdge> edges = Lists.newArrayList();
	private final NameFactory nameFactory = new ShortNameFactory();
	private final PortIdFactory portIdFactory = new PortIdFactoryImpl();

	private PrintWriter out;
	private String rankdir = "TB";

	@Override
	protected void reset() {
		nodes.clear();
		edges.clear();
	}

	public void setOut(PrintWriter out) {
		this.out = out;
	}

	public void setRankdir(String rankdir) {
		this.rankdir = rankdir;
	}

	@Override
	protected void postProcess() {
		start();

		for (GraphvizNode node : nodes.values()) {
			renderNode(node);
		}

		for (GraphvizEdge edge : edges) {
			renderEdge(edge);
		}

		finish();

		out.flush();
	}

	protected Map<String, String> getGraphAttributes() {
		Map<String, String> attrs = Maps.newHashMap();
		attrs.put("rankdir", rankdir);
		return attrs;
	}

	protected void start() {
		out.println("digraph injector {");

		Map<String, String> attrs = getGraphAttributes();
		out.println("graph " + getAttrString(attrs) + ";");
	}

	protected void finish() {
		out.println("}");
	}

	protected void renderNode(GraphvizNode node) {
		Map<String, String> attrs = getNodeAttributes(node);
		out.println(node.getIdentifier() + " " + getAttrString(attrs));
	}

	protected Map<String, String> getNodeAttributes(GraphvizNode node) {
		Map<String, String> attrs = Maps.newHashMap();

		attrs.put("label", getNodeLabel(node));
		// remove most of the margin because the table has internal padding
		attrs.put("margin", "\"0.02,0\"");
		attrs.put("shape", node.getShape().toString());
		attrs.put("style", node.getStyle().toString());

		return attrs;
	}

	/**
	 * Creates the "label" for a node. This is a string of HTML that defines a
	 * table with a heading at the top and (in the case of
	 * {@link ImplementationNode}s) rows for each of the member fields.
	 */
	protected String getNodeLabel(GraphvizNode node) {
		String cellborder = node.getStyle() == NodeStyle.INVISIBLE ? "1" : "0";

		StringBuilder html = new StringBuilder();
		html.append("<");
		html.append("<table cellspacing=\"0\" cellpadding=\"5\" cellborder=\"");
		html.append(cellborder).append("\" border=\"0\">");

		html.append("<tr>").append("<td align=\"left\" port=\"header\" ");
		html.append("bgcolor=\"" + node.getHeaderBackgroundColor() + "\">");

		String subtitle = Joiner.on("<br align=\"left\"/>").join(node.getSubtitles());
		if (subtitle.length() != 0) {
			html.append("<font color=\"").append(node.getHeaderTextColor());
			html.append("\" point-size=\"10\">");
			html.append(subtitle).append("<br align=\"left\"/>").append("</font>");
		}

		html.append("<font color=\"" + node.getHeaderTextColor() + "\">");
		html.append(htmlEscape(node.getTitle())).append("<br align=\"left\"/>");
		html.append("</font>").append("</td>").append("</tr>");

		for (Map.Entry<String, String> field : node.getFields().entrySet()) {
			html.append("<tr>");
			html.append("<td align=\"left\" port=\"").append(htmlEscape(field.getKey())).append("\">");
			html.append(htmlEscape(field.getValue()));
			html.append("</td>").append("</tr>");
		}

		html.append("</table>");
		html.append(">");
		return html.toString();
	}

	protected void renderEdge(GraphvizEdge edge) {
		Map<String, String> attrs = getEdgeAttributes(edge);

		String tailId = getEdgeEndPoint(nodes.get(edge.getTailNodeId()).getIdentifier(),
				edge.getTailPortId(), edge.getTailCompassPoint());

		String headId = getEdgeEndPoint(nodes.get(edge.getHeadNodeId()).getIdentifier(),
				edge.getHeadPortId(), edge.getHeadCompassPoint());

		out.println(tailId + " -> " + headId + " " + getAttrString(attrs));
	}

	protected Map<String, String> getEdgeAttributes(GraphvizEdge edge) {
		Map<String, String> attrs = Maps.newHashMap();

		attrs.put("arrowhead", getArrowString(edge.getArrowHead()));
		attrs.put("arrowtail", getArrowString(edge.getArrowTail()));
		attrs.put("style", edge.getStyle().toString());

		return attrs;
	}

	private String getAttrString(Map<String, String> attrs) {
		List<String> attrList = Lists.newArrayList();

		for (Map.Entry<String, String> attr : attrs.entrySet()) {
			String value = attr.getValue();

			if (value != null) {
				attrList.add(attr.getKey() + "=" + value);
			}
		}

		return "[" + Joiner.on(", ").join(attrList) + "]";
	}

	/**
	 * Turns a {@link List} of {@link ArrowType}s into a {@link String} that
	 * represents combining them. With Graphviz, that just means concatenating
	 * them.
	 */
	protected String getArrowString(List<ArrowType> arrows) {
		return Joiner.on("").join(arrows);
	}

	protected String getEdgeEndPoint(String nodeId, String portId, CompassPoint compassPoint) {
		List<String> portStrings = Lists.newArrayList(nodeId);

		if (portId != null) {
			portStrings.add(portId);
		}

		if (compassPoint != null) {
			portStrings.add(compassPoint.toString());
		}

		return Joiner.on(":").join(portStrings);
	}

	protected String htmlEscape(String str) {
		return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	protected List<String> htmlEscape(List<String> elements) {
		List<String> escaped = Lists.newArrayList();
		for (String element : elements) {
			escaped.add(htmlEscape(element));
		}
		return escaped;
	}

	@Override
	protected void newInterfaceNode(InterfaceNode node) {
		// TODO(phopkins): Show the Module on the graph, which comes from the
		// class name when source is a StackTraceElement.

		NodeId nodeId = node.getId();
		GraphvizNode gnode = new GraphvizNode(nodeId);
		gnode.setStyle(NodeStyle.DASHED);
		Key<?> key = nodeId.getKey();
		gnode.setTitle(nameFactory.getClassName(key));
		gnode.addSubtitle(0, nameFactory.getAnnotationName(key));
		addNode(gnode);
	}

	@Override
	protected void newImplementationNode(ImplementationNode node) {
		NodeId nodeId = node.getId();
		GraphvizNode gnode = new GraphvizNode(nodeId);
		gnode.setStyle(NodeStyle.SOLID);

		gnode.setHeaderBackgroundColor("#000000");
		gnode.setHeaderTextColor("#ffffff");
		gnode.setTitle(nameFactory.getClassName(nodeId.getKey()));

		for (Member member : node.getMembers()) {
			gnode.addField(portIdFactory.getPortId(member), nameFactory.getMemberName(member));
		}

		addNode(gnode);
	}

	@Override
	protected void newInstanceNode(InstanceNode node) {
		NodeId nodeId = node.getId();
		GraphvizNode gnode = new GraphvizNode(nodeId);
		gnode.setStyle(NodeStyle.SOLID);

		gnode.setHeaderBackgroundColor("#000000");
		gnode.setHeaderTextColor("#ffffff");
		gnode.setTitle(nameFactory.getClassName(nodeId.getKey()));

		gnode.addSubtitle(0, nameFactory.getSourceName(node.getSource()));

		gnode.setHeaderBackgroundColor("#aaaaaa");
		gnode.setHeaderTextColor("#ffffff");
		gnode.setTitle(nameFactory.getInstanceName(node.getInstance()));

		for (Member member : node.getMembers()) {
			gnode.addField(portIdFactory.getPortId(member), nameFactory.getMemberName(member));
		}

		addNode(gnode);
	}

	@Override
	protected void newDependencyEdge(DependencyEdge edge) {
		GraphvizEdge gedge = new GraphvizEdge(edge.getFromId(), edge.getToId());
		InjectionPoint fromPoint = edge.getInjectionPoint();
		if (fromPoint == null) {
			gedge.setTailPortId("header");
		} else {
			gedge.setTailPortId(portIdFactory.getPortId(fromPoint.getMember()));
		}
		gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL));
		gedge.setTailCompassPoint(CompassPoint.EAST);

		edges.add(gedge);
	}

	@Override
	protected void newBindingEdge(BindingEdge edge) {
		GraphvizEdge gedge = new GraphvizEdge(edge.getFromId(), edge.getToId());
		gedge.setStyle(EdgeStyle.DASHED);
		switch (edge.getType()) {
			case NORMAL:
				gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL_OPEN));
				break;

			case PROVIDER:
				gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL_OPEN, ArrowType.NORMAL_OPEN));
				break;

			case CONVERTED_CONSTANT:
				gedge.setArrowHead(ImmutableList.of(ArrowType.NORMAL_OPEN, ArrowType.DOT_OPEN));
				break;
		}

		edges.add(gedge);
	}

	private void addNode(GraphvizNode node) {
		node.setIdentifier("x" + nodes.size());
		nodes.put(node.getNodeId(), node);
	}
}
