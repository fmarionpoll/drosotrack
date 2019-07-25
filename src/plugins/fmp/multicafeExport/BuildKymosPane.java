package plugins.fmp.exportMulticafe;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.preferences.XMLPreferences;
import icy.sequence.DimensionId;
import icy.system.thread.ThreadUtil;
import loci.formats.FormatException;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.EnumStatus;
import plugins.fmp.tools.BuildKymographsThread;
import plugins.fmp.tools.EnumStatusComputation;

public class BuildKymosPane  extends JPanel implements PropertyChangeListener, ActionListener, ViewerListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1610357726091762089L;
	public JButton startComputationButton 	= new JButton("Start");
	public JButton stopComputationButton 	= new JButton("Stop");
	
	SequenceVirtual vinputSequence 		= null;
	private ArrayList <SequencePlus> 	kymographArrayList 		= new ArrayList <SequencePlus> ();		// list of kymograph sequences
	
	private EnumStatusComputation sComputation = EnumStatusComputation.START_COMPUTATION; 
	private BuildKymographsThread buildKymographsThread = null;
	private Viewer viewer1 = null;
	private Thread thread = null;
	private int	analyzeStep = 1;
	private int diskRadius = 5;


	
private ExportMultiCAFE 	parent0 	= null;
	
	public void init (JPanel mainPanel, String string, ExportMultiCAFE parent0) {
		this.parent0 = parent0;
		
		final JPanel kymographsPanel = GuiUtil.generatePanel("KYMOGRAPHS");
		mainPanel.add(GuiUtil.besidesPanel(kymographsPanel));
		kymographsPanel.add(GuiUtil.besidesPanel(startComputationButton, stopComputationButton));
		JLabel startLabel = new JLabel("start "); 
		startLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel endLabel = new JLabel("end "); 
		endLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		startComputationButton.addActionListener(this);
		stopComputationButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (o == startComputationButton) {		
			startComputation();
		}

		else if ( o == stopComputationButton ) {
			stopComputation();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}
	
	private void startComputation() {
		
		if (((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getSize() == 0) 
			return;
				
		parent0.listFilesPane.xmlFilesJList.setSelectedIndex(0);
		String oo = ((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getElementAt(0);
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

	private boolean loadSequence(String oo) {

		// open sequence
		File oofile = new File(oo);
		String csdummy = oofile.getParentFile().getAbsolutePath();
		
		vinputSequence = new SequenceVirtual();
		vinputSequence.loadInputVirtualFromName(csdummy);
		vinputSequence.setFileName(csdummy);
		if (vinputSequence.status == EnumStatus.FAILURE) {
			XMLPreferences guiPrefs = parent0.getPreferences("gui");
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
		Rectangle rect0 = parent0.mainFrame.getBoundsInternal();
		rectv.setLocation(rect0.x+ rect0.width, rect0.y);
		viewer1.setBounds(rectv);
	}
	
	private void startstopBufferingThread() {

		if (vinputSequence == null)
			return;

		vinputSequence.vImageBufferThread_STOP();
		vinputSequence.analysisStep = analyzeStep;
		vinputSequence.vImageBufferThread_START(100); //numberOfImageForBuffer);
	}

	private void nextComputation() {
		kymographsSaveToFileIntoResults();
		closeSequence();
		if (sComputation == EnumStatusComputation.STOP_COMPUTATION) {
			sComputation = EnumStatusComputation.START_COMPUTATION;
			startComputationButton.setEnabled(true);
			String oo = ((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).getElementAt(0);
			((DefaultListModel<String>) parent0.listFilesPane.xmlFilesJList.getModel()).removeElement(oo);
			startComputationButton.doClick();
		}
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
	
	private void closeSequence() {
		
		for (SequencePlus seq:kymographArrayList)
			seq.close();
		kymographArrayList.clear();
		vinputSequence.capillaries.capillariesArrayList.clear();
		vinputSequence.close();
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
