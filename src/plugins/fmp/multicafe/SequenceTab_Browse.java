package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;


public class SequenceTab_Browse extends JPanel implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	
	public JTextField 	startFrameTextField		= new JTextField("0");
	public JTextField 	endFrameTextField		= new JTextField("99999999");
	public JTextField 	analyzeStepTextField 	= new JTextField("1");
	private JButton 	updateButton 			= new JButton("Update");
	
	public JButton  	previousButton		 	= new JButton("<");
	public JButton		nextButton				= new JButton(">");
	public JComboBox<String> experimentComboBox	= new JComboBox<String>();
	
	public boolean disableChangeFile = false;
	private Multicafe parent0 = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
	
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(experimentComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel( k2Panel));
		
		add(GuiUtil.besidesPanel( 
				new JLabel("start ", SwingConstants.RIGHT), startFrameTextField, 
				new JLabel("step ", SwingConstants.RIGHT) , analyzeStepTextField 				
				));
		add(GuiUtil.besidesPanel( 
				new JLabel("end ", SwingConstants.RIGHT), endFrameTextField, 
				new JLabel(" "), updateButton ));

		updateButton.addActionListener(this);
		experimentComboBox.addActionListener(this);
		nextButton.addActionListener(this);
		previousButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == updateButton) {
			firePropertyChange("UPDATE", false, true);
		}
		else if ( o == experimentComboBox) {
			if (experimentComboBox.getItemCount() == 0 || parent0.vSequence == null || disableChangeFile)
				return;
			String newtext = (String) experimentComboBox.getSelectedItem();
			String oldtext = parent0.vSequence.getFileName();
			if (!newtext.equals(oldtext)) {
				firePropertyChange("SEQ_OPEN", false, true);
			}
		}
		else if ( o == nextButton) {
			int isel = experimentComboBox.getSelectedIndex();
			if (isel < (experimentComboBox.getItemCount() -1))
				experimentComboBox.setSelectedIndex(isel+1);
		}
		else if ( o == previousButton) {
			int isel = experimentComboBox.getSelectedIndex();
			if (isel > 0)
				experimentComboBox.setSelectedIndex(experimentComboBox.getSelectedIndex()-1);
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
