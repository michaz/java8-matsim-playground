package rx;

import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.AbstractModule;
import rx.subjects.PublishSubject;

public class RxModule extends AbstractModule {
	@Override
	public void install() {
		PublishSubject<Event> events = PublishSubject.create();
		bind(new TypeLiteral<PublishSubject<Event>>(){}).toInstance(events); // private
		bind(new TypeLiteral<Observable<Event>>(){}).toInstance(events); // public
		addControlerListenerBinding().to(RxControlerListener.class);
	}
}
