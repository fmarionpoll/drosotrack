package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


public class ExcelTab_Move  extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JCheckBox 	xyCenterCheckBox 	= new JCheckBox("XY position", true);
	public JCheckBox 	distanceCheckBox 	= new JCheckBox("distance", false);
	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	
	public void init(GridLayout capLayout) {	
		setLayout(capLayout);
		add(GuiUtil.besidesPanel( xyCenterCheckBox, distanceCheckBox, new JLabel(" "), new JLabel(" ")));
		add(GuiUtil.besidesPanel( new JLabel(" "), new JLabel(" "), new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			firePropertyChange("EXPORT_MOVEDATA", false, true);
		}
	}
	
}
