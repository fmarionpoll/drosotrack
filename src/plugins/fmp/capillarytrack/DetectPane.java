package plugins.fmp.capillarytrack;

import java.awt.Color;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.fmp.sequencevirtual.ImageTransformTools;
import plugins.fmp.sequencevirtual.SequencePlus;

public class DetectPane extends JPanel implements PropertyChangeListener, ChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	public JTabbedPane tabbedDetectionPane	= new JTabbedPane();
	public DetectTab_Limits detectLimitsTab = new DetectTab_Limits();
	public DetectTab_Colors detectColorsTab = new DetectTab_Colors();
	public DetectTab_Gulps detectGulpsTab 	= new DetectTab_Gulps();
	public DetectTab_File detectLoadSave 	= new DetectTab_File();
	
	public TransformOp colortransformop 	= TransformOp.NONE;
	public int colordistanceType 			= 0;
	public int colorthreshold 				= 20;
	public ArrayList <Color> colorarray 	= new ArrayList <Color>();
	//private boolean 	thresholdOverlayON	= false;
	public ThresholdType thresholdtype 		= ThresholdType.COLORARRAY; 
	// TODO
	public TransformOp simpletransformop 	= TransformOp.R2MINUS_GB;
	public int simplethreshold 				= 20;
	
	public JComboBox<TransformOp> transformsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.NORM_BRMINUSG, TransformOp.RGB,
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});

	ImageTransformTools tImg = null;
	Capillarytrack parent0 = null;

	
	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		this.parent0 = parent0;
		final JPanel panel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(panel));
		GridLayout capLayout = new GridLayout(4, 2);
		
		detectLimitsTab.init(capLayout, parent0);
		tabbedDetectionPane.addTab("Filters", null, detectLimitsTab, "thresholding a transformed image with different filters");
		detectLimitsTab.addPropertyChangeListener(this);
		
//		detectColorsTab.init(capLayout, parent0, this);
//		tabbedDetectionPane.addTab("Colors", null, detectColorsTab, "thresholding an image with different colors and a distance");
//		detectColorsTab.addPropertyChangeListener(this);
		
		detectGulpsTab.init(capLayout, parent0);	
		tabbedDetectionPane.addTab("Gulps", null, detectGulpsTab, "detect gulps");
		detectGulpsTab.addPropertyChangeListener(this);
		
		detectLoadSave.init (capLayout, parent0);
		tabbedDetectionPane.addTab("Load/Save", null, detectLoadSave, "load / save parameters");
		detectLoadSave.addPropertyChangeListener(this);
		
		tabbedDetectionPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabbedDetectionPane));
	
		detectLimitsTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		colortransformop = TransformOp.NONE;
		tabbedDetectionPane.setSelectedIndex(0);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("MEASURES_OPEN")) {
			if (parent0.kymographArrayList.size() > 0) 		
				firePropertyChange("MEASURES_OPEN", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DISPLAY_FILTERED1")) {
			if (parent0.kymographArrayList.size() > 0) {		
				firePropertyChange("KYMO_DISPLAYFILTERED", false, true);
			}
		}
		else if (arg0.getPropertyName().equals("KYMO_DETECT_TOP")) {
			Detect_Limits detect = new Detect_Limits();
			detect.detectCapillaryLevels(parent0); 
			firePropertyChange("MEASURETOP_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DETECT_GULP")) {
			Detect_Gulps detect = new Detect_Gulps();
			detect.detectGulps(parent0);
			firePropertyChange( "MEASURE_OPEN", false, true);
		}
	}
	
	public void setDetectionParameters(int ikymo) {
		SequencePlus seq = parent0.kymographArrayList.get(ikymo);
		detectLimitsTab.transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		detectLimitsTab.directionComboBox.setSelectedIndex(seq.direction);
		detectLimitsTab.setDetectLevelThreshold(seq.detectLevelThreshold);
		detectLimitsTab.detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		detectLimitsTab.detectAllLevelCheckBox.setSelected(seq.detectAllLevel);

		detectGulpsTab.detectGulpsThresholdTextField.setText(Integer.toString(seq.detectGulpsThreshold));
		detectGulpsTab.transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
		detectGulpsTab.detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}
	
	// ----------------------------
	
	@Override
	public void stateChanged(ChangeEvent arg0) {
		if (arg0.getSource() == tabbedDetectionPane)
			detectColorsTab.colorsUpdateThresholdOverlayParameters();
	}
	
	public void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {

		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		
		for (SequencePlus kSeq: parent0.kymographArrayList) {
			kSeq.beginUpdate();
			
			tImg.setSequence(kSeq);
			IcyBufferedImage img = kSeq.getImage(0, zChannelSource);
			IcyBufferedImage img2 = tImg.transformImage (img, transformop);
			img2 = tImg.transformImage(img2, TransformOp.RTOGB);
			
			if (kSeq.getSizeZ(0) < (zChannelDestination+1)) 
				kSeq.addImage(img2);
			else
				kSeq.setImage(0, zChannelDestination, img2);
			
			if (zChannelDestination == 1)
				kSeq.transformForLevels = transformop;
			else
				kSeq.transformForGulps = transformop;

			kSeq.dataChanged();
			kSeq.endUpdate();
			kSeq.getFirstViewer().getCanvas().setPositionZ(zChannelDestination);
		}
	}
}

