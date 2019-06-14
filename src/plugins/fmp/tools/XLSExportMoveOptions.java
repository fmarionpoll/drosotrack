package plugins.fmp.tools;

import java.util.ArrayList;

public class XLSExportMoveOptions {

	public boolean xyCenter 		= true;
	public boolean distance 		= false;
	public boolean alive 			= true;
	public boolean transpose 		= false;
	public boolean duplicateSeries 	= true;
	public boolean pivot 			= false;
	public boolean exportAllFiles 	= true;
	
	public ArrayList<Experiment> experimentList = new ArrayList<Experiment> ();
}
