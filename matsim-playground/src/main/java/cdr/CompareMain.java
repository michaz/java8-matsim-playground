package cdr;

import cadyts.CadytsModule;
import clones.ClonesConfigGroup;
import clones.ClonesModule;
import jfastemd.Feature;
import jfastemd.JFastEMD;
import jfastemd.Signature;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsPlanChanger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import populationsize.CadytsAndCloneScoringFunctionFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
public class CompareMain {

    private final Sightings sightings;

    private static class VolumeOnLinkFeature implements Feature {

		final Link link;
		final int hour;

		public VolumeOnLinkFeature(Link link, int i) {
			this.link = link;
			this.hour = i;
		}

		@Override
		public double groundDist(Feature f) {
			VolumeOnLinkFeature other = (VolumeOnLinkFeature) f;
			double spacialDistance = CoordUtils.calcEuclideanDistance(link.getCoord(), other.link.getCoord());
			int temporalDistance = Math.abs(hour - other.hour);
			return (0.0 * spacialDistance) + (2.0 * temporalDistance);
		}

	}

	private static final int TIME_BIN_SIZE = 60*60;
	private static final int MAX_TIME = 24 * TIME_BIN_SIZE - 1;

    public VolumesAnalyzer getGroundTruthVolumes() {
        return groundTruthVolumes;
    }

    private VolumesAnalyzer groundTruthVolumes;
	private ZoneTracker.LinkToZoneResolver linkToZoneResolver;
	private Scenario scenario;





	static double compareEMD(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
		Network network = scenario.getNetwork();
		Signature signature1 = makeSignature(network, groundTruthVolumes);
		Signature signature2 = makeSignature(network, cdrVolumes);
        return JFastEMD.distance(signature1, signature2, -1.0);
	}

