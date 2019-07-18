package plugins.fmp.tools;


public enum EnumXLSExperimentDescriptors {
	DATE( "date"), STIML ("stimL"), CONCL ("concL"), STIMR ("stimR"), CONCR ("concR"), CAM ("cam"), CAP ("cap"),
	CAGE("cage"), TIME("time"), NFLIES("nflies"), DUM1("dum1"), DUM2("dum2"), DUM3 ("dum3"), DUM4("dum4");
	
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

