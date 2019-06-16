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

import org.w3c.dom.Document;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.IcyFrameEvent;
import icy.gui.frame.IcyFrameListener;
import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import icy.util.XMLUtil;
import plugins.fmp.sequencevirtual.Capillaries;
import plugins.fmp.tools.Tools;

public class SequenceTab_List  extends JPanel implements ActionListener, IcyFrameListener {
	
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
	
	
	public JButton	showButton = new JButton("show dialog");
	private Multicafe 	parent0 	= null;
		
	public void init (GridLayout capLayout,  Multicafe parent0) {
		this.parent0 = parent0;
		setLayout(capLayout);
		add(GuiUtil.besidesPanel(showButton));
		showButton.addActionListener(this);
	}
	
	private void showDialog() {
		if (mainFrame != null)
			return;
		
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
		
		findButton.addActionListener(this);
		clearSelectedButton.addActionListener(this);
		clearAllButton.addActionListener(this);
		addSelectedButton.addActionListener(this);
		addAllButton.addActionListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
		mainFrame.center();
		mainFrame.addFrameListener( this );
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == findButton) {
			getListofFiles();	
		}
		else if (o == clearSelectedButton) {
			List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
		    removeList (selectedItems);
		}
		else if (o == clearAllButton) {
			((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
		}
		else if (o == showButton) {
			showDialog();
		}
		else if (o == addSelectedButton) {
			List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
			addJPGFilesToCombo(selectedItems);
			removeList(selectedItems);
		}
		else if (o == addAllButton) {
			
			List<String> allItems = new ArrayList <String> ();
			for(int i = 0; i< xmlFilesJList.getModel().getSize();i++)
			    allItems.add(xmlFilesJList.getModel().getElementAt(i));
			addJPGFilesToCombo(allItems);
			((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
		}
	}
	
	private void addJPGFilesToCombo(List<String> allItems) {
		parent0.sequencePane.fileTab.disableChangeFile = true;
		Capillaries dummyCap = new Capillaries();
		for (String csFileName : allItems) {
			final Document doc = XMLUtil.loadDocument(csFileName);
			dummyCap.xmlReadCapillaryParameters(doc);
			parent0.sequencePane.addFileToCombo(dummyCap.sourceName);
		}
		parent0.sequencePane.fileTab.disableChangeFile = false;
	}
	
	private void removeList(List<String> selectedItems) {
		for (String oo: selectedItems)
	    	 ((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);
	}
	
	private void getListofFiles() {
		
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		String lastUsedPathString = guiPrefs.get("lastUsedPath", "");
		File dir = Tools.chooseDirectory(lastUsedPathString);
		if (dir == null) {
			return;
		}
		lastUsedPathString = dir.getAbsolutePath();
		guiPrefs.put("lastUsedPath", lastUsedPathString);
		Path pdir = Paths.get(lastUsedPathString);
		String extension = filterTextField.getText();
		
		try {
			Files.walk(pdir)
			.filter(Files::isRegularFile)
			.forEach((f)->{
			    String fileName = f.toString();
			    if( fileName.contains(extension)) {
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


	@Override
	public void icyFrameOpened(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void icyFrameClosing(IcyFrameEvent e) {
		// TODO Auto-generated method stub
		
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
