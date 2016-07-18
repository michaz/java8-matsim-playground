package inject;

public class Controller2 {

	public Controller2(RainScoringFunctionFactory myScoringFunctionFactory, TripRouter tripRouter) {

	}

	public static void main(String[] args) {
		BikeTravelTime bikeTravelTime = new BikeTravelTime();
		TripRouter tripRouter = new TripRouter(bikeTravelTime);
		RainScoringFunctionFactory rainScoringFunctionFactory = new RainScoringFunctionFactory(tripRouter);
		Controller controller = new Controller(rainScoringFunctionFactory, tripRouter);
	}

}
