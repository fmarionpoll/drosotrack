package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.preferences.XMLPreferences;
import plugins.fmp.sequencevirtual.SequenceVirtual;


public class SequencePane extends JPanel implements PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6826269677524125173L;
	
	public JTabbedPane tabsPane 			= new JTabbedPane();
	public SequenceTab_Open fileTab 		= new SequenceTab_Open();
	public SequenceTab_Options optionsTab 	= new SequenceTab_Options();
	public SequenceTab_Close closeTab 		= new SequenceTab_Close();
	private Multicafe parent0 = null;
	JPanel capPanel ;
	
	public void init (JPanel mainPanel, String string, Multicafe parent0) {
		this.parent0 = parent0;
		capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		fileTab.init(capLayout);
		tabsPane.addTab("Open/select", null, fileTab, "Open stack of files (click on one only) or an AVI file, or select experiment");
		fileTab.addPropertyChangeListener(this);
		
		optionsTab.init(capLayout);
		tabsPane.addTab("Options", null, optionsTab, "change parameters reading file - beginning, end, step");
		optionsTab.addPropertyChangeListener(this);
		
		closeTab.init(capLayout, parent0);
		tabsPane.addTab("Close", null, closeTab, "close file and associated windows");
		closeTab.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	public void UpdateItemsFromSequence(SequenceVirtual vSequence) {
		optionsTab.UpdateItemsFromSequence (vSequence);
	}
	
	public void UpdateItemsToSequence(SequenceVirtual vSequence) {
		optionsTab.UpdateItemsToSequence ( vSequence);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("SEQ_OPEN")) {
			if (sequenceOpenFile()) {
				int endFrame = parent0.vSequence.getSizeT()-1;
				optionsTab.endFrameTextField.setText( Integer.toString(endFrame));
				optionsTab.experimentComboBox.addItem(parent0.vSequence.getName());
				tabsPane.setSelectedIndex(1);
				Viewer v = parent0.vSequence.getFirstViewer();
				Rectangle rectv = v.getBoundsInternal();
				Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
				rectv.setLocation(rect0.x+ rect0.width, rect0.y);
				v.setBounds(rectv);
				firePropertyChange("SEQ_OPEN", false, true);
			}
		 }			  
		 else if (event.getPropertyName().equals("UPDATE")) {
			optionsTab.UpdateItemsToSequence(parent0.vSequence);
			ArrayList<Viewer>vList =  parent0.vSequence.getViewers();
			Viewer v = vList.get(0);
			v.toFront();
			v.requestFocus();
		 }
		 else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			tabsPane.setSelectedIndex(0);
			firePropertyChange("SEQ_CLOSE", false, true);
			
		 }
	}
	
	// -------------------------
	public void startstopBufferingThread() {

		if (parent0.vSequence == null)
			return;

		parent0.vSequence.vImageBufferThread_STOP();
		UpdateItemsToSequence(parent0.vSequence); ;
		parent0.vSequence.vImageBufferThread_START(100); //numberOfImageForBuffer);
	}
	
	public boolean sequenceOpenFile() {
		if (parent0.vSequence != null)
			parent0.vSequence.close();		
		parent0.vSequence = new SequenceVirtual();
		
		String path = parent0.vSequence.loadInputVirtualStack(null);
		if (path != null) {
			initSequenceParameters(parent0.vSequence);
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			parent0.addSequence(parent0.vSequence);
			parent0.vSequence.addListener(parent0);
			startstopBufferingThread();
		}
		return (path != null);
	}
	
	private void initSequenceParameters(SequenceVirtual seq) {
		if (seq.analysisEnd == 99999999) {
			seq.analysisStart = 0;
			seq.analysisEnd = seq.getSizeT()-1;
			seq.analysisStep = 1;
		}
	}

}
