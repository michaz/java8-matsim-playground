package inject;

import org.matsim.core.controler.AbstractModule;

public class ExtensionModule extends AbstractModule {
	@Override
	public void install() {
		bindScoringFunctionFactory().to(RainScoringFunctionFactory.class);

	}
}
