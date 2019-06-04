package plugins.fmp.sequencevirtual;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;

public class PositionsXYT  implements XMLPersistent  {
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

	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		
		Element xmlVal = XMLUtil.getElement(node, "PositionsList");
		if (xmlVal == null) 
			return false;
		
		pointsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			// todo add node for each point
			Point2D point = new Point2D.Double(0,0);
			double x =  XMLUtil.getAttributeDoubleValue( xmlVal, "x", 0);
			double y =  XMLUtil.getAttributeDoubleValue( xmlVal, "y", 0);
			point.setLocation(x, y);
			pointsList.add(point);
		}
		return true;
	}

//	private boolean xmlLoadCagesLimits(Node node) {
//	if (node == null)
//		return false;
//	Element xmlVal = XMLUtil.getElement(node, "CagesLimits");
//	if (xmlVal == null) 
//		return false;
//	
//	cageLimitROIList.clear();
//	int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
//	for (int i=0; i< nb_items; i++) {
//		PositionsXYT pos = new PositionsXYT();
//		pos.loadFromXML(node);
//		cagePositionsList.add(pos);
//	}
//	return true;
//}
	
	@Override
	public boolean saveToXML(Node node) {
		// TODO Auto-generated method stub
		return false;
	}
	
//	private boolean xmlSaveCagesLimits(Node node) {
//		if (node == null)
//			return false;
//		Element xmlVal = XMLUtil.addElement(node, "CagesLimits");
//		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", cageLimitROIList.size());
//		for (PositionsXYT pos: cagePositionsList)
//			pos.saveToXML(xmlVal);
//		return true;
//	}
	

}
