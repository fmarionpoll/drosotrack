package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ImageTransformTools;
import plugins.fmp.tools.ImageTransformTools.TransformOp;


public class KymosPane extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	public JTabbedPane 			tabsPane 	= new JTabbedPane();
	public KymosTab_File 		fileTab 	= new KymosTab_File();
	public KymosTab_DetectLimits limitsTab 	= new KymosTab_DetectLimits();
	public KymosTab_DetectGulps gulpsTab 	= new KymosTab_DetectGulps();
	public KymosTab_Graphs 		graphsTab 	= new KymosTab_Graphs();
	
	ImageTransformTools tImg = null;
	private Multicafe parent0 = null;

	public void init (JPanel mainPanel, String string, Multicafe parent0) {
		
		this.parent0 = parent0;
		final JPanel kymosPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(3, 1);
		
		limitsTab.init(capLayout, parent0);
		limitsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Liquid", null, limitsTab, "Find limits of the columns of liquid");
		
		gulpsTab.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, gulpsTab, "detect gulps");
		gulpsTab.addPropertyChangeListener(this);
		
		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save kymographs");
		
		graphsTab.init(capLayout, parent0);
		graphsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Graphs", null, graphsTab, "Display results as a graph");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabsPane.addChangeListener(this);
		
		kymosPanel.add(GuiUtil.besidesPanel(tabsPane));
		limitsTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		tabsPane.setSelectedIndex(0);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("KYMOS_OK")) {
			tabbedCapillariesAndKymosSelected();
			firePropertyChange( "KYMOS_OK", false, true);
		}
		else if (arg0.getPropertyName().equals("KYMOS_SAVE")) {
			tabsPane.setSelectedIndex(2);
		}
		else if (arg0.getPropertyName().equals("MEASURES_OPEN")) {
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

	public void tabbedCapillariesAndKymosSelected() {
		if (parent0.vSequence == null)
			return;
		int iselected = tabsPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = parent0.vSequence.getFirstViewer();
			v.toFront();
		} else if (iselected == 1) {
			parent0.capillariesPane.optionsTab.displayUpdate();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
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
