package plugins.fmp.fmpTools;


public enum EnumXLSExperimentDescriptors {
	DATE( "date"), 
	BOXID("box_ID"), EXPMT("expmt"), COMMENT("comment"),
	STIM("stim"), CONC("conc"), CAM ("cam"), CAP ("cap"),
	CAGE("cage"), NFLIES("nflies"), 
	DUM1("dum1"), DUM2("dum2"), DUM3 ("dum3"), DUM4("dum4");
	
	private String label;
	EnumXLSExperimentDescriptors (String label) { 
		this.label = label; }
	public String toString() { 
		return label;}	
	public static EnumXLSExperimentDescriptors findByText(String abbr){
	    for(EnumXLSExperimentDescriptors v : values()) { 
	    	if( v.toString().equals(abbr)) { 
	    		return v; 
	    	}  
	    }
	    return null;
	}
}

