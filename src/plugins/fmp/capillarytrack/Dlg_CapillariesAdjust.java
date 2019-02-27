package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;

public class Dlg_CapillariesAdjust extends JPanel implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1756354919434057560L;
	public JTextField	jitterTextField2		= new JTextField("10");
	private JButton 	adjustButton 			= new JButton("Center lines");
	public JCheckBox	refBarCheckBox			= new JCheckBox("display bars", false);
	

	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		add( GuiUtil.besidesPanel(adjustButton, refBarCheckBox,  new JLabel("jitter ", SwingConstants.RIGHT), jitterTextField2));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		adjustButton.addActionListener(this);
		refBarCheckBox.addActionListener(this);
	}
	
	public void enableItems(boolean enabled) {
		jitterTextField2.setEnabled(enabled);
		adjustButton.setEnabled(enabled);
		refBarCheckBox.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == adjustButton)  {
			firePropertyChange("ADJUST_ROILINES", false, true);	
		}
		else if (o == refBarCheckBox) {
			firePropertyChange("ADJUST_DISPLAY", false, true);	
		}
	}

}
