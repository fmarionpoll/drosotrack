package plugins.fmp.sequencevirtual;


import java.util.ArrayList;

public class ExperimentList {
	
	public ArrayList<Experiment> experimentList = null;
	
	public ExperimentList () {
		experimentList = new ArrayList<Experiment> ();
	}
	
	public Experiment getStartAndEndFromAllExperiments() {
		
		System.out.println("get start and end of each stack");
		
		Experiment expglobal = new Experiment();
		Experiment exp0 = experimentList.get(0);
		expglobal.fileTimeImageFirst = exp0.fileTimeImageFirst;
		expglobal.fileTimeImageLast = exp0.fileTimeImageLast;
		for (Experiment exp: experimentList) 
		{
			if (expglobal.fileTimeImageFirst.compareTo(exp.fileTimeImageFirst) > 0) 
				expglobal.fileTimeImageFirst = exp.fileTimeImageFirst;
			if (expglobal.fileTimeImageLast .compareTo(exp.fileTimeImageLast) <0)
				expglobal.fileTimeImageLast = exp.fileTimeImageLast;
			if (exp.vSequence.analysisEnd > exp.vSequence.getSizeT()-1)
				exp.vSequence.analysisEnd = exp.vSequence.getSizeT()-1;
		}
		
		expglobal.fileTimeImageFirstMinutes = expglobal.fileTimeImageFirst.toMillis()/60000;
		expglobal.fileTimeImageLastMinutes = expglobal.fileTimeImageLast.toMillis()/60000;
		
		return expglobal;
	}
	
	public boolean readInfosFromAllExperiments() {

		boolean flag = true;
		for (Experiment exp: experimentList) 
		{
			System.out.println("read expt "+ exp.filename);
			boolean ok = exp.openSequenceAndMeasures();
			flag &= ok;
		}
		return flag;
	}
}
