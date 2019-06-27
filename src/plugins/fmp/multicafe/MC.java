package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequenceVirtual;


public class MC extends JPanel implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5739112045358747277L;
	
	private JTextField 	startFrameTextField		= new JTextField("0");
	JTextField 	endFrameTextField		= new JTextField("99999999");
	private JTextField 	analyzeStepTextField 	= new JTextField("1");
	private JButton 	updateButton 			= new JButton("Update");
	
	private JButton  	previousButton		 	= new JButton("<");
	private JButton		nextButton				= new JButton(">");
	JComboBox<String> 	experimentComboBox		= new JComboBox<String>();
	
	boolean disableChangeFile = false;
	private Multicafe parent0 = null;
	
	void init(GridLayout capLayout, Multicafe parent0) {
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
		
		experimentComboBox.addItemListener(new ItemListener() {
	        public void itemStateChanged(ItemEvent arg0) {
	        	if (arg0.getStateChange() == ItemEvent.DESELECTED) {
	        		firePropertyChange("SEQ_SAVEMEAS", false, true);
	        	}
	        	else if (arg0.getStateChange () == ItemEvent.SELECTED) {
//	        		System.out.println("combo - item selected");
	        		updateBrowseInterface();
	        	}
	        }
	    });
	}
	
	void updateBrowseInterface() {
		int isel = experimentComboBox.getSelectedIndex();
		boolean flag1 = (isel == 0? false: true);
		boolean flag2 = (isel == (experimentComboBox.getItemCount() -1)? false: true);
		previousButton.setEnabled(flag1);
		nextButton.setEnabled(flag2);
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
			if ( experimentComboBox.getSelectedIndex() < (experimentComboBox.getItemCount() -1)) {
				experimentComboBox.setSelectedIndex(experimentComboBox.getSelectedIndex()+1);
			}
			
		}
		else if ( o == previousButton) {
			if (experimentComboBox.getSelectedIndex() > 0) {
				experimentComboBox.setSelectedIndex(experimentComboBox.getSelectedIndex()-1);
			}
		}
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
