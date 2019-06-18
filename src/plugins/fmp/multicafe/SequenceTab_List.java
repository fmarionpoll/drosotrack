package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import icy.util.XMLUtil;
import plugins.fmp.sequencevirtual.Capillaries;
import plugins.fmp.tools.Tools;

public class SequenceTab_List  extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -41617764209983340L;
	
	public JTabbedPane 	tabsPane 	= new JTabbedPane();
	public JTextField 	filterTextField 	= new JTextField("capillarytrack");
	public JButton 		findButton			= new JButton("Select root directory and search...");
	
	public JButton 		clearSelectedButton	= new JButton("Clear selected");
	public JButton 		clearAllButton		= new JButton("Clear all");
	public JButton 		addSelectedButton	= new JButton("Add selected");
	public JButton 		addAllButton		= new JButton("Add all");
	public JList<String> xmlFilesJList		= new JList<String>(new DefaultListModel<String>());
	IcyFrame mainFrame = null;
	
	
	public JButton		showButton 	= new JButton("Search for files...");
	public JButton		closeButton	= new JButton("Close search dialog");
	private Multicafe 	parent0 	= null;
		
	public void init (GridLayout capLayout,  Multicafe parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);
		add(GuiUtil.besidesPanel(showButton, closeButton));
		showButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	showDialog();
            }
        });
		
		closeButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	closeDialog();
            }
        });
	}
	
	private void closeDialog() {
		mainFrame.close();
		mainFrame = null;
		firePropertyChange("SEARCH_CLOSED", false, true);
	}
	
	private void showDialog() {
		if (mainFrame != null)
			closeDialog();
		
		mainFrame = new IcyFrame ("Dialog box to select files", true, true);
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		
		mainPanel.add(GuiUtil.besidesPanel(findButton, filterTextField));
		
		xmlFilesJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		xmlFilesJList.setLayoutOrientation(JList.VERTICAL);
		xmlFilesJList.setVisibleRowCount(20);
		JScrollPane scrollPane = new JScrollPane(xmlFilesJList);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(GuiUtil.besidesPanel(scrollPane));
		
		mainPanel.add(GuiUtil.besidesPanel(clearSelectedButton, clearAllButton));
		mainPanel.add(GuiUtil.besidesPanel(addSelectedButton, addAllButton));
		
		addActionListeners();
		
		mainFrame.pack();
		mainFrame.addToDesktopPane();
		mainFrame.requestFocus();
		mainFrame.center();
		mainFrame.setVisible(true);
	}
	
	void addActionListeners() {
		findButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	findButton.setEnabled(false);
    			final String pattern = filterTextField.getText();
    			getListofFilesMatchingPattern(pattern);
    			findButton.setEnabled(true);
            }
        });
		
		clearSelectedButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
    		    removeList (selectedItems);
            }
        });
		
		clearAllButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
            }
        });
		
		addSelectedButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
    			addJPGFilesToCombo(selectedItems);
    			removeList(selectedItems);
            }
        });
		
		addAllButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
    			List<String> allItems = new ArrayList <String> ();
    			for(int i = 0; i< xmlFilesJList.getModel().getSize();i++)
    			    allItems.add(xmlFilesJList.getModel().getElementAt(i));
    			addJPGFilesToCombo(allItems);
    			((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
            }
        });
		
	}
		
	private void addJPGFilesToCombo(List<String> allItems) {
		parent0.sequencePane.fileTab.disableChangeFile = true;
		for (String csFileName : allItems) {
			String directory = Paths.get(csFileName).getParent().toString();
			
			Capillaries dummyCap = new Capillaries();
			final Document doc = XMLUtil.loadDocument(csFileName);
			dummyCap.xmlReadCapillaryParameters(doc);
			String filename = FilenameUtils.getName(dummyCap.sourceName);
			parent0.sequencePane.sequenceAddtoCombo(directory+ "/"+ filename);
		}
		parent0.sequencePane.fileTab.disableChangeFile = false;
	}
	
	private void removeList(List<String> selectedItems) {
		for (String oo: selectedItems)
	    	 ((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);
	}
	
	private void getListofFilesMatchingPattern(String pattern) {
		
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		String lastUsedPathString = guiPrefs.get("lastUsedPath", "");
		File dir = Tools.chooseDirectory(lastUsedPathString);
		if (dir == null) {
			return;
		}
		lastUsedPathString = dir.getAbsolutePath();
		guiPrefs.put("lastUsedPath", lastUsedPathString);
		Path pdir = Paths.get(lastUsedPathString);
				
		try {
			Files.walk(pdir)
			.filter(Files::isRegularFile)
			.forEach((f)->{
			    String fileName = f.toString();
			    if( fileName.contains(pattern)) {
			    	addIfNew(fileName);
			    }
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addIfNew(String fileName) {
		
		fileName = fileName.toLowerCase();
		
		int ilast = ((DefaultListModel<String>) xmlFilesJList.getModel()).getSize();
		boolean found = false;
		for (int i=0; i < ilast; i++)
		{
			String oo = ((DefaultListModel<String>) xmlFilesJList.getModel()).getElementAt(i);
			if (oo.equals(fileName)) {
				found = true;
				break;
			}
		}
		if (!found)
			((DefaultListModel<String>) xmlFilesJList.getModel()).addElement(fileName);
	}

}
