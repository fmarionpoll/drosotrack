package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.capillarytrack.Capillarytrack.StatusComputation;


public class Dlg_KymosBuild extends JPanel implements ActionListener { 

	/**
	 * 
	 */
	private static final long serialVersionUID = 1771360416354320887L;
	public JButton 	kymoStartComputationButton 	= new JButton("Start");
	public JButton  kymosStopComputationButton 	= new JButton("Stop");
	public JTextField 	startFrameTextField		= new JTextField("0");
	public JTextField 	endFrameTextField		= new JTextField("99999999");
	public JTextField 	analyzeStepTextField 	= new JTextField("1");
	public JTextField 	diskRadiusTextField 	= new JTextField("5");
	public StatusComputation sComputation = StatusComputation.START_COMPUTATION; 
		
	public void init(GridLayout capLayout) {
		setLayout(capLayout);	
		add(GuiUtil.besidesPanel(kymoStartComputationButton, kymosStopComputationButton));
		add(GuiUtil.besidesPanel( new JLabel("start ", SwingConstants.RIGHT), startFrameTextField, new JLabel("end ", SwingConstants.RIGHT), endFrameTextField) );	
		add(GuiUtil.besidesPanel( new JLabel("step ", SwingConstants.RIGHT) , analyzeStepTextField, new JLabel("area ", SwingConstants.RIGHT), diskRadiusTextField));
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		kymoStartComputationButton.addActionListener(this);
		kymosStopComputationButton.addActionListener(this);	

	}
	
	public void enableItems(boolean enabled) {
		kymoStartComputationButton.setEnabled(enabled && (sComputation == StatusComputation.START_COMPUTATION));
		kymosStopComputationButton.setEnabled (enabled && (sComputation == StatusComputation.STOP_COMPUTATION));
		
		startFrameTextField.setEnabled(enabled);
		analyzeStepTextField.setEnabled(enabled );
		endFrameTextField.setEnabled(enabled);
		diskRadiusTextField.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == kymoStartComputationButton)  {
			firePropertyChange("KYMOS_BUILD_START", false, true);	
		}
		else if ( o == kymosStopComputationButton) {
			firePropertyChange("KYMOS_BUILD_STOP", false, true);
		}
	}
}
