package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
	private JRadioButton selectGroupedby2Button = new JRadioButton("grouped by 2");
	private JRadioButton selectRegularButton 	= new JRadioButton("evenly spaced");
	private ButtonGroup buttonGroup2 			= new ButtonGroup();
	private JTextField 	nbcapillariesTextField 	= new JTextField("20");
	private JTextField 	width_between_capillariesTextField = new JTextField("30");
	private JTextField 	width_intervalTextField = new JTextField("53");
	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel( createROIsFromPolygonButton2));
		buttonGroup2.add(selectGroupedby2Button);
		buttonGroup2.add(selectRegularButton);
		selectGroupedby2Button.setSelected(true);
		add( GuiUtil.besidesPanel( new JLabel ("N capillaries ", SwingConstants.RIGHT),  nbcapillariesTextField, selectRegularButton, selectGroupedby2Button)); 
		add( GuiUtil.besidesPanel( new JLabel("Pixels btw. caps ", SwingConstants.RIGHT), width_between_capillariesTextField, new JLabel("btw. groups ", SwingConstants.RIGHT), width_intervalTextField ) );
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
		createROIsFromPolygonButton2.addActionListener(this);
		selectRegularButton.addActionListener(this);
	}
	
	public void enableItems(boolean enabled) {
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
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == createROIsFromPolygonButton2)  {
			firePropertyChange("CREATE_ROILINES", false, true);	
		}
		else if ( o == selectRegularButton) {
			boolean status = false;
			width_between_capillariesTextField.setEnabled(status);
			width_intervalTextField.setEnabled(status);	
		}
	}
	
	// set/ get	
	public void setNbCapillaries(int nrois) {
		nbcapillariesTextField.setText(Integer.toString(nrois));
	}
	public int getNbCapillaries( ) {
		return Integer.parseInt( nbcapillariesTextField.getText() );
	}

	public int getWidthSmallInterval ( ) {
		return Integer.parseInt( width_between_capillariesTextField.getText() );
	}
	
	public int getWidthLongInterval() {
		return Integer.parseInt( width_intervalTextField.getText() );
	}
	
	public boolean getGroupedBy2() {
		return selectGroupedby2Button.isSelected();
	}
	
	public void setGroupedBy2(boolean flag) {
		selectGroupedby2Button.setSelected(flag);
		selectRegularButton.setSelected(!flag);
	}
}
