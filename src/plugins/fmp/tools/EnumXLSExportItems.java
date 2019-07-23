package plugins.fmp.tools;

public enum EnumXLSExportItems {
	TOPLEVEL ("toplevel"), 
	BOTTOMLEVEL ("bottomlevel"), 
	DERIVEDVALUES ("derivative"), 
	SUMGULPS ("sumGulps"), 
	SUMGULPSLR ("sumGulpsL+R"), 
	SUMLR ("sumL+R"), 
	XYCENTER ("xycenter"), 
	DISTANCE ("distance"), 
	ISALIVE ("_alive"), 
	TOPLEVELDELTA ("topdelta"),
	TOPLEVELDELTALR ("topdeltaL+R");
	
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
