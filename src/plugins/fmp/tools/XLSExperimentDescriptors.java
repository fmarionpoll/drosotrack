package plugins.fmp.tools;


public enum XLSExperimentDescriptors {
	DATE( "date"),	STIM ("stim"),	CONC ("conc"),	CAM ("cam"),	CAP ("cap"),
	CAGE("cage"), TIME("time"), NFLIES("nflies"), DUM1("dum1"), DUM2("dum2"), DUM3 ("dum3"), DUM4("dum4");
	
	private String label;
	XLSExperimentDescriptors (String label) { 
		this.label = label; }
	public String toString() { 
		return label;}	
	public static XLSExperimentDescriptors findByText(String abbr){
	    for(XLSExperimentDescriptors v : values()) { 
	    	if( v.toString().equals(abbr)) { 
	    		return v; 
	    	}  
	    }
	    return null;
	}
}

