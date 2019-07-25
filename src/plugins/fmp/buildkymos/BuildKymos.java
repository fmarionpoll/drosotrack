package plugins.fmp.buildkymos;

import java.awt.BorderLayout;
import java.awt.Rectangle;
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
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.file.Saver;
import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.XMLPreferences;
import icy.sequence.DimensionId;
//import icy.system.profile.Chronometer;
import icy.system.thread.ThreadUtil;
import loci.formats.FormatException;
import plugins.fmp.sequencevirtual.*;
import plugins.fmp.tools.BuildKymographsThread;
import plugins.fmp.tools.EnumStatusComputation;
import plugins.fmp.tools.Tools;

public class BuildKymos extends PluginActionable implements ActionListener, ChangeListener, ViewerListener
{
	// -------------------------------------- interface
	private IcyFrame 	mainFrame 				= new IcyFrame("Build Kymographs from list 05-08-2018", true, true, true, true);

	// ---------------------------------------- list of files
	private JTextField 	filterTextField 		= new JTextField("roisline");
	private JList<String> xmlFilesJList			= new JList<String>(new DefaultListModel<String>());
	private JButton 	findButton				= new JButton("Select root directory and search...");
	private JButton 	clearSelectedButton		= new JButton("Clear selected");
	private JButton 	clearAllButton			= new JButton("Clear all");

	// ---------------------------------------- extract kymographs 
	private JButton 	startComputationButton 	= new JButton("Start");
	private JButton    	stopComputationButton 	= new JButton("Stop");
	
	//------------------------------------------- global variables
	private SequenceVirtual vinputSequence 		= null;
	private int	analyzeStep = 1;
	private int diskRadius = 5;
	
	// results arrays
	private ArrayList <SequencePlus> 	kymographArrayList 		= new ArrayList <SequencePlus> ();		// list of kymograph sequences
	private EnumStatusComputation sComputation = EnumStatusComputation.START_COMPUTATION; 
	private BuildKymographsThread buildKymographsThread = null;
	private Viewer viewer1 = null;
	private Thread thread = null;
	
	// -------------------------------------------
	@Override
	public void run() {

		// build and display the GUI
		JPanel mainPanel = GuiUtil.generatePanelWithoutBorder();
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(mainPanel, BorderLayout.CENTER);

		// ----------------- Source
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

		// ----------------- Kymographs
		final JPanel kymographsPanel = GuiUtil.generatePanel("KYMOGRAPHS");
		mainPanel.add(GuiUtil.besidesPanel(kymographsPanel));
		kymographsPanel.add(GuiUtil.besidesPanel(startComputationButton, stopComputationButton));
		JLabel startLabel = new JLabel("start "); 
		startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel endLabel = new JLabel("end "); 
		endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		// -------------------------------------------- action listeners, etc
		findButton.addActionListener(this);
		clearSelectedButton.addActionListener(this);
		clearAllButton.addActionListener(this);
		startComputationButton.addActionListener(this);
		stopComputationButton.addActionListener(this);
		
		mainFrame.pack();
		mainFrame.center();
		mainFrame.setVisible(true);
		mainFrame.addToDesktopPane();
	}

