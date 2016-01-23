package rx;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import rx.subjects.PublishSubject;

public class RxModule extends AbstractModule {
	@Override
	public void install() {
		bind(MatsimRx.class).asEagerSingleton();
		bind(RxControlerListener.class).asEagerSingleton();
		addControlerListenerBinding().to(RxControlerListener.class);
	}

	@Provides @Singleton
	Observable<IterationRx> iterations(Observable<ControlerEvent> controlerEvents, Observable<Event> events) {
		PublishSubject<IterationRx> iterations = PublishSubject.create();
		controlerEvents.filter(e -> e instanceof IterationStartsEvent).forEach(e -> {
			IterationRx currentIteration = new IterationRx(controlerEvents, events);
			iterations.onNext(currentIteration);
		});
		controlerEvents.filter(e -> e instanceof ShutdownEvent).forEach(e -> {
			iterations.onCompleted();
		});
		return iterations;
	}

	@Provides @Singleton
	Observable<ControlerEvent> controlerEvents(RxControlerListener cl) {
		return cl.controlerEvents();
	}

	@Provides @Singleton
	Observable<Event> events(RxControlerListener cl) {
		return cl.events();
	}


}
