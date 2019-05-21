package plugins.fmp.capillarytrack;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;

public class CapillariesTab_File extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4019075448319252245L;
	
	private JButton		openButtonCapillaries	= new JButton("Load...");
	private JButton		saveButtonCapillaries	= new JButton("Save...");
	private Capillarytrack parent0 = null;
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);
		
		JLabel loadsaveText1 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonCapillaries, saveButtonCapillaries));
		
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openButtonCapillaries.addActionListener(this); 
		saveButtonCapillaries.addActionListener(this);	
	}
	
	public void enableItems(boolean enabled) {
//		openButtonCapillaries.setEnabled(enabled);
		saveButtonCapillaries.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openButtonCapillaries)  {
			firePropertyChange("CAP_ROIS_OPEN", false, true);	
		}
		else if ( o == saveButtonCapillaries) {
			firePropertyChange("CAP_ROIS_SAVE", false, true);	
		}		
	}

	// ----------------------------------
	public boolean capillaryRoisOpen(String csFileName) {
		
		parent0.vSequence.removeAllROI();
		boolean flag = false;
		if (csFileName == null)
			flag = parent0.vSequence.xmlReadROIsAndData();
		else
			flag = parent0.vSequence.xmlReadROIsAndData(csFileName);
		return flag;
	}
	
	public boolean capillaryRoisSave() {
		return parent0.vSequence.xmlWriteROIsAndData("capillarytrack.xml");
	}

}
