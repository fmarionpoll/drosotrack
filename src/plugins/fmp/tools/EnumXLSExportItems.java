package plugins.fmp.tools;

public enum XLSExportItems {
	TOPLEVEL ("toplevel"), 
	BOTTOMLEVEL ("bottomlevel"), 
	DERIVEDVALUES ("derivative"), 
	SUMGULPS ("sumGulps"), 
	SUMLR ("sumL+R"), 
	XYCENTER ("xycenter"), 
	DISTANCE ("distance"), 
	ISALIVE ("_alive"), 
	TOPLEVELDELTA ("topdelta"),
	TOPLEVELDELTALR ("topdeltaL+R");
	
	private String label;
	
	XLSExportItems (String label) { 
		this.label = label;
	}
	
	public String toString() { 
		return label;
	}
	
	public static XLSExportItems findByText(String abbr){
	    for(XLSExportItems v : values()) { 
	    	if( v.toString().equals(abbr)) { 
	    		return v; 
    		}  
    	}
	    return null;
	}
}