	// ------------------------------------------
	private void getListofFiles() {
		
		XMLPreferences guiPrefs = this.getPreferences("gui");
		String lastUsedPathString = guiPrefs.get("lastUsedPath", "");
		File dir = Tools.chooseDirectory(lastUsedPathString);
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
	
	private void startstopBufferingThread() {

		if (vinputSequence == null)
			return;

		vinputSequence.vImageBufferThread_STOP();
		vinputSequence.analysisStep = analyzeStep;
		vinputSequence.vImageBufferThread_START(100); //numberOfImageForBuffer);
	}
	
	// ------------------------------------------
	@Override
	public void actionPerformed(ActionEvent e ) 
	{
		Object o = e.getSource();
		if (o == findButton) {
			getListofFiles();	
			startComputationButton.setEnabled(true);
		}
		
		else if (o == clearSelectedButton) {
			List<String> selectedItems = xmlFilesJList.getSelectedValuesList();
		    for (String oo: selectedItems)
		    	 ((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);
		    startComputationButton.setEnabled(true);
		}
		else if (o == clearAllButton) {
			((DefaultListModel<String>) xmlFilesJList.getModel()).removeAllElements();
			startComputationButton.setEnabled(false);
		}
		
		else if (o == startComputationButton) {		
			startComputation();
		}

		else if ( o == stopComputationButton ) {
			stopComputation();
		}
	}

	private boolean loadSequence(String oo) {

		// open sequence
		File oofile = new File(oo);
		String csdummy = oofile.getParentFile().getAbsolutePath();
		
		vinputSequence = new SequenceVirtual();
		vinputSequence.loadInputVirtualFromName(csdummy);
		vinputSequence.setFileName(csdummy);
		if (vinputSequence.status == EnumStatus.FAILURE) {
			XMLPreferences guiPrefs = this.getPreferences("gui");
			String lastUsedPath = guiPrefs.get("lastUsedPath", "");
			String path = vinputSequence.loadInputVirtualStack(lastUsedPath);
			if (path.isEmpty())
				return false;
			vinputSequence.setFileName(path);
			guiPrefs.put("lastUsedPath", path);
			vinputSequence.loadInputVirtualFromName(vinputSequence.getFileName());
		}
		System.out.println("sequence openened: "+ vinputSequence.getFileName());

		return true;
	}
	
	private void loadRois(String oo) {
	
		System.out.println("add rois: "+ oo);
		vinputSequence.removeAllROI();
		vinputSequence.capillaries.xmlReadROIsAndData(oo, vinputSequence);
		vinputSequence.capillaries.extractLinesFromSequence(vinputSequence);
	}
	
	private void startComputation() {
		
		if (((DefaultListModel<String>) xmlFilesJList.getModel()).getSize() == 0) 
			return;
				
		xmlFilesJList.setSelectedIndex(0);
		String oo = ((DefaultListModel<String>) xmlFilesJList.getModel()).getElementAt(0);
		boolean flag = loadSequence(oo);
		if (!flag) {
			System.out.println("sequence "+oo+ " could not be opened: skip record");
			return;
		}
		loadRois(oo);
		initInputSequenceViewer();
		startstopBufferingThread();
		
		if (!vinputSequence.setCurrentVImage(0)) {
			System.out.println("first image from sequence "+oo+ " could not be opened: skip record");
			return;
		}
		
		// build kymograph
		buildKymographsThread.options.vSequence  	= vinputSequence;
		buildKymographsThread.options.analyzeStep 	= analyzeStep;
		buildKymographsThread.options.startFrame 	= (int) vinputSequence.analysisStart;
		buildKymographsThread.options.endFrame 		= (int) vinputSequence.nTotalFrames-1;
		buildKymographsThread.options.diskRadius 	= diskRadius;
		buildKymographsThread.kymographArrayList 	= kymographArrayList;
		
		thread = new Thread(buildKymographsThread);
		thread.start();

		// change display status
		sComputation = EnumStatusComputation.STOP_COMPUTATION;
		stopComputationButton.setEnabled(true);
		startComputationButton.setEnabled(false);
		
		Thread waitcompletionThread = new Thread(new Runnable(){public void run()
		{
			try{ 
				thread.join();
				}
			catch(Exception e){;} 
			finally { 
				nextComputation();
				}
		}});
		waitcompletionThread.start();
	}
	
	private void nextComputation() {
		kymographsSaveToFileIntoResults();
		closeSequence();
		if (sComputation == EnumStatusComputation.STOP_COMPUTATION) {
			sComputation = EnumStatusComputation.START_COMPUTATION;
			startComputationButton.setEnabled(true);
			String oo = ((DefaultListModel<String>) xmlFilesJList.getModel()).getElementAt(0);
			((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);
			startComputationButton.doClick();
		}
	}

	private void stopComputation() {
		
		if (sComputation == EnumStatusComputation.STOP_COMPUTATION) {
			if (thread.isAlive()) {
				thread.interrupt();
				try {
					thread.join();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		sComputation = EnumStatusComputation.START_COMPUTATION;
		startComputationButton.setEnabled(true);
	}
	
	private void initInputSequenceViewer () {

		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				viewer1 = new Viewer(vinputSequence, true);
			}
		}, true);

		
		if (viewer1 == null) {
			//addSequence(vinputSequence);
			viewer1 = Icy.getMainInterface().getFirstViewer(vinputSequence); 
			if (!viewer1.isInitialized()) {
				try {
					Thread.sleep(1000);
					if (!viewer1.isInitialized())
						System.out.println("Viewer still not initialized after 1 s waiting");
				} catch (InterruptedException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		Rectangle rectv = viewer1.getBoundsInternal();
		Rectangle rect0 = mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		viewer1.setBounds(rectv);
	}

	private void closeSequence() {
		
		for (SequencePlus seq:kymographArrayList)
			seq.close();
		kymographArrayList.clear();
		vinputSequence.capillaries.capillariesArrayList.clear();
		vinputSequence.close();
	}
	
	private void kymographsSaveToFileIntoResults() {

		Path dir = Paths.get(vinputSequence.getDirectory());
		dir = dir.resolve("results");
		String directory = dir.toAbsolutePath().toString();
		
		if (Files.notExists(dir))  {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Creating directory failed: "+ directory);
				return;
			}
		}

		// send some info
		ProgressFrame progress = new ProgressFrame("Save kymographs");
//		Chronometer chrono = new Chronometer("Tracking computation" );
//		int nbSecondsStart =  0;
//		int nbSecondsEnd = 0;

		for (SequencePlus seq: kymographArrayList) {

			progress.setMessage( "Save kymograph file : " + seq.getName());
//			nbSecondsStart =  (int) (chrono.getNanos() / 1000000000f);
			String filename = directory + "\\" + seq.getName() + ".tiff";
			File file = new File (filename);
			IcyBufferedImage image = seq.getFirstImage();
			try {
				Saver.saveImage(image, file, true);
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
//			nbSecondsEnd =  (int) (chrono.getNanos() / 1000000000f);
			//System.out.println("File "+ seq.getName() + " saved in: " + (nbSecondsEnd-nbSecondsStart) + " s");
		}
		System.out.println("End of Kymograph saving process");
		progress.close();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		// ignore
		System.out.println("state change detected");
	}

	@Override	
	public void viewerChanged(ViewerEvent event)
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
            vinputSequence.currentFrame = event.getSource().getPositionT() ;  
	}

	@Override
	public void viewerClosed(Viewer viewer)
	{
		viewer.removeListener(this);
	}

}

