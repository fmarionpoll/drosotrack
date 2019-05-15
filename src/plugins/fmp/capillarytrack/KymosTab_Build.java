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
import plugins.fmp.sequencevirtual.SequenceVirtual;


public class KymosTab_Build extends JPanel implements ActionListener { 

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
	
	Capillarytrack parent0;
		
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);	
		add(GuiUtil.besidesPanel(kymoStartComputationButton, kymosStopComputationButton));
		add(GuiUtil.besidesPanel(new JLabel("area around ROIs", SwingConstants.RIGHT), diskRadiusTextField, new JLabel (" "), new JLabel (" ")));
		defineActionListeners();
		this.parent0 = parent0;
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
	
	public void UpdateItemsFromSequence (SequenceVirtual vSequence) {
//		endFrameTextField.setText(Integer.toString((int) vSequence.analysisEnd));
//		startFrameTextField.setText(Integer.toString((int) vSequence.analysisStart));
//		analyzeStepTextField.setText(Integer.toString(vSequence.analyzeStep));
	}
	
	public void UpdateItemsToSequence (SequenceVirtual vSequence) {
//		vSequence.analysisStart = Integer.parseInt( startFrameTextField.getText() );
//		vSequence.analysisEnd 	= Integer.parseInt( endFrameTextField.getText());
//		vSequence.analyzeStep   = Integer.parseInt(analyzeStepTextField.getText());
	}
}
