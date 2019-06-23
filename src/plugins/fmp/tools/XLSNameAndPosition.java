package plugins.fmp.tools;

import java.awt.Point;

public class XLSNameAndPosition {
	String 	name;
	int column;
	int row;

	XLSNameAndPosition (String name, Point pt) {
		this.name = name;
		column = pt.x;
		row = pt.y;
	}
}
