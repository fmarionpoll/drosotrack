package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;


public class MoveTab_Detect extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private Multicafe parent0;
	
	private JButton startComputationButton 	= new JButton("Start");
	private JButton stopComputationButton	= new JButton("Stop");
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		add( GuiUtil.besidesPanel( startComputationButton, stopComputationButton ) );
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
	}
	
	public void enableItems(boolean enabled) {
//		createROIsFromPolygonButton2.setEnabled(enabled);
//		selectGroupedby2Button.setEnabled(enabled);
//		selectRegularButton.setEnabled(enabled);
//		nbcapillariesTextField.setEnabled(enabled);
//		selectRegularButton.setEnabled(enabled);
//		selectGroupedby2Button .setEnabled(enabled);
//		width_between_capillariesTextField.setEnabled(enabled );
//		width_intervalTextField.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
//		Object o = e.getSource();
//		if ( o == createROIsFromPolygonButton2)  {
//			roisGenerateFromPolygon();
//			parent0.vSequence.keepOnly2DLines_CapillariesArrayList();
//			firePropertyChange("CAPILLARIES_NEW", false, true);	
//		}
//		else if ( o == selectRegularButton) {
//			boolean status = false;
//			width_between_capillariesTextField.setEnabled(status);
//			width_intervalTextField.setEnabled(status);	
//		}
	}
	

}