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

public class Dlg_KymosLoadSave  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3973928400949966679L;
	private JButton		openButtonKymos	= new JButton("Load...");
	private JButton		saveButtonKymos	= new JButton("Save...");
	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		
		JLabel loadsaveText1 = new JLabel ("-> File (tiff) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonKymos, saveButtonKymos));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openButtonKymos.addActionListener(this);
		saveButtonKymos.addActionListener (this);
	}
	
	public void enableItems(boolean enabled) {
		openButtonKymos.setEnabled(enabled);
		saveButtonKymos.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openButtonKymos)  {
			firePropertyChange("KYMOS_OPEN", false, true);	
		}
		else if ( o == saveButtonKymos) {
			firePropertyChange("KYMOS_SAVE", false, true);	
		}		
	}

}
