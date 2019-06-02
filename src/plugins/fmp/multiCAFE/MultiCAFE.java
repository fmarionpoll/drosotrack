package plugins.fmp.multiCAFE;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.swimmingPool.SwimmingObject;
import plugins.adufour.ezplug.*;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class MultiCAFE extends EzPlug implements EzStoppable {
	SequenceVirtual vSequence = null;
	ArrayList <SequencePlus> kymographArrayList	= new ArrayList <SequencePlus> ();	// list of kymograph sequences

	boolean stopFlag = false;
	EzGroup groupLoadFiles;
	EzVarFile varFile;
	EzVarBoolean loadCapillaries;
	EzVarBoolean loadCages;
	EzVarBoolean loadKymographs;
	EzVarBoolean loadMeasures;
	EzGroup groupViewMode;
	EzVarInteger start;
	EzVarInteger end;
	EzVarInteger step;
	EzButton closeAll;
	
	ActionListener runcloseall = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			new AnnounceFrame("close all!");
		}
	};
	
	
	@Override
	protected void initialize() {
		
		varFile = new EzVarFile("select a file", null);
		loadCapillaries = new EzVarBoolean("load capillaries", true);
		loadCages = new EzVarBoolean("load cages", false);
		loadKymographs = new EzVarBoolean("load kymographs", true);
		loadMeasures = new EzVarBoolean("load measures", true);
		groupLoadFiles = new EzGroup("Load file", varFile, loadCapillaries, loadCages, loadKymographs, loadMeasures);
		super.addEzComponent(groupLoadFiles);
		
		start = new EzVarInteger("start", 0, 9999999, 1);
		end = new EzVarInteger("end", 0, 9999999, 1);
		step = new EzVarInteger("step", 0, 9999999, 1);
		groupViewMode = new EzGroup("View mode", start, end, step);
		super.addEzComponent(groupViewMode);
		
		closeAll = new EzButton ("close all files", runcloseall);
		super.addEzComponent(closeAll);
	}
	
	
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void execute() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void stopExecution()
	{
		// this method is from the EzStoppable interface
		// if this interface is implemented, a "stop" button is displayed
		// and this method is called when the user hits the "stop" button
		stopFlag = true;
	}

}
