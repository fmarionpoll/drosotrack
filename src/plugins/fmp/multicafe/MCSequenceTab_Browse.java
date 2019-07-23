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
import plugins.fmp.sequencevirtual.SequenceVirtual;


public class MCSequenceTab_Browse extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	
	private JTextField 	startFrameTextField		= new JTextField("0");
	JTextField 			endFrameTextField		= new JTextField("99999999");
	private JTextField 	analyzeStepTextField 	= new JTextField("1");
	private JButton 	updateButton 			= new JButton("Update");
	
	
	void init(GridLayout capLayout) {
		setLayout(capLayout);
			
		add(GuiUtil.besidesPanel( 
				new JLabel("start ", SwingConstants.RIGHT), startFrameTextField, 
				new JLabel("step ", SwingConstants.RIGHT) , analyzeStepTextField 				
				));
		add(GuiUtil.besidesPanel( 
				new JLabel("end ", SwingConstants.RIGHT), endFrameTextField, 
				new JLabel(" "), updateButton ));

		updateButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
			firePropertyChange("UPDATE", false, true);
		} } );

	}
		
	void setBrowseItems (SequenceVirtual seq) {
		endFrameTextField.setText(Integer.toString((int) seq.analysisEnd));
		startFrameTextField.setText(Integer.toString((int) seq.analysisStart));
		analyzeStepTextField.setText(Integer.toString(seq.analysisStep));
	}
	
	void getBrowseItems (SequenceVirtual seq) {
		seq.analysisStart 	= Integer.parseInt( startFrameTextField.getText() );
		seq.analysisEnd 	= Integer.parseInt( endFrameTextField.getText());
		seq.analysisStep 	= Integer.parseInt( analyzeStepTextField.getText() );
	}

}
