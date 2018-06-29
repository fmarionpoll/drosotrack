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
import javax.swing.Timer;
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
import icy.sequence.DimensionId;
import icy.system.profile.Chronometer;
import icy.system.thread.ThreadUtil;

import loci.formats.FormatException;
import plugins.fmp.capillarytrack.BuildKymographsThread;
import plugins.fmp.sequencevirtual.*;

public class BuildKymos extends PluginActionable implements ActionListener, ChangeListener, ViewerListener
{
	// -------------------------------------- interface
	private IcyFrame 	mainFrame 				= new IcyFrame("Build Kymographs from list 04-04-2018", true, true, true, true);

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
	private Timer checkBufferTimer				= new Timer(1000, this);
	private int	analyzeStep = 1;

	private int diskRadius = 5;
	private int numberOfImageForBuffer = 100;
	
	// results arrays
	private ArrayList <SequencePlus> 	kymographArrayList 		= new ArrayList <SequencePlus> ();		// list of kymograph sequences

	enum StatusComputation {START_COMPUTATION, STOP_COMPUTATION};
	private StatusComputation 	sComputation = StatusComputation.START_COMPUTATION; 
	private BuildKymographsThread buildKymographsThread = null;
	private Viewer viewer1 = null;
	
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
		checkBufferTimer.start();
	}

	// ------------------------------------------
	private void getListofXMLFiles() {
		
		File dir = Tools.chooseDirectory();
		Path pdir = Paths.get(dir.getAbsolutePath());
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
	
	private void initInputSeq2 () {

		addSequence(vinputSequence);
		viewer1 = vinputSequence.getFirstViewer();
		viewer1.addListener(BuildKymos.this);
	
		Rectangle rectv = viewer1.getBoundsInternal();
		Rectangle rect0 = mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		viewer1.setBounds(rectv);
		vinputSequence.removeAllImages();
		checkBufferTimer.start();	
		
		ThreadUtil.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				final Viewer v = Icy.getMainInterface().getFirstViewer(vinputSequence);
				if (v != null)
					v.addListener(BuildKymos.this);
			}
		});
	}
	
	private void startstopBufferingThread() {

		checkBufferTimer.stop();
		if (vinputSequence == null)
			return;

		vinputSequence.vImageBufferThread_STOP();
		vinputSequence.istep = analyzeStep;
		vinputSequence.vImageBufferThread_START(numberOfImageForBuffer);
		checkBufferTimer.start();
	}
	
	// ------------------------------------------
	@Override
	public void actionPerformed(ActionEvent e ) 
	{
		Object o = e.getSource();

		// _______________________________________________
		if (o == findButton) {
			getListofXMLFiles();	
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
		
		// _______________________________________________
		else if (o == startComputationButton) 
		{		
			int i = ((DefaultListModel<String>) xmlFilesJList.getModel()).getSize()-1;
			if (i >= 0) {
				i = 0;
				xmlFilesJList.setSelectedIndex(i);
				String oo = ((DefaultListModel<String>) xmlFilesJList.getModel()).getElementAt(i);
				// clear sequence
				if (vinputSequence == null) 
					vinputSequence = new SequenceVirtual();
				else
					vinputSequence.removeAllROI();
				// load parameters
				vinputSequence.xmlReadROIsAndData(oo);
				if (vinputSequence.sourceFile == null)
				{
					// try reading files from the same directory as the xml file
					File oofile = new File(oo);
					String csdummy = oofile.getParentFile().getAbsolutePath();
					vinputSequence.loadInputVirtualFromName(csdummy);
					
					if (vinputSequence.status == SequenceVirtual.Status.FAILURE) {
					String name = vinputSequence.loadInputVirtualStack();
						if (name.isEmpty())
							return;
						vinputSequence.sourceFile = name;
					}
					else
						vinputSequence.sourceFile = csdummy;
					vinputSequence.xmlWriteROIsAndDataNoQuestion(oo);
				}
				vinputSequence.loadInputVirtualFromName(vinputSequence.sourceFile);
				startstopBufferingThread();
				if (viewer1 == null)
					initInputSeq2();
				boolean flag = vinputSequence.setCurrentVImage(0);
				
				if (flag) {
					// build kymograph
					buildKymographsThread = new BuildKymographsThread();
					buildKymographsThread.vinputSequence  		= vinputSequence;
					buildKymographsThread.analyzeStep 			= analyzeStep;
					buildKymographsThread.startFrame 			= (int) vinputSequence.analysisStart;
					buildKymographsThread.endFrame 				= (int) vinputSequence.nTotalFrames-1;
					buildKymographsThread.diskRadius 			= diskRadius;
					buildKymographsThread.kymographArrayList 	= kymographArrayList;
					buildKymographsThread.start();

					// change display status
					sComputation = StatusComputation.STOP_COMPUTATION;
					stopComputationButton.setEnabled(true);
					startComputationButton.setEnabled(false);
					
					//observer thread for notifications
					Thread waitcompletionThread = new Thread(new Runnable(){public void run()
					{
						try{buildKymographsThread.join();}
						catch(Exception e){;} 
						finally{ stopComputationButton.doClick();}
					}});
					waitcompletionThread.start();
				}
			}
		}

		// _______________________________________________
		else if ( o == stopComputationButton ) {
			boolean gotonext = true;
			if (sComputation == StatusComputation.STOP_COMPUTATION) {
				if (buildKymographsThread.isAlive()) {
					gotonext = false;
					buildKymographsThread.interrupt();
					try {
						buildKymographsThread.join();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					buildKymographsThread.progress.close();
				}
				else {
					kymographsSaveToFileIntoResults();
				}
			}
			
			sComputation = StatusComputation.START_COMPUTATION;
			startComputationButton.setEnabled(true);
			if (gotonext && ((DefaultListModel<String>) xmlFilesJList.getModel()).getSize() > 0)
			{
				String oo = ((DefaultListModel<String>) xmlFilesJList.getModel()).getElementAt(0);
				((DefaultListModel<String>) xmlFilesJList.getModel()).removeElement(oo);				
				startComputationButton.doClick();
			}

			if (((DefaultListModel<String>) xmlFilesJList.getModel()).getSize() == 0) {
				viewer1.close();
				viewer1 = null;
			}
		}
	}


	private void kymographsSaveToFileIntoResults() {

		Path dir = Paths.get(vinputSequence.sourceFile).getParent();
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
		Chronometer chrono = new Chronometer("Tracking computation" );
		int nbSecondsStart =  0;
		int nbSecondsEnd = 0;

		for (SequencePlus seq: kymographArrayList) {

			progress.setMessage( "Save kymograph file : " + seq.getName());
			nbSecondsStart =  (int) (chrono.getNanos() / 1000000000f);
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
			nbSecondsEnd =  (int) (chrono.getNanos() / 1000000000f);
			System.out.println("File "+ seq.getName() + " saved in: " + (nbSecondsEnd-nbSecondsStart) + " s");
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

