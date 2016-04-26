package segmenttraces;

import cdr.Sighting;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

import java.util.Map;

public class SightingWrapperFacility implements Facility {
	private final Sighting sighting;

	public SightingWrapperFacility(Sighting sighting) {
		this.sighting = sighting;
	}

	@Override
	public Id<Link> getLinkId() {
		return Id.createLinkId(sighting.getCellTowerId());
	}

	@Override
	public Coord getCoord() {
		return null;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return null;
	}

	@Override
	public Id getId() {
		return null;
	}
}
