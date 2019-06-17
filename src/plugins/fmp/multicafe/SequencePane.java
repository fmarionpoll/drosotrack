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
	
	public JTabbedPane 			tabsPane 	= new JTabbedPane();
	public SequenceTab_Open 	fileTab 	= new SequenceTab_Open();
	public SequenceTab_Options 	optionsTab 	= new SequenceTab_Options();
	public SequenceTab_Close 	closeTab 	= new SequenceTab_Close();
	public SequenceTab_List		listTab		= new SequenceTab_List();
	private Multicafe 			parent0 	= null;
	
	
	public void init (JPanel mainPanel, String string, Multicafe parent0) {
		this.parent0 = parent0;
		final JPanel capPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(capPanel));
		GridLayout capLayout = new GridLayout(4, 1);
		
		fileTab.init(capLayout, parent0);
		tabsPane.addTab("Open/Add", null, fileTab, "Open one or several stacks of .jpg files");
		fileTab.addPropertyChangeListener(this);
		
		optionsTab.init(capLayout);
		tabsPane.addTab("Options", null, optionsTab, "Parameters to read/analyze stack");
		optionsTab.addPropertyChangeListener(this);
		
		listTab.init(capLayout, parent0);
		tabsPane.addTab("List", null, listTab, "Define list of files");
		listTab.addPropertyChangeListener(this);
		
		closeTab.init(capLayout, parent0);
		tabsPane.addTab("Close", null, closeTab, "Close file and associated windows");
		closeTab.addPropertyChangeListener(this);

		tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("SEQ_OPEN")) {
			if (parent0.vSequence== null) {
				String filename = (String) fileTab.experimentComboBox.getSelectedItem();
				sequenceCreateNew(filename);
				updateParametersForSequence();
				firePropertyChange("SEQ_OPEN", false, true);
			}
			else if (sequenceCreateNew(null)) {
				fileTab.experimentComboBox.removeAllItems();
				fileTab.experimentComboBox.addItem(parent0.vSequence.getFileName());
				updateParametersForSequence();
				firePropertyChange("SEQ_OPEN", false, true);
			}
		}
		else if (event.getPropertyName().equals("SEQ_ADD")) {
			if (sequenceCreateNew(null)) {
				String strItem = parent0.vSequence.getFileName();
				sequenceAddtoCombo(strItem);
				fileTab.experimentComboBox.setSelectedItem(strItem);
				updateParametersForSequence();
				firePropertyChange("SEQ_OPEN", false, true);
			}
		 }	
		 else if (event.getPropertyName().equals("UPDATE")) {
			optionsTab.UpdateItemsToSequence(parent0);
			ArrayList<Viewer>vList =  parent0.vSequence.getViewers();
			Viewer v = vList.get(0);
			v.toFront();
			v.requestFocus();
		 }
		 else if (event.getPropertyName().equals("SEQ_CHANGE")) {
			String filename = (String) fileTab.experimentComboBox.getSelectedItem();
			sequenceCreateNew(filename);
			updateParametersForSequence();
			firePropertyChange("SEQ_OPEN", false, true);
		}
		 else if (event.getPropertyName().equals("SEQ_CLOSE")) {
			tabsPane.setSelectedIndex(0);
			fileTab.experimentComboBox.removeAllItems();
			firePropertyChange("SEQ_CLOSE", false, true);
		 }
	}
	
	public void sequenceAddtoCombo(String strItem) {
		int nitems = fileTab.experimentComboBox.getItemCount();
		boolean alreadystored = false;
		for (int i=0; i < nitems; i++) {
			if (strItem.equals(fileTab.experimentComboBox.getItemAt(i))) {
				alreadystored = true;
				break;
			}
		}
		if(!alreadystored) {
			fileTab.experimentComboBox.addItem(strItem);
		}
	}
	
	// -------------------------
	public void startstopBufferingThread() {

		if (parent0.vSequence == null)
			return;

		parent0.vSequence.vImageBufferThread_STOP();
		optionsTab.UpdateItemsToSequence(parent0); ;
		parent0.vSequence.vImageBufferThread_START(100); //numberOfImageForBuffer);
	}
	
	public boolean sequenceCreateNew (String filename) {
		if (parent0.vSequence != null)
			parent0.vSequence.close();		
		parent0.vSequence = new SequenceVirtual();
		
		String path = parent0.vSequence.loadVirtualStackAt(filename);
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
	
	private void updateParametersForSequence() {
		int endFrame = parent0.vSequence.getSizeT()-1;
		optionsTab.endFrameTextField.setText( Integer.toString(endFrame));
		tabsPane.setSelectedIndex(1);
		Viewer v = parent0.vSequence.getFirstViewer();
		Rectangle rectv = v.getBoundsInternal();
		Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		v.setBounds(rectv);
	}
	
	private void initSequenceParameters(SequenceVirtual seq) {
		if (seq.analysisEnd == 99999999) {
			seq.analysisStart = 0;
			seq.analysisEnd = seq.getSizeT()-1;
			seq.analysisStep = 1;
		}
	}

}
