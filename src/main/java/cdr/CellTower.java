package cdr;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;

public class CellTower {
	
	public final String id;
	public final Coord coord;
	public Geometry cell;
	
	
	public int nSightings = 0;
	
	public CellTower(String id, Coord coord) {
		super();
		this.id = id;
		this.coord = coord;
	}
	
}