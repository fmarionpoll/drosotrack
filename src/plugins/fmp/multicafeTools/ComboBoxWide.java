package plugins.fmp.toolsMulticafe;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

//got this workaround from the following bug: 
//http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607 

public class ComboBoxWide extends JComboBox<String>{ 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ComboBoxWide() { 
	} 
	
	public ComboBoxWide(final Object items[]){ 
		super(); 
	} 
	
	public ComboBoxWide(Vector<?> items) { 
		super(); 
	} 
	
	public ComboBoxWide(ComboBoxModel<?> aModel) { 
		super(); 
	} 
	
	private boolean layingOut = false; 
	
	public void doLayout(){ 
		try{ 
		    layingOut = true; 
		        super.doLayout(); 
		}finally{ 
		    layingOut = false; 
		} 
	} 
	
	public Dimension getSize(){ 
		Dimension dim = super.getSize(); 
		if(!layingOut) 
		    dim.width = Math.max(dim.width, getPreferredSize().width); 
		return dim; 
	} 
}
