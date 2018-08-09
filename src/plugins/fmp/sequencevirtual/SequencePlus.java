package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import icy.roi.ROI2D;
import icy.roi.ROIEvent;
import icy.sequence.Sequence;
import icy.sequence.SequenceEvent.SequenceEventType;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.fmp.sequencevirtual.ImageTransform.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class SequencePlus extends Sequence {
	
	private ArrayList<Integer> levelTopArrayList 	= new ArrayList<Integer>();	// result of the detection of the top of the capillary level
	private ArrayList<Integer> levelBottomArrayList = new ArrayList<Integer>();	// result of the detection of the top of the capillary level
	public ArrayList<Integer> derivedValuesArrayList= new ArrayList<Integer>(); // (derivative) result of the detection of the capillary level
	private ArrayList<Integer> consumptionArrayList 	= new ArrayList<Integer>();	// (derivative) result of the detection of the capillary level
	public boolean hasChanged = false;
	
	public boolean bStatusChanged = false;
	public boolean detectTop = true;
	public boolean detectBottom = true;
	public boolean detectAllLevel = true;
	public boolean detectAllGulps = true;
	public	TransformOp	transformForLevels = TransformOp.GBmR;
	public 	int direction = 0;
	public 	int	detectLevelThreshold = 35;
	public	int detectGulpsThreshold = 90;
	public	TransformOp transformForGulps = TransformOp.XDIFFN;
	public enum ArrayListType {topLevel, bottomLevel, derivedValues, cumSum, topAndBottom}

	// -----------------------------------------------------
	public ArrayList<Integer> getArrayList (ArrayListType option) {
		
		ArrayList<Integer> datai = new ArrayList<Integer> ();
		switch (option) {
		case derivedValues:	// 1
			datai = derivedValuesArrayList;
			break;
		case cumSum:		// 2
			datai = consumptionArrayList;
			break;
		case bottomLevel: 	// 4
			datai = levelBottomArrayList;
			break;
		case topLevel:		// 0
		default:
			datai = levelTopArrayList;
			break;
		}
		return datai;
	}
	
	public ArrayList<Integer> getArrayListFromRois (ArrayListType option) {
		
		ArrayList<ROI2D> listRois = getROI2Ds();
		if (listRois == null)
			return null;
		ArrayList<Integer> datai = null;
		
		switch (option) {
		case derivedValues:	// 1
			datai = derivedValuesArrayList;
			//datai = new ArrayList<Integer>(Collections.nCopies(this.getWidth(), 0));
			break;
		case cumSum:		// 2
			datai = new ArrayList<Integer>(Collections.nCopies(this.getWidth(), 0));
			addRoitoDataArray("gulp", datai);
			break;
		case bottomLevel: 	// 4
			datai = copyRoitoDataArray("bottomlevel");
			break;
		case topLevel:		// 0
		default:
			datai = copyRoitoDataArray("toplevel");
			break;
		}
		return datai;
	}
	
	private ArrayList<Integer> copyRoitoDataArray(String filter) {
		
		ArrayList<ROI2D> listRois = getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) { 
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine)roi);
				return transfertRoiYValuesToDataArray((ROI2DPolyLine)roi);
			}
		}
		return null;
	}
	
	private void addRoitoDataArray(String filter, ArrayList<Integer> cumSumArray) {
		
		ArrayList<ROI2D> listRois = getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) 
				addRoitoCumulatedSumArray((ROI2DPolyLine) roi, cumSumArray);
		}
		return ;
	}
	
	private void copyRoitoDataArray(ROI2DPolyLine roiLine, ArrayList<Integer> intArray) {

		interpolateMissingPointsAlongXAxis (roiLine);
		intArray = transfertRoiYValuesToDataArray(roiLine);
	}
	
	private ArrayList<Integer> transfertRoiYValuesToDataArray(ROI2DPolyLine roiLine) {

		Polyline2D line = roiLine.getPolyline2D();
		ArrayList<Integer> intArray = new ArrayList<Integer> (line.npoints);
		for (int i=0; i< line.npoints; i++) 
			intArray.add((int) line.ypoints[i]);

		return intArray;
	}
	
	private boolean interpolateMissingPointsAlongXAxis (ROI2DPolyLine roiLine) {
		// interpolate points so that each x step has a value	
		// assume that points are ordered along x
	
		Polyline2D line = roiLine.getPolyline2D();
		int roiLine_npoints = line.npoints;
		// exit if the length of the segment is the same
		int roiLine_nintervals =(int) line.xpoints[roiLine_npoints-1] - (int) line.xpoints[0] +1;  
		
		if (roiLine_npoints == roiLine_nintervals)
			return true;
		else if (roiLine_npoints <= roiLine_nintervals)
			return false;
		
		List<Point2D> pts = new ArrayList <Point2D>(roiLine_npoints);
		double ylast = line.ypoints[roiLine_npoints-1];
		for (int i=1; i< roiLine_npoints; i++) {
			
			int xfirst = (int) line.xpoints[i-1];
			int xlast = (int) line.xpoints[i];
			double yfirst = line.ypoints[i-1];
			ylast = line.ypoints[i];
			for (int j = xfirst; j< xlast; j++) {
				
				int val = (int) (yfirst + (ylast-yfirst)*(j-xfirst)/(xlast-xfirst));
				Point2D pt = new Point2D.Double(j, val);
				pts.add(pt);
			}
		}
		Point2D pt = new Point2D.Double(line.xpoints[roiLine_npoints-1], ylast);
		pts.add(pt);
		
		roiLine.setPoints(pts);
		return true;
	}
	
	private void addRoitoCumulatedSumArray(ROI2DPolyLine roi, ArrayList<Integer> consumptionArrayList) {
		
		interpolateMissingPointsAlongXAxis (roi);
		ArrayList<Integer> intArray = transfertRoiYValuesToDataArray(roi);
		Polyline2D line = roi.getPolyline2D();
		int jstart = (int) line.xpoints[0];

		int previousY = intArray.get(0);
		for (int i=0; i< intArray.size(); i++) {
			int val = intArray.get(i);
			int deltaY = val - previousY;
			previousY = val;
			for (int j = jstart+i; j< consumptionArrayList.size(); j++) {
				consumptionArrayList.set(j, consumptionArrayList.get(j) +deltaY);
			}
		}
	}
	
	public void transferRoistoData () {

		// load topLevelArrayList = 1 polyline...	
		ArrayList<ROI2D> listRois = getROI2Ds();
		if (listRois == null)
			return;

		// prepare consumption array list (erase it and fill with null values)
		consumptionArrayList.clear();
		int nelements = getWidth();
		if (levelTopArrayList.size() > 0)
			nelements = levelTopArrayList.size();
		if (consumptionArrayList.size() != nelements) {
			consumptionArrayList= new ArrayList<Integer>(Collections.nCopies(nelements, 0));
		}

		// ----------------------- read topLevel polyline & unselect any rois
		for (ROI2D roi: listRois) {
			if (roi.isSelected())
				roi.setSelected(false);

			String name = roi.getName();
			if (name.contains("toplevel")) 
				copyRoitoDataArray((ROI2DPolyLine) roi, levelTopArrayList);

			else if (name.contains("bottomlevel"))
				copyRoitoDataArray((ROI2DPolyLine) roi, levelBottomArrayList);

			else if (name.contains("gulp")) {
				addRoitoCumulatedSumArray((ROI2DPolyLine)roi, consumptionArrayList);
			}
		}
		Collections.sort(listRois, new Tools.ROI2DNameComparator()); 
	}
	
	public void validateRois() {

		ArrayList<ROI2D> listRois = getROI2Ds();
		
		for (ROI2D roi: listRois) {

			if (roi.getName().contains("level") || roi.getName().contains("gulp")) 
				continue;

			// if gulp not found - add an index to it
			if (roi instanceof ROI2DPolyLine) {
				ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
				Polyline2D line = roiLine.getPolyline2D();
				roi.setName("gulp"+String.format("%07d", (int) line.xpoints[0]));
				roi.setColor(Color.red);						// set color to red
			}
		}
		Collections.sort(listRois, new Tools.ROI2DNameComparator());
	}
	
    @Override
    public void roiChanged(ROIEvent event)
    {
    	hasChanged = true;
        super.roiChanged(event.getSource(), SequenceEventType.CHANGED);
    }

	public boolean loadXMLResults (String directory, int start, int end) {
	
		// check if directory is present. If not, create it
		String resultsDirectory = directory+"\\results\\";
		Path resultsPath = Paths.get(resultsDirectory);
		if (Files.notExists(resultsPath)) 
				return false; 
		
		setFilename(resultsDirectory+getName()+start+"_to_"+end+".xml");
		Path filenamePath = Paths.get(filename);
		if (Files.notExists(filenamePath)) 
			return false; 
		
		removeAllROI();
		boolean flag = loadXMLData();
		if (flag) {
			ArrayList<ROI2D> listRois = getROI2Ds();
			for (ROI2D roi: listRois) {
			    addROI(roi);
			}
		}
		
		// save specific parameters to XML before
		Node myNode = getNode(this.getName()+"_parameters");
		detectTop = XMLUtil.getElementBooleanValue(myNode, "detectTop", true);
		detectBottom = XMLUtil.getElementBooleanValue(myNode, "detectBottom", false);
		detectAllLevel = XMLUtil.getElementBooleanValue(myNode, "detectAllLevel", true);
		detectAllGulps = XMLUtil.getElementBooleanValue(myNode, "detectAllGulps", true); 
		bStatusChanged = XMLUtil.getElementBooleanValue(myNode, "bStatusChanged", false);

		//transformForLevels 
		int dummy = XMLUtil.getElementIntValue(myNode, "transformForLevels", 0);
		transformForLevels = TransformOp.values()[dummy];
		direction = XMLUtil.getElementIntValue(myNode, "direction", 0);
		detectLevelThreshold = XMLUtil.getElementIntValue(myNode, "detectLevelThreshold", 35);
		detectGulpsThreshold = XMLUtil.getElementIntValue(myNode, "detectGulpsThreshold", 75);
		int dummy2 = XMLUtil.getElementIntValue(myNode, "transformForGulps", 3);
		transformForGulps = TransformOp.values()[dummy2];
				
		return flag;
	}

	public boolean saveXMLResults(String directory, int start, int end) {

		// check if directory is present. If not, create it
		String resultsDirectory = directory+"\\results\\";
		Path resultsPath = Paths.get(resultsDirectory);
		if (Files.notExists(resultsPath)) {
			try {
				resultsPath = Files.createDirectories(resultsPath);
			} catch (IOException e) {
				// Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		
		// save specific parameters to XML before
		Node myNode = getNode(this.getName()+"_parameters");
		XMLUtil.setElementBooleanValue(myNode, "detectTop", detectTop);
		XMLUtil.setElementBooleanValue(myNode, "detectBottom", detectBottom);
		XMLUtil.setElementBooleanValue(myNode, "detectAllLevel", detectAllLevel);
		XMLUtil.setElementBooleanValue(myNode, "detectAllGulps", detectAllGulps); 
		XMLUtil.setElementBooleanValue(myNode, "bStatusChanged", bStatusChanged);

		int dummy1 = transformForLevels.ordinal(); 
		XMLUtil.setElementIntValue(myNode, "transformForLevels", dummy1);
		XMLUtil.setElementIntValue(myNode, "direction", direction);
		XMLUtil.setElementIntValue(myNode, "detectLevelThreshold", detectLevelThreshold);
		XMLUtil.setElementIntValue(myNode, "detectGulpsThreshold", detectGulpsThreshold);
		int dummy2 = transformForGulps.ordinal();
		XMLUtil.setElementIntValue(myNode, "transformForGulps", dummy2);
		
		// save file
		setFilename(resultsDirectory+getName()+start+"_to_"+end+".xml");
		return saveXMLData();
	}

}
