package rx;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import rx.subjects.PublishSubject;

public class IterationRx {

	PublishSubject<Event> iterationEvents;

	IterationRx(Observable<ControlerEvent> controlerEvents, Observable<Event> events) {
		iterationEvents = PublishSubject.create();
		Subscription subscription = events.subscribe(iterationEvents);
		controlerEvents.takeFirst(e -> e instanceof IterationEndsEvent).forEach(e -> {
			subscription.unsubscribe();
			iterationEvents.onCompleted();
		});
	}

	public Observable<Event> events() {
		return iterationEvents;
	}

}
