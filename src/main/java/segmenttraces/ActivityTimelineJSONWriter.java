package segmenttraces;

import cdr.Sighting;
import cdr.Sightings;
import com.google.gson.Gson;
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ActivityTimelineJSONWriter {

	static class DataSet {
		List<Col> cols = new ArrayList<>();
		List<Row> rows = new ArrayList<>();
	}

	static class Col {
		String id;
		String type;
	}

	static class Row {
		List<Entry> c = new ArrayList<>();
	}

	static class Entry {
		public Entry(String v, String f) {
			this.v = v;
			this.f = f;
		}

		String v;
		String f;
	}
	public static void main(String[] args) throws IOException {
		final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
		final RegimeResource uncongested = experiment.getRegime("uncongested3");
		MultiRateRunResource multiRateRun = uncongested.getMultiRateRun("onlyheavyusers-noenrichment-segmentation");
		IterationResource iteration = multiRateRun.getRateRun("50.0", "1").getIteration(0);
		Network network = uncongested.getBaseRun().getConfigAndNetwork().getNetwork();
		Map<Id<Person>, Plan> population = getExperiencedPlans(iteration, network);
//		Map<Id<Person>, Plan> originalPopulation = getExperiencedPlans(uncongested.getBaseRun().getLastIteration(), network);
		Sightings sightings = multiRateRun.getSightings("50.0");

		Gson gson = new Gson();
		DataSet dataSet = new DataSet();
		{
			Col c = new Col();
			c.id="Agent ID";
			c.type="string";
			dataSet.cols.add(c);
		}
		{
			Col c = new Col();
			c.id="Activity";
			c.type="string";
			dataSet.cols.add(c);
		}
		{
			Col c = new Col();
			c.id="Begin";
			c.type="date";
			dataSet.cols.add(c);
		}
		{
			Col c = new Col();
			c.id="End";
			c.type="date";
			dataSet.cols.add(c);
		}

		dataSet.rows.addAll(getTaskSeries("Reconstructed", population, sightings));
		FileWriter writer = new FileWriter("src/main/js/pups.json");
		gson.toJson(dataSet, writer);
		writer.close();
	}

	private static List<Row> getTaskSeries(String name, Map<Id<Person>, Plan> population, Sightings sightings) {
		List<Row> result = new ArrayList<>();
		population.entrySet().stream().limit(100).forEach((en) -> {
			int i = 0;
			Plan plan = en.getValue();
			Id id = en.getKey();
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
					if (endTime < startTime) {
						throw new RuntimeException();
					}
					Row e = new Row();
					e.c.add(new Entry(id.toString()+" activities", null));
					e.c.add(new Entry(act.getType()+i, null));
					e.c.add(new Entry(toPissString(startTime), null));
					e.c.add(new Entry(toPissString(endTime), null));
					result.add(e);
					i++;
				}
			}
			for (Sighting sighting : sightings.getSightingsPerPerson().get(id)) {
				Row e = new Row();
				e.c.add(new Entry(id.toString()+" sightings", null));
				e.c.add(new Entry("sighting"+i, null));
				e.c.add(new Entry(toPissString(sighting.getTime()), null));
				e.c.add(new Entry(toPissString(sighting.getTime()), null));
				result.add(e);
				i++;
			}
		});
		return result;
	}

	private static String toPissString(double startTime) {
		int s = (int) startTime;
		long h = (long)(s / 3600);
		s = s % 3600;
		int m = (int)(s / 60);
		s = s % 60;
		return "Date(1970,1,1,"+h+","+m+","+s+")";
	}


	private static Map<Id<Person>, Plan> getExperiencedPlans(IterationResource iteration, Network network) {
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
