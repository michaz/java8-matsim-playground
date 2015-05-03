package enrichtraces;

import org.matsim.api.core.v01.Id;
import playground.mzilske.cdr.Sighting;
import playground.mzilske.cdr.Sightings;
import playground.mzilske.populationsize.ExperimentResource;
import playground.mzilske.populationsize.MultiRateRunResource;
import playground.mzilske.populationsize.RegimeResource;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrajectoryEnrichmentScript {

    public static void main(String[] args) throws IOException {
        final Map<Id, List<Sighting>> dense = new HashMap<>();
        final Map<Id, List<Sighting>> sparse = new HashMap<>();
        final ExperimentResource experiment = new ExperimentResource("/Users/michaelzilske/runs-svn/synthetic-cdr/transportation/berlin/");
        final RegimeResource regime = experiment.getRegime("uncongested3");
        MultiRateRunResource multiRateRun = regime.getMultiRateRun("randomcountlocations100.0");
        Sightings sightings = multiRateRun.getSightings("5");
        for (Map.Entry<Id, List<Sighting>> entry : sightings.getSightingsPerPerson().entrySet()) {
            if (entry.getValue().size() > 20) {
                dense.put(entry.getKey(), entry.getValue());
            } else {
                sparse.put(entry.getKey(), entry.getValue());
            }
        }
        DistanceCalculator distanceCalculator = new DistanceCalculator(multiRateRun.getBaseRun().getConfigAndNetwork().getNetwork());
        ArrayList<Map.Entry<Id, List<Sighting>>> denseList = new ArrayList<>(dense.entrySet());

        try (FileWriter fw = new FileWriter("output/histogram.txt"); FileWriter fw2 = new FileWriter("output/scatter.txt")) {
            sparse.values().forEach(s -> {
                try {
                    fw.append(String.format("%f\tsparse\n", distanceCalculator.distance(s)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            denseList.forEach(d -> {
                try {
                    fw.append(String.format("%f\tdense\n", distanceCalculator.distance(d.getValue())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            sparse.values().forEach(s -> {
                distanceCalculator.sortDenseByProximityToSparse(s, denseList);
                Map.Entry<Id, List<Sighting>> best = denseList.get(0);
                List<Sighting> enriched = new ArrayList<>(s);
                TrajectoryEnricher trajectoryEnricher = new TrajectoryEnricher(distanceCalculator, enriched, best.getValue());
                trajectoryEnricher.drehStreckAll();
                try {
                    fw2.append(String.format("%f\t%f\t%f\n", distanceCalculator.distance(s), distanceCalculator.distance(best.getValue()), distanceCalculator.distance(enriched)));
                    fw2.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    fw.append(String.format("%f\texpanded\n",distanceCalculator.distance(enriched)));
                    fw.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

}
