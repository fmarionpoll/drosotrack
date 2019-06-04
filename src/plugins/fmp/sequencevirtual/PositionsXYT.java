package plugins.fmp.sequencevirtual;

import java.awt.geom.Point2D;
import java.util.ArrayList;

public class PositionsXYT {
	public ArrayList<Point2D> pointsList = new ArrayList<Point2D>();
	
	
	public void ensureCapacity(int minCapacity) {
		pointsList.ensureCapacity(minCapacity);
	}

	public Point2D get(int i) {
		return pointsList.get(i);
	}

	public void add(Point2D flyPosition) {
		pointsList.add(flyPosition);
		
	}
}