	static double compareTimebins(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
		double sum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			int[] volumesForLink1 = getVolumesForLink(groundTruthVolumes, link);
			int[] volumesForLink2 = getVolumesForLink(cdrVolumes, link);
			for (int i = 0; i < volumesForLink1.length; ++i) {
				int diff = volumesForLink1[i] - volumesForLink2[i];
				sum += Math.abs(diff) * link.getLength();
				if (diff != 0) {
					System.out.println(Arrays.toString(volumesForLink1));
					System.out.println(Arrays.toString(volumesForLink2));
					System.out.println("=== " + link.getId());
				}
			}
		}
		return sum;
	}


    static double compareAllDay(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
		double sum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
            int[] volumesForLink1 = getVolumesForLink(groundTruthVolumes, link);
            int[] volumesForLink2 = getVolumesForLink(cdrVolumes, link);
			int sum1 = 0;
			int sum2 = 0;
			for (int i = 0; i < volumesForLink1.length; ++i) {
				sum1 += volumesForLink1[i];
				sum2 += volumesForLink2[i];
			}
			int diff = sum2 - sum1;
			sum += Math.abs(diff) * link.getLength();
		}
		return sum;
	}

	static double compareEMDMassPerLink(Scenario scenario, VolumesAnalyzer cdrVolumes, VolumesAnalyzer groundTruthVolumes) {
		double sum = 0;
		double lengthsum = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
            int[] volumesForLink1 = getVolumesForLink(groundTruthVolumes, link);
            int[] volumesForLink2 = getVolumesForLink(cdrVolumes, link);
			double emd =  MatchDistance.emd(MatchDistance.int2double(volumesForLink1), MatchDistance.int2double(volumesForLink2));
			if (! Double.isNaN(emd)) {
				lengthsum += link.getLength();
				sum += link.getLength() *emd;
			}
		}
		return sum / lengthsum;
	}

	public static Counts volumesToCounts(Network network, VolumesAnalyzer volumesAnalyzer, double scaleFactor) {
		Counts counts = new Counts();
		for (Link link : network.getLinks().values()) {
			Count count = counts.createAndAddCount(link.getId(), link.getId().toString());
			int[] volumesForLink = getVolumesForLink(volumesAnalyzer, link);
			int h = 1;
			for (int v : volumesForLink) {
				count.createVolume(h, v * scaleFactor);
				++h;
			}
		}
		return counts;
	}

    @Inject
	public CompareMain(Scenario scenario, Sightings sightings, ZoneTracker.LinkToZoneResolver linkToZoneResolver, VolumesAnalyzer volumesAnalyzer) {
		super();
		this.scenario = scenario;
        this.sightings = sightings;
        this.groundTruthVolumes = volumesAnalyzer;
        this.linkToZoneResolver = linkToZoneResolver;
	}

	private static Signature makeSignature(Network network, VolumesAnalyzer volumesAnalyzer) {
		int n = 0;
		for (Link link : network.getLinks().values()) {
			int[] volumesForLink = getVolumesForLink(volumesAnalyzer, link);
            for (int aVolumesForLink : volumesForLink) {
                if (aVolumesForLink != 0) {
                    ++n;
                }
            }
		}
		Feature[] features = new Feature[n];
		double[] weights = new double[n];
		n = 0;
		for (Link link : network.getLinks().values()) {
			int[] volumesForLink = getVolumesForLink(volumesAnalyzer, link);
			for (int i=0; i<volumesForLink.length;++i) {
				if (volumesForLink[i] != 0) {
					Feature feature = new VolumeOnLinkFeature(link, i);
					features[n] = feature;
					weights[n] = volumesForLink[i];
					++n;
				}
			}
		}
		System.out.println(n);
		Signature signature = new Signature();
		signature.setFeatures(features);
		signature.setNumberOfFeatures(n);
		signature.setWeights(weights);
		return signature;
	}

	public static VolumesAnalyzer runWithTwoPlansAndCadyts(String outputDirectory, Network network, final ZoneTracker.LinkToZoneResolver linkToZoneResolver, Sightings allSightings, Counts counts) {
		Config config = ConfigUtils.createConfig();
		final double cadytsWeight = 100.0;
		ActivityParams sightingParam = new ActivityParams("sighting");
		sightingParam.setTypicalDuration(30.0 * 60);
		config.planCalcScore().addActivityParams(sightingParam);
		config.planCalcScore().setPerforming_utils_hr(0);
		config.planCalcScore().getModes().get("car").setMarginalUtilityOfTraveling(-6);
		config.planCalcScore().getModes().get("car").setConstant(0);
		config.planCalcScore().getModes().get("car").setMonetaryDistanceRate(0);
		config.planCalcScore().setWriteExperiencedPlans(true);
        config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(10);
		config.controler().setCreateGraphs(false);
		config.qsim().setFlowCapFactor(100);
		config.qsim().setStorageCapFactor(100);
		config.qsim().setRemoveStuckVehicles(false);
		StrategySettings stratSets = new StrategySettings(Id.create(1, StrategySettings.class));
		stratSets.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		stratSets.setWeight(1.) ;
		config.strategy().addStrategySettings(stratSets);
		ClonesConfigGroup clonesConfigGroup = new ClonesConfigGroup();
		clonesConfigGroup.setCloneFactor(3.0);
		config.addModule(clonesConfigGroup);


		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		scenario.setNetwork(network);
		scenario.addScenarioElement("calibrationCounts", counts);
		PopulationFromSightings.createPopulationWithTwoPlansEach(scenario, linkToZoneResolver, allSightings);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CadytsModule());
		controler.addOverridingModule(new ClonesModule());
		CadytsAndCloneScoringFunctionFactory factory = new CadytsAndCloneScoringFunctionFactory();
		factory.setCadytsweight(cadytsWeight);
		controler.setScoringFunctionFactory(factory);
		controler.run();
		return controler.getVolumes();
	}


    public static double calcCadytsScore(final CadytsContext context, Plan plan) {
		cadyts.demand.Plan<Link> currentPlanSteps = context.getPlansTranslator().getCadytsPlan(plan);
        return context.getCalibrator().calcLinearPlanEffect(currentPlanSteps);
	}
	
	public static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Link link) {
		return getVolumesForLink(volumesAnalyzer1, link.getId());
	}

    public static int[] getVolumesForLink(VolumesAnalyzer volumesAnalyzer1, Id<Link> linkId) {
        int maxSlotIndex = (MAX_TIME / TIME_BIN_SIZE) + 1;
        int[] maybeVolumes = volumesAnalyzer1.getVolumesForLink(linkId);
        if(maybeVolumes == null) {
            return new int[maxSlotIndex + 1];
        }
        return maybeVolumes;
    }

}
