package rx;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import rx.subjects.PublishSubject;

import javax.inject.Inject;

class RxControlerListener implements StartupListener, ShutdownListener {

	private final PublishSubject<Event> events;

	@Inject
	RxControlerListener(PublishSubject<Event> events, EventsManager eventsManager) {
		this.events = events;
		Observable<Event> eternalEvents = ObservableUtils.fromEventsManager(eventsManager);
		eternalEvents.subscribe(events);
	}

	@Override
	public void notifyStartup(StartupEvent event) {

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		events.onCompleted();
	}

}
