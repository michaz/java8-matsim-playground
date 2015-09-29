package segmenttraces;

import cdr.Sightings;
import cdr.SightingsImpl;
import cdr.SightingsReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.SlidingGanttCategoryDataset;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import populationsize.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static segmenttraces.ActivityTimelineChart.*;

public class RunActivityStructure {

	public static void main(String[] args) {
		String baseRunDir = args[0];
		String sightingsDir = args[1];
		String runDir = args[2];
		String output = args[3];
		Set<Id<Person>> agents = getAgentIds();
		RunResource run = new RunResource(runDir);
		RunResource baseRun = new RunResource(baseRunDir);

		IterationResource iteration = run.getIteration(0);
		Network network = baseRun.getConfigAndNetwork().getNetwork();
		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
		population.keySet().retainAll(agents);
		population = new TreeMap<>(population);

		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(baseRun.getLastIteration(), network);
		originalPopulation.keySet().retainAll(agents);
		originalPopulation = new TreeMap<>(originalPopulation);
		final Sightings sightings = new SightingsImpl();
		new SightingsReader(sightings).read(IOUtils.getInputStream(sightingsDir + "/sightings.txt"));
		sightings.getSightingsPerPerson().keySet().retainAll(agents);

		TaskSeriesCollection taskSeriesCollection = new TaskSeriesCollection();

		taskSeriesCollection.add(getSightings(new TreeMap<>(sightings.getSightingsPerPerson())));
		taskSeriesCollection.add(getTaskSeries("Original", originalPopulation));
		taskSeriesCollection.add(getTaskSeries("Reconstructed", population));

		SlidingGanttCategoryDataset dataset = new SlidingGanttCategoryDataset(taskSeriesCollection, 0, 5);
		JFreeChart ganttChart = ChartFactory.createGanttChart("Activities", "Agent", "Time", dataset);
		((DateAxis) ganttChart.getCategoryPlot().getRangeAxis()).setTimeZone(TimeZone.getTimeZone("UTC"));
		ganttChart.getCategoryPlot().getRangeAxis().setLowerBound(9.0 * 60 * 60 * 1000);
		ganttChart.getCategoryPlot().getRangeAxis().setUpperBound(24.0 * 60 * 60 * 1000);
		GanttRenderer renderer = (GanttRenderer) ((CategoryPlot) ganttChart.getPlot()).getRenderer();
		renderer.setDrawBarOutline(true);
		renderer.setSeriesPaint(0, Color.BLACK);
		renderer.setSeriesPaint(1, Color.BLUE);
		renderer.setSeriesPaint(2, Color.RED);
		renderer.setSeriesOutlinePaint(0, Color.BLACK);
		renderer.setSeriesOutlinePaint(1, Color.BLUE);
		renderer.setSeriesOutlinePaint(2, Color.RED);
		renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0F));

		try {
			ChartUtilities.saveChartAsPNG(new File(output), ganttChart, 1024, 768);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	static Set<Id<Person>> getAgentIds() {
		Set<Id<Person>> agents = new TreeSet<>();
		agents.add(Id.createPersonId("24122484"));
		agents.add(Id.createPersonId("14104774"));
		agents.add(Id.createPersonId("23135904"));
		return agents;
	}

}
