package inject;


public class Controller {

	public static void main(String[] args) {
		Controller controller = new Controller();
		controller.setScoringFunctionFactory(new RainScoringFunctionFactory());
		controller.addTravelTime("bike", new BikeTravelTime());
	}

	public Controller(RainScoringFunctionFactory myScoringFunctionFactory, TripRouterImpl tripRouter) {

	}

	public Controller() {

	}

	private void addTravelTime(String bike, BikeTravelTime bikeTravelTime) {

	}

	public void setScoringFunctionFactory(RainScoringFunctionFactory scoringFunctionFactory) {
	}
}
