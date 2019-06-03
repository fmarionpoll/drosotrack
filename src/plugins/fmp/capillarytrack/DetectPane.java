package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import plugins.fmp.tools.StatusPane;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ImageTransformTools;
import plugins.fmp.tools.ImageTransformTools.TransformOp;

public class DetectPane extends JPanel implements PropertyChangeListener, ChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	public JTabbedPane tabsPane				= new JTabbedPane();
	public DetectTab_Limits limitsTab 		= new DetectTab_Limits();
	public DetectTab_Colors colorsTab 		= new DetectTab_Colors();
	public DetectTab_Gulps gulpsTab 		= new DetectTab_Gulps();
	public DetectTab_File fileTab 			= new DetectTab_File();
	
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
//		GridLayout capLayout = new GridLayout(4, 2);
		GridLayout capLayout = new GridLayout(3, 2);
		
		limitsTab.init(capLayout, parent0);
		tabsPane.addTab("Upper/lower(1)", null, limitsTab, "thresholding a transformed image with different filters");
		limitsTab.addPropertyChangeListener(this);
		
//		detectColorsTab.init(capLayout, parent0, this);
//		tabbedDetectionPane.addTab("Colors", null, detectColorsTab, "thresholding an image with different colors and a distance");
//		detectColorsTab.addPropertyChangeListener(this);
		
		gulpsTab.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, gulpsTab, "detect gulps");
		gulpsTab.addPropertyChangeListener(this);
		
		fileTab.init (capLayout, parent0);
		tabsPane.addTab("Load/Save", null, fileTab, "load / save parameters");
		fileTab.addPropertyChangeListener(this);
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabsPane));
	
		limitsTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		tabsPane.setSelectedIndex(0);
	}
	
	public void enableItems(StatusPane status) {
		boolean enable1 = !(status == StatusPane.DISABLED);
		limitsTab.enableItems(enable1);
		fileTab.enableItems(enable1);
		boolean enable2 = (status == StatusPane.FULL);
		gulpsTab.enableItems(enable2);
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
			firePropertyChange("MEASURETOP_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DETECT_GULP")) {
			firePropertyChange( "MEASUREGULPS_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("MEASURES_SAVE")) {
			tabsPane.setSelectedIndex(0);
		}
	}
	
	public void setDetectionParameters(int ikymo) {
		SequencePlus seq = parent0.kymographArrayList.get(ikymo);
		limitsTab.transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		limitsTab.directionComboBox.setSelectedIndex(seq.direction);
		limitsTab.setDetectLevelThreshold(seq.detectLevelThreshold);
		limitsTab.detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		limitsTab.detectAllLevelCheckBox.setSelected(seq.detectAllLevel);

		gulpsTab.detectGulpsThresholdTextField.setText(Integer.toString(seq.detectGulpsThreshold));
		gulpsTab.transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
		gulpsTab.detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}
	
	
	// ----------------------------
	
	@Override
	public void stateChanged(ChangeEvent arg0) {
		if (arg0.getSource() == tabsPane)
			colorsTab.colorsUpdateThresholdOverlayParameters();
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

