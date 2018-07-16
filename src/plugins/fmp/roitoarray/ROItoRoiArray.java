package plugins.fmp.roitoarray;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.preferences.XMLPreferences;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;
import plugins.fmp.sequencevirtual.ImageTransform;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ThresholdOverlay;
import plugins.fmp.sequencevirtual.Tools;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DEllipse;

public class ROItoRoiArray extends EzPlug implements ViewerListener {

	// -------------------------------------- interface 
	EzButton		openFileButton;
	EzVarSequence   sequence = new EzVarSequence("Select data from");
	EzVarText		rootnameComboBox;
	EzVarInteger 	nrows;
	EzVarInteger 	ncolumns;
	EzVarInteger 	columnSize;
	EzVarInteger 	columnSpan;
	EzVarInteger 	rowWidth; 
	EzVarInteger 	rowInterval; 
	EzVarText 		resultComboBox;
	EzButton		findLinesButton;
	EzVarBoolean 	overlayCheckBox;
	EzVarText		filterComboBox;
	EzVarInteger 	threshold;
	
	private ImageTransform imgTransf = new ImageTransform();
	private ThresholdOverlay thresholdOverlay = null;
	private SequenceVirtual vSequence = null;
	private int numberOfImageForBuffer 		= 100;
	private String lastUsedPath		= null;
	private String rootname;
	private String [] transforms;
	
	// ----------------------------------
	
	@Override
	protected void initialize() {

		// 1) init variables
		resultComboBox 	= new EzVarText("Split polygon as ", new String[] {"vertical lines", "polygons", "circles"}, 1, false);
		rootnameComboBox= new EzVarText("Root name", new String[] {"gridA", "gridB", "gridC"}, 0, true);
		ncolumns		= new EzVarInteger("N columns ", 5, 1, 1000, 1);
		columnSize		= new EzVarInteger("column width ", 10, 0, 1000, 1);
		columnSpan		= new EzVarInteger("space btw. col. ", 0, 0, 1000, 1);
		nrows 			= new EzVarInteger("N rows ", 10, 1, 1000, 1); 
		rowWidth		= new EzVarInteger("row height ", 10, 0, 1000, 1);
		rowInterval 	= new EzVarInteger("space btw. row ", 0, 0, 1000, 1);
		
		findLinesButton = new EzButton("test", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { doTest(); }	});
		
