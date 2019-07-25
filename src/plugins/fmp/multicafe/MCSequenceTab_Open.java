package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import icy.util.XMLUtil;
import plugins.fmp.toolsMulticafe.Tools;
import plugins.fmp.toolsSequence.Capillaries;


public class MCSequenceTab_Open extends JPanel implements IcyFrameListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6565346204580890307L;
	private JButton 	openButton				= new JButton("Open...");
	private JButton 	addButton				= new JButton("Add...");
	private JButton		showButton 				= new JButton("Search for files...");
	private JButton		closeButton				= new JButton("Close search dialog");
	private JCheckBox	capillariesCheckBox		= new JCheckBox("capillaries", true);
	private JCheckBox	cagesCheckBox			= new JCheckBox("cages", true);
	private JCheckBox	kymographsCheckBox		= new JCheckBox("kymographs", true);
	private JCheckBox	measuresCheckBox		= new JCheckBox("measures", true);
	JCheckBox			graphsCheckBox			= new JCheckBox("graphs", true);

	private JTextField 	filterTextField 		= new JTextField("capillarytrack");
	private JButton 	findButton				= new JButton("Select root directory and search...");
	private JButton 	clearSelectedButton		= new JButton("Clear selected");
	private JButton 	clearAllButton			= new JButton("Clear all");
	private JButton 	addSelectedButton		= new JButton("Add selected");
	private JButton 	addAllButton			= new JButton("Add all");
	private JList<String> 	xmlFilesJList		= new JList<String>(new DefaultListModel<String>());
	
	public List<String> 	selectedNames = new ArrayList<String> ();
	IcyFrame mainFrame = null;
	private Multicafe parent0 = null;
	private boolean isSearchRunning = false;

	
	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(openButton, addButton));
		add( GuiUtil.besidesPanel(showButton, closeButton));
		add( GuiUtil.besidesPanel(capillariesCheckBox, kymographsCheckBox, cagesCheckBox, measuresCheckBox, graphsCheckBox));
		
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
            	firePropertyChange("SEARCH_CLOSED", false, true);
            }
        });
		
		openButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	if(parent0.sequencePane.infosTab.experimentComboBox.getItemCount() > 0 )
            		parent0.sequencePane.closeTab.closeAll();
            	firePropertyChange("SEQ_OPENFILE", false, true);
            }
         });
  
		addButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	firePropertyChange("SEQ_ADDFILE", false, true);
            }
         });
	}
	
	boolean isCheckedLoadPreviousProfiles() {
		return capillariesCheckBox.isSelected();
	}
	
	boolean isCheckedLoadKymographs() {
		return kymographsCheckBox.isSelected();
	}
	
	boolean isCheckedLoadCages() {
		return cagesCheckBox.isSelected();
	}
	
	boolean isCheckedLoadMeasures() {
		return measuresCheckBox.isSelected();
	}
	
	private void closeDialog() {
		mainFrame.close();
	}
	
	private void showDialog() {
		if (mainFrame != null) {
			mainFrame.close();
			mainFrame = null;
		}
		
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
    			final String pattern = filterTextField.getText();
    			if (isSearchRunning) 
    				return;

    	      	SwingUtilities.invokeLater(new Runnable() { public void run() {
    	      		isSearchRunning = true;
    	    		ProgressFrame progress = new ProgressFrame("Browsing directories to find files matching the searched name...");
    	    		getXmlListofFilesMatchingPattern(pattern);
    	    		progress.close();
    	    		isSearchRunning = false;
    	      	}});
            }
        });
		
		clearSelectedButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
            	List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
    		    removeListofNamesFromXmlList (selectedItems);
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
    			addNamesToSelectedList(selectedItems);
    			removeListofNamesFromXmlList(selectedItems);
            }
        });
		
		addAllButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
    			List<String> allItems = new ArrayList <String> ();
    			for(int i = 0; i< xmlFilesJList.getModel().getSize();i++)
    			    allItems.add(xmlFilesJList.getModel().getElementAt(i));
    			addNamesToSelectedList(allItems);
    			((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
    			mainFrame.close();
    			firePropertyChange("SEARCH_CLOSED", false, true);
            }
        });
		
	}
		
	private void addNamesToSelectedList(List<String> stringList) {
		
		for (String name : stringList) {
			String directory = Paths.get(name).getParent().toString();
			Capillaries dummyCap = new Capillaries();
			final Document doc = XMLUtil.loadDocument(name);
			dummyCap.xmlReadCapillaryParameters(doc);
			String filename = directory+ "/"+ FilenameUtils.getName(dummyCap.sourceName);
			selectedNames.add(filename);
		}
	}
	
	private void removeListofNamesFromXmlList(List<String> selectedItems) {
		for (String oo: selectedItems)
	    	 ((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);
	}
	
 	private void getXmlListofFilesMatchingPattern(String pattern) {
		
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		String lastUsedPathString = guiPrefs.get("lastUsedPath", "");
		File dir = Tools.chooseDirectory(lastUsedPathString);
		if (dir == null) {
			return;
		}
		
		lastUsedPathString = dir.getAbsolutePath();
		guiPrefs.put("lastUsedPath", lastUsedPathString);
	
		try {
			Files.walk(Paths.get(lastUsedPathString))
//			.collect(Collectors.toList())
//			.parallelStream()
			.filter(Files::isRegularFile)		
			.forEach((f)->{
			    String fileName = f.toString();
			    if( fileName.contains(pattern)) {
			    	addNameToXmlListIfNew(fileName);
			    }
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void addNameToXmlListIfNew(String fileName) {
		
		int ilast = ((DefaultListModel<String>) xmlFilesJList.getModel()).getSize();
		boolean found = false;
		for (int i=0; i < ilast; i++)
		{
			String oo = ((DefaultListModel<String>) xmlFilesJList.getModel()).getElementAt(i);
			if (oo.equalsIgnoreCase (fileName)) {
				found = true;
				break;
			}
		}
		if (!found)
			((DefaultListModel<String>) xmlFilesJList.getModel()).addElement(fileName);
	}

	@Override
	public void icyFrameOpened(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameClosing(IcyFrameEvent e) {
	}

	@Override
	public void icyFrameClosed(IcyFrameEvent e) {
		mainFrame = null;
	}

	@Override
	public void icyFrameIconified(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameDeiconified(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameActivated(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameDeactivated(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameInternalized(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameExternalized(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
