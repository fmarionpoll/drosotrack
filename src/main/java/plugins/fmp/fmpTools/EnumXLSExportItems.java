package plugins.fmp.fmpTools;

public enum EnumXLSExportItems {
	TOPLEVEL ("toplevel"), 
	BOTTOMLEVEL ("bottomlevel"), 
	DERIVEDVALUES ("derivative"), 
	SUMGULPS ("sumGulps"), 
	SUMGULPS_LR ("sumGulps_L+R"), 
	TOPLEVEL_LR ("toplevel_L+R"), 
	XYCENTER ("xycenter"), 
	DISTANCE ("distance"), 
	ISALIVE ("_alive"), 
	TOPLEVELDELTA ("topdelta"),
	TOPLEVELDELTA_LR ("topdelta_L+R");
	
	private String label;
	
	EnumXLSExportItems (String label) { 
		this.label = label;
	}
	
	public String toString() { 
		return label;
	}
	
	public static EnumXLSExportItems findByText(String abbr){
	    for(EnumXLSExportItems v : values()) { 
	    	if( v.toString().equals(abbr)) { 
	    		return v; 
    		}  
    	}
	    return null;
	}
}
