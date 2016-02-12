package rx;

import org.junit.Test;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorNotImplementedException;
import rx.subjects.PublishSubject;

public class RxUnderstandingTest {

	@Test
	public void testExceptionInSubscriber() {
		PublishSubject<String> publishSubject = PublishSubject.create();
		publishSubject.subscribe(s -> {throw new RuntimeException(s);}, Throwable::printStackTrace);
		publishSubject.onNext("Wurst");
	}

	@Test(expected = OnErrorNotImplementedException.class)
	public void testExceptionInSubscriberWithoutHandling() {
		PublishSubject<String> publishSubject = PublishSubject.create();
		publishSubject.subscribe(s -> {throw new RuntimeException(s);});
		publishSubject.onNext("Wurst");
	}

	@Test(expected = OnErrorFailedException.class)
	public void testExceptionInSubscriberAndOnError() {
		PublishSubject<String> publishSubject = PublishSubject.create();
		publishSubject.subscribe(s -> {throw new RuntimeException(s);}, t -> {throw new RuntimeException(t);});
		publishSubject.onNext("Wurst");
	}

}
