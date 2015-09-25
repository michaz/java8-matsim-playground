package segmenttraces;

import cdr.Sightings;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.SlidingGanttCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import populationsize.ExperimentResource;
import populationsize.IterationResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ActivityTimelineChart {

	public static void main(String[] args) {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource uncongested = experiment.getRegime("uncongested3");
		MultiRateRunResource multiRateRun = uncongested.getMultiRateRun("onlyheavyusers-nothing");
		IterationResource iteration = multiRateRun.getRateRun("50.0", "1").getIteration(0);
		Network network = uncongested.getBaseRun().getConfigAndNetwork().getNetwork();
		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(uncongested.getBaseRun().getLastIteration(), network);
		Sightings sightings = multiRateRun.getSightings("50.0");
		TaskSeriesCollection taskSeriesCollection = new TaskSeriesCollection();

		taskSeriesCollection.add(getSightings(sightings));
		taskSeriesCollection.add(getTaskSeries("Original", originalPopulation));
		taskSeriesCollection.add(getTaskSeries("Reconstructed", population));

		SlidingGanttCategoryDataset dataset = new SlidingGanttCategoryDataset(taskSeriesCollection, 0, 5);
		JFreeChart ganttChart = ChartFactory.createGanttChart("Activities", "Agent", "Time", dataset);
		GanttRenderer renderer = (GanttRenderer) ((CategoryPlot) ganttChart.getPlot()).getRenderer();
		renderer.setDrawBarOutline(true);
		renderer.setSeriesPaint(0, Color.BLACK);
		renderer.setSeriesPaint(1, Color.BLUE);
		renderer.setSeriesPaint(2, Color.RED);
		renderer.setSeriesOutlinePaint(0, Color.BLACK);
		renderer.setSeriesOutlinePaint(1, Color.BLUE);
		renderer.setSeriesOutlinePaint(2, Color.RED);
		renderer.setSeriesOutlineStroke(0, new BasicStroke(1.0F));

		ChartPanel chartpanel = new ChartPanel(ganttChart);
		chartpanel.setPreferredSize(new Dimension(400, 400));
		JScrollBar scroller = new JScrollBar(1, 0, 5, 0, sightings.getSightingsPerPerson().size());
		scroller.getModel().addChangeListener(e -> dataset.setFirstCategoryIndex(scroller.getValue()));

		JFrame frame = new JFrame("Activities");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(chartpanel);
		frame.getContentPane().add(scroller, "East");

		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	static TaskSeries getTaskSeries(String name, Map<Id<Person>, Plan> population) {
		TaskSeries activities = new TaskSeries(name);
		population.forEach((id, plan) -> {
			Task t1 = new Task(id.toString(), new Date(0), new Date((24+8) * 60 * 60 * 1000));

			int i=0;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					double startTime = act.getStartTime();
					if (startTime == Time.UNDEFINED_TIME) {
						startTime = 0;
					}
					double endTime = act.getEndTime();
					if (endTime == Time.UNDEFINED_TIME) {
						endTime = (24 + 8) * 60 * 60;
					}
					t1.addSubtask(new Task(act.getType() + i, new Date((long) startTime * 1000), new Date((long) endTime * 1000)));
					i++;
				}
			}
			activities.add(t1);
		});
		return activities;
	}

	static TaskSeries getSightings(Sightings sightings) {
		TaskSeries activities = new TaskSeries("Sightings");
		sightings.getSightingsPerPerson().forEach((id, sightingsPerPerson) -> {
			final int[] i = {0};
			Task t1 = new Task(id.toString(), new Date(0), new Date((24+8) * 60 * 60 * 1000));
			sightingsPerPerson.stream().forEach(sighting -> {
				t1.addSubtask(new Task("sighting" + i[0], new Date((long) sighting.getTime()*1000), new Date(((long) sighting.getTime()+10)*1000)));
				i[0]++;
			});
			activities.add(t1);
			System.out.println(sightingsPerPerson.size());
		});
		return activities;
	}

	static Map<Id<Person>, Plan> getExperiencedPlans(IterationResource iteration, Network network) {
		ScenarioUtils.ScenarioBuilder scenarioBuilder = new ScenarioUtils.ScenarioBuilder(ConfigUtils.createConfig());
		scenarioBuilder.setNetwork(network);
		scenarioBuilder.setPopulation(iteration.getPlans());
		Scenario scenario = scenarioBuilder.build();
		EventsToScore eventsToScore = new EventsToScore(scenario, new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				return new ScoringFunction() {
					@Override
					public void handleActivity(Activity activity) {

					}

					@Override
					public void handleLeg(Leg leg) {

					}

					@Override
					public void agentStuck(double time) {

					}

					@Override
					public void addMoney(double amount) {

					}

					@Override
					public void finish() {

					}

					@Override
					public double getScore() {
						return 0;
					}

					@Override
					public void handleEvent(Event event) {

					}
				};
			}
		});
		ReplayEvents.run(scenario, iteration.getEventsFileName(), new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(eventsToScore);
			}
		});
		eventsToScore.finish();
		return eventsToScore.getAgentRecords();
	}

}
