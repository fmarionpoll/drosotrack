package plugins.fmp.multicafeExport;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.IcyFrame;
import icy.gui.util.GuiUtil;
import icy.plugin.abstract_.PluginActionable;


public class ExportMultiCAFE  extends PluginActionable implements ChangeListener, PropertyChangeListener {

	IcyFrame 		mainFrame 			= new IcyFrame("Export capillarytrack analyses 16-06-2019", true, true, true, true);
	ListFilesPane 	listFilesPane 		= new ListFilesPane();
	BuildKymosPane	buildKymosPane		= new BuildKymosPane();

	@Override
	public void run() {

		// build and display the GUI
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);
		
		listFilesPane.init(mainPanel, "SOURCE", this);
		listFilesPane.addPropertyChangeListener(this);
		
		buildKymosPane.init(mainPanel, "KYMOGRAPHS", this);
		buildKymosPane.addPropertyChangeListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// ignore
		System.out.println("state change detected");
	}


	@Override
	public void propertyChange(PropertyChangeEvent evt) {	
	}

}
