package plugins.fmp.tools;

import java.util.ArrayList;

import plugins.fmp.multicafe.Experiment;

public class XLSExportCapillariesOptions {
	public boolean topLevel = true; 
	public boolean bottomLevel = false; 
	public boolean derivative = false; 
	public boolean consumption = false; 
	public boolean sum = true; 
	public boolean transpose = false; 
	public boolean t0 = true;
	public boolean onlyalive = false;
	public boolean pivot = false;
	public boolean exportAllFiles = true;
	
	public ArrayList<Experiment> experimentList = new ArrayList<Experiment> ();

}
