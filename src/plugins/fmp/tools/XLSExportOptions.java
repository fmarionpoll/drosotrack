package plugins.fmp.tools;

import plugins.fmp.sequencevirtual.ExperimentList;

public class XLSExportOptions {
	
	public boolean xyCenter 		= true;
	public boolean distance 		= false;
	public boolean alive 			= true;
	
	public boolean topLevelDelta	= true;
	public boolean topLevel 		= true; 
	public boolean bottomLevel 		= false; 
	public boolean derivative 		= false; 
	public boolean consumption 		= false; 
	public boolean sum 				= true;
	public boolean t0				= true;
	public boolean onlyalive		= true;

	public boolean transpose 		= false;
	public boolean duplicateSeries 	= true;
	public boolean pivot 			= false;
	public boolean exportAllFiles 	= true;
	public boolean absoluteTime		= false;
	
	public ExperimentList experimentList = new ExperimentList ();
}
