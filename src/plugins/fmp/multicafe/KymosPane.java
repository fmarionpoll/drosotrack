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
import plugins.fmp.multicafe.MultiCAFE.StatusPane;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ImageTransformTools;
import plugins.fmp.tools.ImageTransformTools.TransformOp;


public class KymosPane extends JPanel implements PropertyChangeListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	public JTabbedPane tabsPane 		= new JTabbedPane();
	public KymosTab_Options optionsTab 	= new KymosTab_Options();
	public KymosTab_File fileTab 		= new KymosTab_File();
	public KymosTab_Build buildTab 		= new KymosTab_Build();
	public KymosTab_Filter filterTab 	= new KymosTab_Filter();
	public KymosTab_DetectLimits limitsTab = new KymosTab_DetectLimits();
	public KymosTab_DetectGulps gulpsTab = new KymosTab_DetectGulps();
	ImageTransformTools tImg = null;
	private MultiCAFE parent0 = null;

	public void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		
		this.parent0 = parent0;
		final JPanel kymosPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		buildTab.init(capLayout, parent0);
		buildTab.addPropertyChangeListener(this);
		tabsPane.addTab("Build", null, buildTab, "Build kymographs from ROI lines placed over capillaries");
		
		filterTab.init(capLayout, parent0);
		filterTab.addPropertyChangeListener(this);
		tabsPane.addTab("Filter", null, filterTab, "Cross-correlate columns of pixels to reduce drift");

		optionsTab.init(capLayout, parent0);
		optionsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Display", null, optionsTab, "Display options of data & kymographs");
		
		limitsTab.init(capLayout, parent0);
		limitsTab.addPropertyChangeListener(this);
		tabsPane.addTab("Limits", null, limitsTab, "Find limits of the columns of liquid");
		
		gulpsTab.init(capLayout, parent0);	
		tabsPane.addTab("Gulps", null, gulpsTab, "detect gulps");
		gulpsTab.addPropertyChangeListener(this);
		
		fileTab.init(capLayout, parent0);
		fileTab.addPropertyChangeListener(this);
		tabsPane.addTab("Load/Save", null, fileTab, "Load/Save kymographs");
		
		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabsPane.addChangeListener(this);
		
		kymosPanel.add(GuiUtil.besidesPanel(tabsPane));
		limitsTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		tabsPane.setSelectedIndex(0);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("KYMOS_OPEN")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferFileNamesToComboBox();
			tabsPane.setSelectedIndex(2);
		}	
		else if (arg0.getPropertyName().equals("KYMOS_CREATE")) {
			optionsTab.viewKymosCheckBox.setSelected(true);
			optionsTab.transferRoisNamesToComboBox(parent0.vSequence.capillariesArrayList);
			tabsPane.setSelectedIndex(2);
		}
		else if (arg0.getPropertyName() .equals("KYMOS_DISPLAY_UPDATE")) {
			int ikymo = optionsTab.kymographNamesComboBox.getSelectedIndex();
			optionsTab.selectKymograph(ikymo);
		}
		else if (arg0.getPropertyName().equals("KYMOS_OK")) {
			fileTab.enableItems(true);
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
			optionsTab.displayUpdate();
		}
	}
	
	public boolean loadDefaultKymos() {
		String path = parent0.vSequence.getDirectory();
		final String cs = path+"\\results";
		int i = 0;
		boolean flag = fileTab.openFiles(cs);
		if (flag) {
			optionsTab.transferFileNamesToComboBox();
			optionsTab.viewKymosCheckBox.setSelected(true);
			i = 2;
		}
		tabsPane.setSelectedIndex(i);
		return flag;
	}

	@Override
	public void stateChanged(ChangeEvent event) {
		if (event.getSource() == tabsPane)
			tabbedCapillariesAndKymosSelected();
	}
		
	public void enableItems(StatusPane status) {
		boolean enable1 = !(status == StatusPane.DISABLED);
		buildTab.enableItems(enable1);
		fileTab.enableItems(enable1);
		boolean enable2 = (status == StatusPane.FULL);
		optionsTab.enableItems(enable2);
		limitsTab.enableItems(enable2);
		gulpsTab.enableItems(enable2);
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
