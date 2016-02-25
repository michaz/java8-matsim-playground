package inject;

public class Controller2 {

	public Controller2(RainScoringFunctionFactory myScoringFunctionFactory, TripRouterImpl tripRouter) {

	}

	public static void main(String[] args) {
		BikeTravelTime bikeTravelTime = new BikeTravelTime();
		TripRouterImpl tripRouter = new TripRouterImpl(bikeTravelTime);
		RainScoringFunctionFactory rainScoringFunctionFactory = new RainScoringFunctionFactory(tripRouter);
		Controller controller = new Controller(rainScoringFunctionFactory, tripRouter);
	}

}
