package segmenttraces;

import cdr.Sighting;
import cdr.Sightings;
import cdr.SightingsImpl;
import cdr.SightingsReader;
import org.jfree.chart.ChartFactory;
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
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import populationsize.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static segmenttraces.ActivityTimelineChart.*;

public class RunActivityStructure {

	public static void main(String[] args) {
		Random random = MatsimRandom.getRandom();
		String baseRunDir = args[0];
		String sightingsDir = args[1];
		String runDir = args[2];
		String output = args[3];
		RunResource run = new RunResource(runDir);
		RunResource baseRun = new RunResource(baseRunDir);

		IterationResource iteration = run.getIteration(0);
		Network network = baseRun.getConfigAndNetwork().getNetwork();

		final Sightings sightings = new SightingsImpl();
		new SightingsReader(sightings).read(IOUtils.getInputStream(sightingsDir + "/sightings.txt"));

		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(baseRun.getLastIteration(), network);
		LinkedHashMap<Id, List<Sighting>> sortedSightings = new LinkedHashMap<>();
		LinkedHashMap<Id<Person>, Plan> sortedPopulation = new LinkedHashMap<>();
		LinkedHashMap<Id<Person>, Plan> sortedOriginalPopulation = new LinkedHashMap<>();
		Collection<Id<Person>> agents = getAgentIds((Set<Id<Person>>) (Set) population.keySet(), random);
		sightings.getSightingsPerPerson().keySet().retainAll(agents);

		for (Id<Person> personId : agents) {
			sortedSightings.put(personId, sightings.getSightingsPerPerson().get(personId));
			sortedPopulation.put(personId, population.get(personId));
			sortedOriginalPopulation.put(personId, originalPopulation.get(personId));
		}

		TaskSeriesCollection taskSeriesCollection = new TaskSeriesCollection();

		taskSeriesCollection.add(getSightings(sortedSightings));
		taskSeriesCollection.add(getTaskSeries("Original", sortedOriginalPopulation));
		taskSeriesCollection.add(getTaskSeries("Reconstructed", sortedPopulation));

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

	static Collection<Id<Person>> getAgentIds(Set<Id<Person>> ids, Random random) {
		List<Id<Person>> idList = new ArrayList<>(ids);
		Collections.shuffle(idList, random);
		return idList.stream().limit(5).collect(Collectors.toList());
	}

}
