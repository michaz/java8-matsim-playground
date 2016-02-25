package reactor;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import rx.subscriptions.Subscriptions;

public class FluxUtils {

	public static Flux<Event> fromEventsManager(EventsManager eventsManager) {
		return Flux.create(subscriber -> {
			final BasicEventHandler handler = new BasicEventHandler() {
				@Override
				public void handleEvent(Event event) {
					subscriber.onNext(event);
				}

				@Override
				public void reset(int iteration) {

				}
			};
			eventsManager.addHandler(handler);
		});
	}

}
