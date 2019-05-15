package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;


public class KymosPane extends JPanel  implements PropertyChangeListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	public JTabbedPane tabbedKymosPane = new JTabbedPane();
	public KymosTab_Options optionsKymoTab = new KymosTab_Options();
	public KymosTab_File fileKymoTab = new KymosTab_File();
	public KymosTab_Build buildKymosTab = new KymosTab_Build();

	private Capillarytrack parent0 = null;

	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		
		this.parent0 = parent0;
		
		final JPanel kymosPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		buildKymosTab.init(capLayout, parent0);
		tabbedKymosPane.addTab("Build", null, buildKymosTab, "Build kymographs from ROI lines placed over capillaries");
		
		optionsKymoTab.init(capLayout, parent0);
		optionsKymoTab.addPropertyChangeListener(parent0);
		tabbedKymosPane.addTab("Display", null, optionsKymoTab, "Display options of data & kymographs");
		
		fileKymoTab.init(capLayout, parent0);
		fileKymoTab.addPropertyChangeListener(this);
		tabbedKymosPane.addTab("Load/Save", null, fileKymoTab, "Load/Save kymographs");
		
		tabbedKymosPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		buildKymosTab.addPropertyChangeListener(parent0);
		kymosPanel.add(GuiUtil.besidesPanel(tabbedKymosPane));
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("KYMOS_OPEN")) {
			optionsKymoTab.viewKymosCheckBox.setSelected(true);
			optionsKymoTab.transferFileNamesToComboBox();
		}	
		else if (arg0.getPropertyName().equals("KYMOS_CREATE")) {
			optionsKymoTab.viewKymosCheckBox.setSelected(true);
			optionsKymoTab.transferRoisNamesToComboBox(parent0.vSequence.capillariesArrayList);
		 }
		
	}
	
	public void tabbedCapillariesAndKymosSelected() {
		if (parent0.vSequence == null)
			return;
		int iselected = tabbedKymosPane.getSelectedIndex();
		if (iselected == 0) {
			Viewer v = parent0.vSequence.getFirstViewer();
			v.toFront();
		} else if (iselected == 1) {
			optionsKymoTab.displayUpdate();
		}
	}
}
