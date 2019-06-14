package plugins.fmp.tools;


public enum ThresholdType { 
	SINGLE ("simple threshold"), COLORARRAY ("Color array"), NONE("undefined");
	
	private String label;
	ThresholdType (String label) { 
		this.label = label;}
	public String toString() { 
		return label;}	
	public static ThresholdType findByText(String abbr){
	    for(ThresholdType v : values()){ 
	    	if( v.toString().equals(abbr)) { 
	    		return v; }  }
	    return null;
	}
}
