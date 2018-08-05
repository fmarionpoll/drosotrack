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
	
	public ArrayList<Integer> levelTopArrayList 	= new ArrayList<Integer>();	// result of the detection of the top of the capillary level
	public ArrayList<Integer> levelBottomArrayList 	= new ArrayList<Integer>();	// result of the detection of the top of the capillary level
	public ArrayList<Integer> derivedArrayList 		= new ArrayList<Integer>();	// (derivative) result of the detection of the capillary level
	public ArrayList<Integer> derivedValuesArrayList= new ArrayList<Integer>(); // (derivative) result of the detection of the capillary level
	public ArrayList<Integer> consumptionArrayList 	= new ArrayList<Integer>();	// (derivative) result of the detection of the capillary level
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

	// -----------------------------------------------------
	public ArrayList<Integer> getArrayList (int ioption) {
		
		ArrayList<Integer> datai = new ArrayList<Integer> ();
		switch (ioption) {
		case 1:
			datai = derivedValuesArrayList;
			break;
		case 2:
			datai = consumptionArrayList;
			break;
		case 3:
			datai = derivedArrayList;
			break;
		case 4: 
			datai = levelBottomArrayList;
			break;
		case 0:
		default:
			datai = levelTopArrayList;
			break;
		}
		return datai;
	}
	
	private void copyRoitoData(ROI2DPolyLine roiLine, ArrayList<Integer> intArray) {
		// interpolate points so that each step has a value	
		intArray.clear();
		Polyline2D line = roiLine.getPolyline2D();
		int npoints = line.npoints;
		List<Point2D> pts = new ArrayList <Point2D>();
		
		double ylast = line.ypoints[npoints-1];
		for (int i=1; i< npoints; i++) {
			int xfirst = (int) line.xpoints[i-1];
			int xlast = (int) line.xpoints[i];
			double yfirst = line.ypoints[i-1];
			ylast = line.ypoints[i];
			for (int j = xfirst; j< xlast; j++) {
				int val = (int) (yfirst + (ylast-yfirst)*(j-xfirst)/(xlast-xfirst));
				intArray.add( val);
				Point2D pt = new Point2D.Double(j, val);
				pts.add(pt);
			}
		}
		intArray.add( (int) ylast);
		Point2D pt = new Point2D.Double(line.xpoints[npoints-1], ylast);
		pts.add(pt);
		
		roiLine.setPoints(pts);
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

		boolean bInitGulps = false;

		// ----------------------- read topLevel polyline & unselect any rois
		for (ROI2D roi: listRois) {
			if (roi.isSelected())
				roi.setSelected(false);

			String name = roi.getName();
			if (name.contains("toplevel")) 
				copyRoitoData((ROI2DPolyLine) roi, levelTopArrayList);

			else if (name.contains("bottomlevel"))
				copyRoitoData((ROI2DPolyLine) roi, levelBottomArrayList);

			else if (name.contains("gulp")) {
				if (!bInitGulps) {
					bInitGulps = true;
					consumptionArrayList= new ArrayList<Integer>(Collections.nCopies(nelements, 0));
				}
				ArrayList<Integer> intArray = new ArrayList<Integer>();
				copyRoitoData((ROI2DPolyLine)roi, intArray);
				Polyline2D line = ((ROI2DPolyLine)roi).getPolyline2D();
				int jstart = (int) line.xpoints[0];

				int previousY = intArray.get(0);
				for (int i=0; i< intArray.size(); i++) {
					int val = intArray.get(i);
					int deltaY = val - previousY;
					previousY = val;
					for (int j = jstart+i; j< nelements; j++) {
						consumptionArrayList.set(j, consumptionArrayList.get(j) +deltaY);
					}
				}
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
