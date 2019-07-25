package plugins.fmp.multicafeExport;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

import icy.gui.util.GuiUtil;
import icy.preferences.XMLPreferences;
import plugins.fmp.multicafeTools.MulticafeTools;

public class ListFilesPane extends JPanel implements PropertyChangeListener, ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -41617764209983340L;
	
	public JTabbedPane 	tabsPane 	= new JTabbedPane();
	public JTextField 	filterTextField 	= new JTextField("roisline");
	public JButton 	findButton				= new JButton("Select root directory and search...");
	public JButton 	clearSelectedButton		= new JButton("Clear selected");
	public JButton 	clearAllButton			= new JButton("Clear all");
	public JList<String> xmlFilesJList			= new JList<String>(new DefaultListModel<String>());
	
	private ExportMultiCAFE 	parent0 	= null;
	
	
	public void init (JPanel mainPanel, String string, ExportMultiCAFE parent0) {
		this.parent0 = parent0;
		final JPanel sourcePanel = GuiUtil.generatePanel("SOURCE");
		mainPanel.add(GuiUtil.besidesPanel(sourcePanel));
		
		JPanel k0Panel = new JPanel();
		k0Panel.setLayout(new BorderLayout());
		JLabel filterLabel = new JLabel("File pattern: ");
		k0Panel.add(filterLabel, BorderLayout.LINE_START); 
		k0Panel.add(filterTextField, BorderLayout.PAGE_END);
		
		sourcePanel.add(GuiUtil.besidesPanel(k0Panel, findButton));
		xmlFilesJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		xmlFilesJList.setLayoutOrientation(JList.VERTICAL);
		xmlFilesJList.setVisibleRowCount(20);
		JScrollPane scrollPane = new JScrollPane(xmlFilesJList);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		sourcePanel.add(GuiUtil.besidesPanel(scrollPane));
		sourcePanel.add(GuiUtil.besidesPanel(clearSelectedButton, clearAllButton));
		
		findButton.addActionListener(this);
		clearSelectedButton.addActionListener(this);
		clearAllButton.addActionListener(this);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent event) {

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == findButton) {
			getListofFiles();	
		}
		
		else if (o == clearSelectedButton) {
			List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
		    for (String oo: selectedItems)
		    	 ((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);
		}
		else if (o == clearAllButton) {
			((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
		}

	}
	
	private void getListofFiles() {
		
		XMLPreferences guiPrefs = parent0.getPreferences("gui");
		String lastUsedPathString = guiPrefs.get("lastUsedPath", "");
		File dir = MulticafeTools.chooseDirectory(lastUsedPathString);
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

}

