package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import plugins.fmp.capillarytrack.Capillarytrack.StatusPane;

public class ResultsPane extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3841093170110565530L;
	public JTabbedPane tabsPane 			= new JTabbedPane();
	public ResultsTab_Graphics graphicsTab 	= new ResultsTab_Graphics();
	public ResultsTab_Excel excelTab 		= new ResultsTab_Excel();
	
	public void init (JPanel mainPanel, String string, Capillarytrack parent0) {
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(2, 2);
		
		graphicsTab.init(capLayout, parent0);
		tabsPane.addTab("Graphics", null, graphicsTab, "Display results as graphics");
		graphicsTab.addPropertyChangeListener(this);
		
		excelTab.init(capLayout, parent0);
		tabsPane.addTab("Export", null, excelTab, "Export results to Excel");
		excelTab.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getPropertyName().equals("EXPORT_TO_EXCEL")) {
			firePropertyChange("EXPORT_TO_EXCEL", false, true);	
		}
	}
	
	public void enableItems(StatusPane status) {
		boolean enable1 = !(status == StatusPane.DISABLED);
		graphicsTab.enableItems(enable1);
		excelTab.enableItems(enable1);
	}

}
