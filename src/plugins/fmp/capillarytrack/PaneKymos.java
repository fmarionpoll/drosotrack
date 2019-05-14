package plugins.fmp.capillarytrack;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class PaneKymos extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7339633966002954720L;
	
	public JTabbedPane tabbedKymosPane = new JTabbedPane();
	public PaneKymos_Options optionsKymoTab = new PaneKymos_Options();
	public PaneKymos_LoadSave fileKymoTab = new PaneKymos_LoadSave();
	public PaneKymos_Build buildKymosTab = new PaneKymos_Build();

	private Capillarytrack parent0 = null;

	public void init (JPanel mainPanel, String string, Capillarytrack parent) {
		
		this.parent0 = parent0;
		
		final JPanel kymosPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(kymosPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		buildKymosTab.init(capLayout, parent0);
		tabbedKymosPane.addTab("Build", null, buildKymosTab, "Build kymographs from ROI lines placed over capillaries");
		
		optionsKymoTab.init(capLayout);
		optionsKymoTab.addPropertyChangeListener(parent);
		tabbedKymosPane.addTab("Display", null, optionsKymoTab, "Display options of data & kymographs");
		
		fileKymoTab.init(capLayout);
		fileKymoTab.addPropertyChangeListener(parent);
		tabbedKymosPane.addTab("Load/Save", null, fileKymoTab, "Load/Save kymographs");
		
		tabbedKymosPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		buildKymosTab.addPropertyChangeListener(parent);
		kymosPanel.add(GuiUtil.besidesPanel(tabbedKymosPane));
	}
	
	public void UpdateItemsFromSequence(SequenceVirtual vSequence) {
		buildKymosTab.UpdateItemsFromSequence (vSequence);
	}
	
	public void UpdateItemsToSequence(SequenceVirtual vSequence) {
		buildKymosTab.UpdateItemsToSequence ( vSequence);
	}
}
