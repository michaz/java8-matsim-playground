package segmenttraces;

import cdr.Sightings;
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
import org.matsim.core.utils.io.UncheckedIOException;
import populationsize.ExperimentResource;
import populationsize.IterationResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static segmenttraces.ActivityTimelineChart.*;

public class RunActivityTimelineFigures {

	public static void main(String[] args) {
		Set<Id<Person>> agents = getAgentIds();
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource uncongested = experiment.getRegime("uncongested3");
		MultiRateRunResource multiRateRun = uncongested.getMultiRateRun("onlyheavyusers-noenrichment-segmentation10minutes");
		IterationResource iteration = multiRateRun.getRateRun("50.0", "1").getIteration(0);
		Network network = uncongested.getBaseRun().getConfigAndNetwork().getNetwork();
		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
		population.keySet().retainAll(agents);
		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(uncongested.getBaseRun().getLastIteration(), network);
		originalPopulation.keySet().retainAll(agents);
		Sightings sightings = multiRateRun.getSightings("50.0");
		sightings.getSightingsPerPerson().keySet().retainAll(agents);

		TaskSeriesCollection taskSeriesCollection = new TaskSeriesCollection();

		taskSeriesCollection.add(getSightings(sightings));
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
			ChartUtilities.saveChartAsPNG(new File("output/activity-structure.png"), ganttChart, 1024, 768);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	static Set<Id<Person>> getAgentIds() {
		Set<Id<Person>> agents = new HashSet<>();
		agents.add(Id.createPersonId("24122484"));
		agents.add(Id.createPersonId("14104774"));
		agents.add(Id.createPersonId("23135904"));
		return agents;
	}

}
