package rx;

import com.google.inject.Inject;
import org.matsim.core.controler.ControlerI;

public class MatsimRx {

	@Inject
	ControlerI controler;

	@Inject
	Observable<IterationRx> iterations;

	public Observable<IterationRx> iterations() {
		return iterations;
	};

	public void run() {
		controler.run();
	}

}
