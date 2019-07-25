package plugins.fmp.toolsMulticafe;

import java.awt.Point;
import java.nio.file.attribute.FileTime;

public class XLSNameAndPosition {
	String 		name;
	int 		column;
	int 		row;
	long		fileTimeImageFirstMinutes 	= 0;
	long		fileTimeImageLastMinutes 	= 0;
	long 		fileTimeSpan = 0;
	FileTime	fileTimeImageFirst;
	FileTime	fileTimeImageLast;
	
	XLSNameAndPosition (String name, Point pt) {
		this.name = name;
		column = pt.x;
		row = pt.y;
	}
	
	XLSNameAndPosition (String name) {
		this.name = name;
	}
}
