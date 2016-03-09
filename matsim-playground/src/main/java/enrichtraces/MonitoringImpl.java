package enrichtraces;

import rx.Observable;
import rx.subjects.PublishSubject;

class MonitoringImpl implements TrajectoryReEnricherMonitoring {

	PublishSubject<DataPoint> lengthBeforeVsAfter = PublishSubject.create();

	@Override
	public Observable<DataPoint> lengthBeforeVsAfter() {
		return lengthBeforeVsAfter;
	}

}
