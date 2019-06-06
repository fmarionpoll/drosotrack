package plugins.fmp.sequencevirtual;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.kernel.roi.roi2d.*;

public class PositionsXYT  implements XMLPersistent  {
	public ROI2DPolygon roi;
	public int lastTime = 0;
	public ArrayList<Point2D> pointsList = new ArrayList<Point2D>();
	public ArrayList<Integer> timeList = new ArrayList <Integer>();
	
	
	public PositionsXYT(ROI2D roi) {
		this.roi = (ROI2DPolygon) roi;
	}
	
	public PositionsXYT() {
		this.roi = new ROI2DPolygon();
	}
	
	public void ensureCapacity(int minCapacity) {
		pointsList.ensureCapacity(minCapacity);
		timeList.ensureCapacity(minCapacity);
	}

	public Point2D getPoint(int i) {
		return pointsList.get(i);
	}
	public int getTime(int i) {
		return timeList.get(i);
	}

	public void add(Point2D flyPosition, int frame) {
		pointsList.add(flyPosition);
		timeList.add(frame);
	}

	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		
		Element node_roi = XMLUtil.getElement(node, "roi");
		roi.loadFromXML(node_roi);
		
		Element node_lastime = XMLUtil.getElement(node, "lastTimeItMoved");
		lastTime = XMLUtil.getAttributeIntValue(node_lastime, "t", 0);

		Element node_position_list = XMLUtil.getElement(node, "PositionsList");
		if (node_position_list == null) 
			return false;
		
		pointsList.clear();
		timeList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(node_position_list, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			Point2D point = new Point2D.Double(0,0);
			double x =  XMLUtil.getAttributeDoubleValue( node_position_i, "x", 0);
			double y =  XMLUtil.getAttributeDoubleValue( node_position_i, "y", 0);
			point.setLocation(x, y);
			pointsList.add(point);
			int t =  (int) XMLUtil.getAttributeDoubleValue( node_position_i, "t", 0);
			timeList.add(t);
		}
		return true;
	}


	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		
		Element node_roi = XMLUtil.addElement(node, "roi");
		roi.saveToXML(node_roi);
		
		Element node_lastime = XMLUtil.addElement(node, "lastTimeItMoved");
		XMLUtil.setAttributeIntValue(node_lastime, "t", lastTime);
		
		Element node_position_list = XMLUtil.addElement(node, "PositionsList");
		XMLUtil.setAttributeIntValue(node_position_list, "nb_items", pointsList.size());
		
		int i = 0;
		for (Point2D point: pointsList) {
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.addElement(node_position_list, elementi);
			XMLUtil.setAttributeDoubleValue(node_position_i, "x", point.getX());
			XMLUtil.setAttributeDoubleValue(node_position_i, "y", point.getY());
			XMLUtil.setAttributeDoubleValue(node_position_i, "t", timeList.get(i));
			i++;
		}
		return true;
	}
	

}
