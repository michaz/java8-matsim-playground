package java8;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public class FilterAndPredicate {

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Collection<Id<Person>> ids = new ArrayList<>();
        Predicate<Identifiable<Person>> idPredicate = p -> ids.contains(p.getId());
        scenario.getPopulation().getPersons().keySet().stream()
                .filter(id -> idPredicate.test(scenario.getPopulation().getPersons().get(id)));
        // Geht das kürzer oder funktionaler? Nach einem Predicate filtern, was ich auf f(elem) anwende statt auf elem?
    }

}
