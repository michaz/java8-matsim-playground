package rx;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import rx.subjects.PublishSubject;

import javax.inject.Inject;

class RxControlerListener implements StartupListener, ShutdownListener, IterationStartsListener, IterationEndsListener {

	public PublishSubject<Event> events() {
		return events;
	}

	public PublishSubject<ControlerEvent> controlerEvents() {
		return controlerEvents;
	}

	private final PublishSubject<Event> events = PublishSubject.create();
	private final PublishSubject<ControlerEvent> controlerEvents = PublishSubject.create();

	@Inject
	RxControlerListener(EventsManager eventsManager) {
		Observable<Event> eternalEvents = ObservableUtils.fromEventsManager(eventsManager);
		eternalEvents.subscribe(events);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		controlerEvents.onNext(event);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
		controlerEvents.onNext(iterationStartsEvent);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
		controlerEvents.onNext(iterationEndsEvent);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		controlerEvents.onNext(event);
		events.onCompleted();
	}


}
