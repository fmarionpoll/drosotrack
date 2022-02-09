package plugins.fmp.fmpSequence;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.preferences.XMLPreferences;
import icy.roi.ROI;
import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;

public class DrosoSequence extends EzPlug {
	
	public SequenceVirtual vSequence = null;
	ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences

	boolean stopFlag = false;
	EzGroup groupLoadFiles;
	EzButton openFile;
	EzVarBoolean loadCapillaries = new EzVarBoolean("load capillaries", true);
	EzVarBoolean loadCages = new EzVarBoolean("load cages", true);
	
	EzGroup groupViewMode;
	EzVarInteger start = new EzVarInteger("start", 0, 9999999, 1);
	EzVarInteger end = new EzVarInteger("end", 0, 9999999, 1);
	EzVarInteger step = new EzVarInteger("step", 0, 9999999, 1);
	EzButton updateOptionsButton;
	EzVarBoolean displayCapillaries = new EzVarBoolean ("display capillaries", true);;
	EzVarBoolean displayCages = new EzVarBoolean ("display cages", true);
	
	EzGroup groupClose;
	EzButton closeAll;
	
	ActionListener runcloseall = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			closeAll();
		}
	};
	
	ActionListener updateoptions = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateVisuals();
		}
	};
	
	ActionListener openfile = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			openFile();
		}
	};
	
	@Override
	protected void initialize() {

		getUI().setTitle("SequenceVirtual");
		
//		loadKymographs 	= new EzVarBoolean("load kymographs", true);
//		loadMeasures 	= new EzVarBoolean("load measures", true);
//		groupLoadFiles 	= new EzGroup("Open", openFile, loadCapillaries, loadCages, loadKymographs, loadMeasures);
		openFile = new EzButton("Select file...", openfile);
		groupLoadFiles = new EzGroup("Open", 
				openFile, 
				loadCapillaries, 
				loadCages);
		super.addEzComponent(groupLoadFiles);

		updateOptionsButton = new EzButton("Update", updateoptions);
		groupViewMode = new EzGroup("Options", 
				start, 
				end, 
				step, 
				updateOptionsButton, 
				displayCapillaries, 
				displayCages);
		super.addEzComponent(groupViewMode);
		groupViewMode.setFoldedState(true);
		
		closeAll = new EzButton ("Close", runcloseall);
		groupClose = new EzGroup ("Close sequence", closeAll);
		super.addEzComponent(groupClose);
		groupClose.setFoldedState(true);
		
		displayCapillaries.addVarChangeListener(new EzVarListener<Boolean>() {
            @Override
            public void variableChanged(EzVar<Boolean> source, Boolean newValue) {
                roisDisplayLine(newValue, 1);
            }});
		displayCages.addVarChangeListener(new EzVarListener<Boolean>() {
            @Override
            public void variableChanged(EzVar<Boolean> source, Boolean newValue) {
                roisDisplayLine(newValue, 2);
            }});
	}
	
	@Override
	public void clean() {
	}

	@Override
	protected void execute() {		
	}
	
	private void openFile() {
		if (!sequenceOpenFile())
			return;
		UpdateItemsFromSequence (vSequence);
		
		if (loadCapillaries.getValue())
			loadDefaultCapillaries();
		if (loadCages.getValue())
			loadDefaultCages();
		startstopBufferingThread();
		groupLoadFiles.setFoldedState(true);
		groupViewMode.setFoldedState(false);
	}
	
	private void closeAll() {
		vSequence.close();
		groupLoadFiles.setFoldedState(false);
		groupViewMode.setFoldedState(true);
		groupClose.setFoldedState(true);
	}
	
	public void updateVisuals() {
		startstopBufferingThread();
		roisDisplayLine(displayCapillaries.getValue(), 1);
		roisDisplayLine(displayCages.getValue(), 2);
	}
	
	private void roisDisplayLine(boolean isVisible, int option) {
		if (vSequence == null)
			return;
		
		String ccs = "line";
		if (option == 2)
			ccs = "cage";
		ArrayList<Viewer>vList =  vSequence.getViewers();
		Viewer v = vList.get(0);
		IcyCanvas canvas = v.getCanvas();
		List<Layer> layers = canvas.getLayers(false);
		if (layers == null)
			return;
		for (Layer layer: layers) {
			ROI roi = layer.getAttachedROI();
			if (roi == null)
				continue;
			String cs = roi.getName();
			if (cs.contains(ccs))  
				layer.setVisible(isVisible);
		}
	}
	
	public void startstopBufferingThread() {
		if (vSequence == null)
			return;
		vSequence.vImageBufferThread_STOP();
		UpdateItemsToSequence(vSequence); 
		vSequence.vImageBufferThread_START(100); 
	}
	
	public boolean sequenceOpenFile() {
		if (vSequence != null)
			vSequence.close();		
		vSequence = new SequenceVirtual();
		
		String path = vSequence.loadInputVirtualStack(null);
		if (path != null) {
			XMLPreferences guiPrefs = getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			addSequence(vSequence);
			initSequenceParameters(vSequence);
		}
		return (path != null);
	}
	
	private void initSequenceParameters(SequenceVirtual seq) {
		seq.analysisStart = 0;
		seq.analysisEnd = seq.getSizeT()-1;
		seq.analysisStep = 1;
	}
	
	public void UpdateItemsFromSequence (SequenceVirtual vSequence) {
		if (vSequence == null)
			return;
		end.setValue((int) vSequence.analysisEnd);
		start.setValue((int) vSequence.analysisStart);
		step.setValue((int) vSequence.analysisStep);
	}
	
	private void UpdateItemsToSequence(SequenceVirtual vSequence) {
		if (vSequence == null)
			return;
		vSequence.analysisEnd = end.getValue();
		vSequence.analysisStart = start.getValue();
		vSequence.analysisStep = step.getValue();
	}

	private boolean loadDefaultCapillaries() {
		String path = vSequence.getDirectory();
		boolean flag = capillaryRoisOpen(path+File.separator+"capillarytrack.xml");
		return flag;
	}
	
	private boolean loadDefaultCages() {
		String path = vSequence.getDirectory();
		boolean flag = capillaryRoisOpen(path+File.separator+"drosotrack.xml");
		return flag;
	}
	
	public boolean capillaryRoisOpen(String csFileName) {
		boolean flag = false;
		if (csFileName == null)
			flag = vSequence.capillaries.xmlReadROIsAndData(vSequence);
		else
			flag = vSequence.capillaries.xmlReadROIsAndData(csFileName, vSequence);
		
		vSequence.analysisStart = vSequence.capillaries.analysisStart;
		vSequence.analysisEnd = vSequence.capillaries.analysisEnd;
		vSequence.analysisStep = vSequence.capillaries.analysisStep;
		return flag;
	}
}