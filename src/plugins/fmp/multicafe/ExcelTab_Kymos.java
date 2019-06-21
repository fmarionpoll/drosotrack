package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


public class ExcelTab_Kymos extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox 	topLevelCheckBox 	= new JCheckBox("top", true);
	public JCheckBox 	topLevelDCheckBox 	= new JCheckBox("top delta", true);
	
	public JCheckBox 	bottomLevelCheckBox = new JCheckBox("bottom", false);
	public JCheckBox 	consumptionCheckBox = new JCheckBox("gulps", false);
	public JCheckBox 	sumCheckBox 		= new JCheckBox("L+R", true);
	public JCheckBox 	derivativeCheckBox  = new JCheckBox("derivative", false);
	public JCheckBox	t0CheckBox			= new JCheckBox("t-t0", true);
	public JCheckBox	onlyaliveCheckBox   = new JCheckBox("dead=empty");

	
	
	public void init(GridLayout capLayout) {	
		setLayout(capLayout);
		add(GuiUtil.besidesPanel( topLevelCheckBox, topLevelDCheckBox, bottomLevelCheckBox, consumptionCheckBox));
		add(GuiUtil.besidesPanel( t0CheckBox, sumCheckBox, new JLabel(" "), new JLabel(" "))); 
		add(GuiUtil.besidesPanel( onlyaliveCheckBox, new JLabel(" "), new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			firePropertyChange("EXPORT_KYMOSDATA", false, true);
		}
	}

}
