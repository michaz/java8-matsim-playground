package enrichtraces;

import cdr.Sighting;
import cdr.Sightings;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import populationsize.ExperimentResource;
import populationsize.MultiRateRunResource;
import populationsize.RegimeResource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;



/*

Dense:
DescriptiveStatistics:
n: 9523
min: 0.0
max: 473511.74367474805
mean: 31115.100901202604
std dev: 29680.59487242061
median: 23781.409998918167
skewness: 3.37227329119894
kurtosis: 22.068842685986798

Sparse:
DescriptiveStatistics:
n: 8818
min: 0.0
max: 261765.59972849258
mean: 11783.754365262212
std dev: 22239.533742504143
median: 3013.534553861539
skewness: 4.115268796042307
kurtosis: 24.82976552289355

Adapted:
DescriptiveStatistics:
n: 8818
min: 0.0
max: 498771.969190614
mean: 25827.36198421404
std dev: 30721.87597305989
median: 21079.711683792197
skewness: 3.768302050949663
kurtosis: 24.630108699938233

Compare: http://research.microsoft.com/en-us/um/people/jckrumm/Publications%202012/2012-01-0489%20SAE%20published.pdf


Removed dupes:

Dense:
DescriptiveStatistics:
n: 9400
min: 0.0
max: 473511.74367474805
mean: 31228.704985971694
std dev: 29752.54957535047
median: 23865.07387981683
skewness: 3.3775128541939043
kurtosis: 22.080312793402587

Sparse:
DescriptiveStatistics:
n: 7978
min: 0.0
max: 261765.59972849258
mean: 11437.65732225337
std dev: 21501.37718000461
median: 3022.6625950819416
skewness: 4.197668580314432
kurtosis: 26.60844209510826

Adapted:
DescriptiveStatistics:
n: 7978
min: 0.0
max: 498771.969190614
mean: 25322.394173820383
std dev: 29768.18203656562
median: 20674.39472174456
skewness: 3.7449440303773045
kurtosis: 25.096505111563573

Adapted with sample of 500:
DescriptiveStatistics:
n: 7978
min: 0.0
max: 552852.7304149707
mean: 23616.100613090068
std dev: 32755.155332056842
median: 14429.212377185815
skewness: 4.063384156064024
kurtosis: 29.611448570372037
 */

public class RunTrajectoryEnrichment {

	public void start() throws IOException {
		try (FileWriter fw = new FileWriter("output/histogram.txt"); FileWriter fw2 = new FileWriter("output/scatter.txt")) {

			final Map<Id, List<Sighting>> dense = new HashMap<>();
			final Map<Id, List<Sighting>> sparse = new HashMap<>();
			final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
			final RegimeResource regime = experiment.getRegime("uncongested3");
			MultiRateRunResource multiRateRun = regime.getMultiRateRun("realcountlocations100.0");
			Sightings sightings = multiRateRun.getSightings("5");
			filterDupes(sightings);


			DistanceCalculator distanceCalculator = new DistanceCalculator(multiRateRun.getBaseRun().getConfigAndNetwork().getNetwork());

			for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
				if (entry.getValue().size() > 20) {
					dense.put(entry.getKey(), entry.getValue());
				} else {
					sparse.put(entry.getKey(), entry.getValue());
				}
			}

			double[] denseDistances = dense.values().stream()
					.mapToDouble(distanceCalculator::distance)
					.toArray();
			System.out.println(new DescriptiveStatistics(denseDistances));
			Arrays.stream(denseDistances).forEach(d -> {
				try {
					fw.append(String.format("%f\tdense\n", d));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			double[] sparseDistances = sparse.values().stream()
					.mapToDouble(distanceCalculator::distance)
					.toArray();
			System.out.println(new DescriptiveStatistics(sparseDistances));
			Arrays.stream(sparseDistances).forEach(s -> {
				try {
					fw.append(String.format("%f\tsparse\n", s));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			double[] expandedDistances = sparse.values().parallelStream()
					.mapToDouble(s -> {
						List<Map.Entry<Id, List<Sighting>>> denseList = new ArrayList<>(dense.entrySet());
						Collections.shuffle(denseList);
						denseList = denseList.subList(0, 500);
						distanceCalculator.sortDenseByProximityToSparse(s, denseList);
						Map.Entry<Id, List<Sighting>> best = denseList.get(0);
						List<Sighting> enriched = new ArrayList<>(s);
						TrajectoryEnricher trajectoryEnricher = new TrajectoryEnricher(distanceCalculator, enriched, best.getValue());
						trajectoryEnricher.drehStreckAll();
						double enrichedDistance = distanceCalculator.distance(enriched);
						try {
							fw.append(String.format("%f\texpanded\n", enrichedDistance));
							fw.flush();
							fw2.append(String.format("%f\t%f\t%f\n", distanceCalculator.distance(s), distanceCalculator.distance(best.getValue()), enrichedDistance));
							fw2.flush();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						return enrichedDistance;
					}).toArray();
			System.out.println(new DescriptiveStatistics(expandedDistances));
		}
	}

	private void filterDupes(Sightings sightings) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile("/Users/michaelzilske/shared-svn/studies/countries/de/berlin/plans/baseplan_car_only.xml.gz");
		System.out.println(scenario.getPopulation().getPersons().size());
		System.out.println(sightings.getSightingsPerPerson().size());

		Set<String> fingerPrints = new HashSet<>();
		Iterator<? extends Map.Entry<Id<Person>, ? extends Person>> i = scenario.getPopulation().getPersons().entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<Id<Person>, ? extends Person> next = i.next();
			Person person = next.getValue();
			if (fingerPrints.contains(fingerPrint(person.getSelectedPlan()))) {
				i.remove();
				sightings.getSightingsPerPerson().remove(person.getId());
			}
			fingerPrints.add(fingerPrint(person.getSelectedPlan()));
		}
		System.out.println(scenario.getPopulation().getPersons().size());
		System.out.println(sightings.getSightingsPerPerson().size());
	}

	private String fingerPrint(Plan selectedPlan) {
		if (selectedPlan.getPlanElements().isEmpty()) {
			return Integer.toString(selectedPlan.hashCode());
		} else {
			return selectedPlan.getPlanElements().stream()
					.filter(pe -> pe instanceof Activity)
					.map(pe -> (Activity) pe)
					.map(act -> act.getCoord().toString())
					.collect(Collectors.joining());
		}
	}

	public static void main(String[] args) throws IOException {
		new RunTrajectoryEnrichment().start();
	}

}