		openFileButton = new EzButton("Open file or sequence",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { openFile(); } });
		
		overlayCheckBox = new EzVarBoolean("build from overlay", false);
		overlayCheckBox.addVarChangeListener(new EzVarListener<Boolean>() {
             @Override
             public void variableChanged(EzVar<Boolean> source, Boolean newValue) {displayOverlay(newValue);}
         });
		transforms =  imgTransf.getAvailableTransforms();
		filterComboBox = new EzVarText("Filter as ", transforms, 6, false);
		filterComboBox.addVarChangeListener(new EzVarListener<String>() {
			@Override
			public void variableChanged(EzVar<String> source, String newString) {updateOverlay();}
		});
		threshold = new EzVarInteger("threshold ", 70, 1, 255, 10);
		threshold.addVarChangeListener(new EzVarListener<Integer>() {
            @Override
            public void variableChanged(EzVar<Integer> source, Integer newValue) { updateThreshold(newValue); }
        });

		// 2) add variables to the interface
		addEzComponent(sequence);
		addEzComponent(openFileButton);
		addEzComponent(rootnameComboBox);
		addEzComponent(resultComboBox);
		addEzComponent(ncolumns);
		addEzComponent(columnSize);
		addEzComponent(columnSpan);
		addEzComponent(nrows);
		addEzComponent(rowWidth);
		addEzComponent(rowInterval);
		addEzComponent(overlayCheckBox);
		addEzComponent(filterComboBox);
		addEzComponent(threshold);
		addEzComponent(findLinesButton);
	}
	
	// ----------------------------------
	private void doTest() {
		
	}
	
	private void displayOverlay (Boolean newValue) {
		if (vSequence == null)
			return;

		if (newValue) {
			
			if (thresholdOverlay == null) {
				thresholdOverlay = new ThresholdOverlay();
				vSequence.setThresholdOverlay(thresholdOverlay);
			}
			vSequence.threshold = threshold.getValue();
			vSequence.addOverlay(thresholdOverlay);
			updateOverlay();
		}
		else  {
			if (vSequence == null)
				return;
			if (thresholdOverlay != null) 
				vSequence.removeOverlay(thresholdOverlay);
			vSequence.setThresholdOverlay(null);
			thresholdOverlay = null;
		}
	}
	
	private void updateThreshold (int newValue) {
		if (vSequence == null)
			return;
		
		vSequence.threshold = threshold.getValue();
		updateOverlay();
	}
	
	private int getFilterComboSelectedItem() {
		String selected = filterComboBox.getValue();
		int transform = -1;
		for (int i=0; i < transforms.length; i++) {
			if (transforms[i] == selected) {
				transform = i;
				break;
			}
		}
		return transform;
	}
	
	private void updateOverlay () {
		if (vSequence == null)
			return;
		if (thresholdOverlay == null) {
			thresholdOverlay = new ThresholdOverlay();
			vSequence.setThresholdOverlay(thresholdOverlay);
		}
		int transform = getFilterComboSelectedItem();
		if (transform < 0)
			return;
		thresholdOverlay.setThresholdOverlayParameters( vSequence,
				overlayCheckBox.getValue(), 
				vSequence.threshold, 
				transform);
			
		if (thresholdOverlay != null) {
			thresholdOverlay.painterChanged();
		}
	}
	
	private void openFile() {
		String path = null;
		if (vSequence != null) {
			vSequence.close();
		}
		
		vSequence = new SequenceVirtual();
		XMLPreferences guiPrefs = this.getPreferences("gui");
		lastUsedPath = guiPrefs.get("lastUsedPath", "");
		
		path = vSequence.loadInputVirtualStack(lastUsedPath);
		if (path != null) {
			guiPrefs.put("lastUsedPath", path);
			lastUsedPath = path;
			initInputSeq();
		}
	}
	
	// ----------------------------------
	private void addEllipseROI (List<Point2D> points, String baseName, int i, int j) {
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName(rootname+baseName+ String.format("_r%02d", j) + String.format("_c%02d", i));
		roiP.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiP);
	}
	
	private void addPolygonROI (List<Point2D> points, String baseName, int i, int j) {
		ROI2DPolygon roiP = new ROI2DPolygon (points);
		roiP.setName(rootname+baseName+ String.format("_R%02d", j) + String.format("_C%02d", i));
		roiP.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiP);
	}
	
	private void addLineROI (List<Point2D> points, String baseName, int i, int j) {
		ROI2DLine roiL1 = new ROI2DLine (points.get(0), points.get(1));
		roiL1.setName(rootname+baseName+ String.format("%02d", i/2)+"L");
		roiL1.setReadOnly(false);
		roiL1.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiL1, true);
		
		ROI2DLine roiL2 = new ROI2DLine (points.get(2), points.get(3));
		roiL2.setName(rootname+baseName+ String.format("%02d", i/2)+"R");
		roiL2.setReadOnly(false);
		roiL2.setColor(Color.YELLOW);
		sequence.getValue(true).addROI(roiL2, true);
	}
	
	private void createROISFromPolygon(int ioption) {
		
		ROI2D roi = sequence.getValue(true).getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI 2D POLYGON");
			return;
		}

		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		sequence.getValue(true).removeAllROI();
		sequence.getValue(true).addROI(roi, true);
		
		double colSpan = columnSpan.getValue();
		double colSize = columnSize.getValue();
		double nbcols = ncolumns.getValue(); 
		double colsSum = nbcols * (colSize + colSpan) + colSpan;
		
		double rowSpan = rowInterval.getValue();
		double rowSize = rowWidth.getValue();
		double nbrows = nrows.getValue();
		double rowsSum = nbrows * (rowSize + rowSpan) + rowSpan;

		String baseName = null;
		rootname = rootnameComboBox.getValue()+"_";

		for (int column=0; column< nbcols; column++) {
			
			double ratioX0 = ((colSize + colSpan)*column + colSpan) /colsSum;
			
			double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * ratioX0;
			double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * ratioX0;
			Point2D.Double ipoint0 = new Point2D.Double (x, y);
			
			x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * ratioX0 ;
			y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * ratioX0 ;
			Point2D.Double ipoint1 = new Point2D.Double (x, y);

			double ratioX1 = ((colSize + colSpan)*(column+1)) / colsSum;

			x = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * ratioX1;
			y = roiPolygon.ypoints[1]+ (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * ratioX1;
			Point2D.Double ipoint2 = new Point2D.Double (x, y);
			
			x = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * ratioX1;
			y = roiPolygon.ypoints[0]+ (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * ratioX1;
			Point2D.Double ipoint3 = new Point2D.Double (x, y);
			
			for (int row=0; row < nrows.getValue(); row++) {
				
				double ratioY0 = ( (rowSize + rowSpan)*row + rowSpan)/rowsSum;

				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY0;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY0;
				Point2D.Double point0 = new Point2D.Double (x, y);
				
				x = ipoint3.x + (ipoint2.x - ipoint3.x) * ratioY0;
				y = ipoint3.y + (ipoint2.y - ipoint3.y) * ratioY0;
				Point2D.Double point3 = new Point2D.Double (x, y);
				
				double ratioY1 = ( (rowSize + rowSpan)*(row+1)) / rowsSum;
				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY1;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY1;
				Point2D.Double point1 = new Point2D.Double (x, y);
				
				x = ipoint3.x + (ipoint2.x - ipoint3.x) * ratioY1;
				y = ipoint3.y + (ipoint2.y - ipoint3.y) * ratioY1;
				Point2D.Double point2 = new Point2D.Double (x, y);
				
				List<Point2D> points = new ArrayList<>();
				points.add(point0);
				points.add(point1);
				points.add(point2);
				points.add(point3);
				
				switch (ioption)
				{
				case 0:
					if (baseName == null)
						baseName = "line ";
					addLineROI (points, baseName, column, row);
					break;
				case 1:
					if (baseName == null)
						baseName = "area ";
					addPolygonROI (points, baseName, column, row);
					break;
				case 2:
				default:
					if (baseName == null)
						baseName = "circle ";
					addEllipseROI (points, baseName, column, row);
					break;
				}
			}
		}

		ArrayList<ROI2D> list = sequence.getValue(true).getROI2Ds();
		Collections.sort(list, new Tools.ROI2DNameComparator());
	}
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void execute() {
		String choice = resultComboBox.getValue();
		if (choice == "vertical lines") {
			createROISFromPolygon(0);
		}
		else if (choice == "polygons") {
			createROISFromPolygon(1);
		}
		else if (choice == "circles") {
			createROISFromPolygon(2);
		}
//		else
			
	}

	private void initInputSeq () {

		// transfer 1 image to the viewer
		addSequence(vSequence);
		Viewer v = vSequence.getFirstViewer();
		v.addListener(ROItoRoiArray.this);

		vSequence.removeAllImages();
		startstopBufferingThread();		
	}
	
	private void startstopBufferingThread() {

		if (vSequence == null)
			return;

		vSequence.vImageBufferThread_STOP();
		vSequence.istep = 1;
		vSequence.vImageBufferThread_START(numberOfImageForBuffer);
	}
	
	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			vSequence.currentFrame = event.getSource().getPositionT() ; 
	}
	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
		vSequence = null;
	}


}

