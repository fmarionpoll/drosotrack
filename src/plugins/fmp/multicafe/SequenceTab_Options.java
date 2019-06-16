package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;


public class SequenceTab_Options extends JPanel implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	
	public JTextField 	startFrameTextField		= new JTextField("0");
	public JTextField 	endFrameTextField		= new JTextField("99999999");
	public JTextField 	analyzeStepTextField 	= new JTextField("1");
	private JButton 	updateButton 			= new JButton("Update");
	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
	
		add(GuiUtil.besidesPanel( 
				new JLabel("start ", SwingConstants.RIGHT), startFrameTextField, 
				new JLabel("step ", SwingConstants.RIGHT) , analyzeStepTextField 				
				));
		add(GuiUtil.besidesPanel( 
				new JLabel("end ", SwingConstants.RIGHT), endFrameTextField, 
				new JLabel(" "), updateButton ));

		updateButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == updateButton) {
			firePropertyChange("UPDATE", false, true);
		}
	}

	public void UpdateItemsFromSequence (Multicafe parent0) {
		endFrameTextField.setText(Integer.toString((int) parent0.vSequence.analysisEnd));
		startFrameTextField.setText(Integer.toString((int) parent0.vSequence.analysisStart));
		analyzeStepTextField.setText(Integer.toString(parent0.vSequence.analysisStep));
	}
	
	public void UpdateItemsToSequence (Multicafe parent0) {
		parent0.vSequence.analysisStart = Integer.parseInt( startFrameTextField.getText() );
		parent0.vSequence.analysisEnd 	= Integer.parseInt( endFrameTextField.getText());
		parent0.vSequence.analysisStep 	= Integer.parseInt( analyzeStepTextField.getText() );
		parent0.vSequence.cleanUpBufferAndRestart();
	}

}
