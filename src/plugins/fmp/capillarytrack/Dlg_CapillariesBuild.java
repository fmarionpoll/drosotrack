package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;


public class Dlg_CapillariesBuild extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2809225190576447425L;
	private JButton 	createROIsFromPolygonButton2 = new JButton("Generate ROIs (from Polygon 2D)");
	public JRadioButton selectGroupedby2Button = new JRadioButton("grouped by 2");
	public JRadioButton selectRegularButton 	= new JRadioButton("evenly spaced");
	private ButtonGroup buttonGroup2 			= new ButtonGroup();
	public JTextField 	nbcapillariesTextField 	= new JTextField("20");
	public JTextField 	width_between_capillariesTextField = new JTextField("30");
	public JTextField 	width_intervalTextField = new JTextField("53");
	

	public void init(GridLayout capLayout) {
		JComponent roiPanel = new JPanel(false);
		roiPanel.setLayout(capLayout);
		
		roiPanel.add( GuiUtil.besidesPanel( createROIsFromPolygonButton2));
		buttonGroup2.add(selectGroupedby2Button);
		buttonGroup2.add(selectRegularButton);
		selectGroupedby2Button.setSelected(true);
		roiPanel.add( GuiUtil.besidesPanel( new JLabel ("N capillaries ", SwingConstants.RIGHT),  nbcapillariesTextField, selectRegularButton, selectGroupedby2Button)); 
		roiPanel.add( GuiUtil.besidesPanel( new JLabel("Pixels btw. caps ", SwingConstants.RIGHT), width_between_capillariesTextField, new JLabel("btw. groups ", SwingConstants.RIGHT), width_intervalTextField ) );
		add(roiPanel);
	}
	
	public void enable(boolean enabled) {
		createROIsFromPolygonButton2.setEnabled(enabled);
		selectGroupedby2Button.setEnabled(enabled);
		selectRegularButton.setEnabled(enabled);
		nbcapillariesTextField.setEnabled(enabled);
		selectRegularButton.setEnabled(enabled);
		selectGroupedby2Button .setEnabled(enabled);
		width_between_capillariesTextField.setEnabled(enabled );
		width_intervalTextField.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}
