package reactor;

import org.junit.Test;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.TopicProcessor;
import reactor.core.subscriber.Subscribers;

public class ReactorUnderstandingTest {

	@Test
	public void testTPAwaitsCompletion() throws InterruptedException {
		TopicProcessor<String> processor = TopicProcessor.create();
		processor.subscribe(Subscribers.consumer(s -> System.out.println("TP1: " + s + " " + Thread.currentThread())));
		processor.subscribe(Subscribers.consumer(s -> System.out.println("TP2: " + s + " " + Thread.currentThread())));
		Flux.fromArray(new String[]{"Wurst", "Wurst", "Blubb"}).subscribe(processor);
		// No idea what this does. Produces too many values.
	}


	@Test
	public void testEPAwaitsCompletion() throws InterruptedException {
		EmitterProcessor<String> processor = EmitterProcessor.create();
		processor.subscribe(Subscribers.consumer(s -> System.out.println("EP1: " + s + " " + Thread.currentThread())));
		processor.subscribe(Subscribers.consumer(s -> System.out.println("EP2: " + s + " " + Thread.currentThread())));
		Flux.fromArray(new String[]{"Wurst", "Wurst", "Blubb"}).subscribe(processor);
		// But when I simply call on.. on the emitterprocessor, it doesn't await.
		// And also this only awaits shutdown of JUnit (probably on a ShutdownHook.
		// .. no it doesn't. Pure coincidence.
	}

	@Test
	public void testExceptionInSubscriber() throws InterruptedException {
		TopicProcessor<String> processor = TopicProcessor.create();
		processor.subscribe(Subscribers.consumer(s -> {
			if (s.equals("Blubb")) {
				throw new RuntimeException(s);
			}
			System.out.println("Q: " + s + " " + Thread.currentThread());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, Throwable::printStackTrace));
		Flux.fromArray(new String[]{"Wurst", "Wurst", "Blubb"}).subscribe(processor);
	}

	@Test
	public void testExceptionInSubscriberWithNoOnError() throws InterruptedException {
		// The rx PublishSubject would throw an Exception here
		TopicProcessor<String> processor = TopicProcessor.create();
		processor.subscribe(Subscribers.consumer(s -> {
			if (s.equals("Blubb")) {
				throw new RuntimeException(s);
			}
			System.out.println("R: " + s + " " + Thread.currentThread());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}));
		Flux.fromArray(new String[]{"Wurst", "Wurst", "Blubb"}).subscribe(processor);
	}

}
