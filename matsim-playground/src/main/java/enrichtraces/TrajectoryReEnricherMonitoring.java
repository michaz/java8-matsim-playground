package enrichtraces;

import rx.Observable;

public interface TrajectoryReEnricherMonitoring {

	class DataPoint {
		public double lengthBefore;
		public double lengthAfter;
	}

	Observable<DataPoint> lengthBeforeVsAfter();

}
