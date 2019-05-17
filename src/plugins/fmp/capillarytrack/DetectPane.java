package plugins.fmp.capillarytrack;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.fmp.capillarytrack.Capillarytrack.StatusAnalysis;
import plugins.fmp.sequencevirtual.ImageTransformTools;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.Tools;

public class DetectPane extends JPanel implements PropertyChangeListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3457738144388946607L;
	
	public JTabbedPane tabbedDetectionPane	= new JTabbedPane();
	public DetectTab_Limits detectTopBottomTab = new DetectTab_Limits();
	public DetectTab_Colors detectColorsTab = new DetectTab_Colors();
	public DetectTab_Gulps detectGulpsTab 	= new DetectTab_Gulps();
	public DetectTab_File detectLoadSave 	= new DetectTab_File();
	
	public JRadioButton rbFilterbyColor		= new JRadioButton("filter by color array");
	public JRadioButton rbFilterbyFunction	= new JRadioButton("filter by function");
	// colors
	public TransformOp colortransformop 	= TransformOp.NONE;
	public int 		colordistanceType 		= 0;
	public int 		colorthreshold 			= 20;
	public ArrayList <Color> colorarray 	= new ArrayList <Color>();
	//private boolean 	thresholdOverlayON	= false;
	public ThresholdType thresholdtype 		= ThresholdType.COLORARRAY; 
	// TODO
	public TransformOp simpletransformop 	= TransformOp.R2MINUS_GB;
	public int 		simplethreshold 		= 20;
	
	ImageTransformTools tImg = null;
	Capillarytrack parent0 = null;

	
	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		this.parent0 = parent0;
		final JPanel panel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(panel));
		panel.add( GuiUtil.besidesPanel(rbFilterbyFunction, rbFilterbyColor));
		ButtonGroup bgchoice = new ButtonGroup();
		bgchoice.add(rbFilterbyColor);
		bgchoice.add(rbFilterbyFunction);
		GridLayout capLayout = new GridLayout(4, 2);
		
		detectTopBottomTab.init(capLayout, parent0);
		tabbedDetectionPane.addTab("Filters", null, detectTopBottomTab, "thresholding a transformed image with different filters");
		detectTopBottomTab.addPropertyChangeListener(this);
		
		detectColorsTab.init(capLayout);
		tabbedDetectionPane.addTab("Colors", null, detectColorsTab, "thresholding an image with different colors and a distance");
		detectColorsTab.addPropertyChangeListener(this);
		
		detectGulpsTab.init(capLayout);	
		tabbedDetectionPane.addTab("Gulps", null, detectGulpsTab, "detect gulps");
		detectGulpsTab.addPropertyChangeListener(this);
		
		detectLoadSave.init (capLayout, parent0);
		tabbedDetectionPane.addTab("Load/Save", null, detectLoadSave, "load / save parameters");
		detectLoadSave.addPropertyChangeListener(this);
		
		tabbedDetectionPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabbedDetectionPane));
	
		detectTopBottomTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		colortransformop = TransformOp.NONE;
		tabbedDetectionPane.setSelectedIndex(0);
		rbFilterbyFunction.setSelected(true);
		
		rbFilterbyColor.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			if (rbFilterbyColor.isSelected())
				selectTab(1);
		} } );
		
		rbFilterbyFunction.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			if (rbFilterbyFunction.isSelected())
				selectTab(0);
		} } );
	}
	
	private void selectTab(int index) {
		tabbedDetectionPane.setSelectedIndex(index);
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("MEASURES_OPEN")) {
			if (parent0.kymographArrayList.size() > 0) {		
				firePropertyChange("MEASURES_OPEN", false, true);
			}
		}
		else if (arg0.getPropertyName().equals("KYMO_DISPLAYFILTERED1")) {
			kymosDisplayFiltered(1);
			firePropertyChange("KYMO_DISPLAYFILTERED", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMO_DETECT_TOP")) {
			kymosDisplayFiltered(1);
			DetectCapillaryLevels detect = new DetectCapillaryLevels();
			detect.kymosDetectCapillaryLevels(parent0); 
			firePropertyChange("MEASURETOP_OK", false, true);
		}
	}
	
	public void setDetectionParameters(int ikymo) {
		SequencePlus seq = parent0.kymographArrayList.get(ikymo);
		detectTopBottomTab.transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		detectTopBottomTab.directionComboBox.setSelectedIndex(seq.direction);
		detectTopBottomTab.setDetectLevelThreshold(seq.detectLevelThreshold);
		detectTopBottomTab.detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		detectTopBottomTab.detectAllLevelCheckBox.setSelected(seq.detectAllLevel);

		detectGulpsTab.detectGulpsThresholdTextField.setText(Integer.toString(seq.detectGulpsThreshold));
		detectGulpsTab.transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
		detectGulpsTab.detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}
	
	// ----------------------------
	public void kymosDisplayFiltered(int zChannel) {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new Tools.SequenceNameComparator()); 
		TransformOp transform;
		if (zChannel == 1) 
			transform = (TransformOp) detectTopBottomTab.transformForLevelsComboBox.getSelectedItem();
		else 
			transform = (TransformOp) detectGulpsTab.transformForGulpsComboBox.getSelectedItem();
		kymosBuildFiltered(0, zChannel, transform, detectTopBottomTab.getSpanDiffTop());
	}
	
	public void kymosBuildFiltered(int zChannelSource, int zChannelDestination, TransformOp transformop, int spanDiff) {

		if (tImg == null) 
			tImg = new ImageTransformTools();
		tImg.setSpanDiff(spanDiff);
		
		for (int i=0; i < parent0.kymographArrayList.size(); i++) {

			SequencePlus kSeq = parent0.kymographArrayList.get(i); 
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

