package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.drosoTools.DrosoTools;
import plugins.fmp.drosoTools.EnumImageTransformOp;



public class DetectTab_Limits  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -680464530512061091L;
	
	public JComboBox<String> directionComboBox = new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	public JCheckBox	detectAllLevelCheckBox 	= new JCheckBox ("all", true);
	public JTextField 	detectTopTextField 		= new JTextField("35");
	public JComboBox<EnumImageTransformOp> transformForLevelsComboBox = new JComboBox<EnumImageTransformOp> (new EnumImageTransformOp[] {
			EnumImageTransformOp.R_RGB, EnumImageTransformOp.G_RGB, EnumImageTransformOp.B_RGB, 
			EnumImageTransformOp.R2MINUS_GB, EnumImageTransformOp.G2MINUS_RB, EnumImageTransformOp.B2MINUS_RG, EnumImageTransformOp.RGB,
			EnumImageTransformOp.GBMINUS_2R, EnumImageTransformOp.RBMINUS_2G, EnumImageTransformOp.RGMINUS_2B, 
			EnumImageTransformOp.H_HSB, EnumImageTransformOp.S_HSB, EnumImageTransformOp.B_HSB	});
	private JButton		displayTransform1Button	= new JButton("Display");
	private JTextField	spanTopTextField		= new JTextField("3");
	public JButton 		detectTopButton 		= new JButton("Detect");
	Capillarytrack parent0 = null;
	
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, detectTopTextField, transformForLevelsComboBox, displayTransform1Button )); 
		add( GuiUtil.besidesPanel(new JLabel("span ", SwingConstants.RIGHT), spanTopTextField, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( detectTopButton,  detectAllLevelCheckBox, new JLabel(" ")));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		transformForLevelsComboBox.addActionListener(this);
		detectTopButton.addActionListener(this);		
		displayTransform1Button.addActionListener(this);
	}
	
	public void enableItems(boolean enabled) {
		detectAllLevelCheckBox.setEnabled(enabled);
		detectTopButton.setEnabled(enabled);
		transformForLevelsComboBox.setEnabled(enabled);
		displayTransform1Button.setEnabled(enabled);
		directionComboBox.setEnabled(enabled);
		detectTopTextField.setEnabled(enabled);
		spanTopTextField.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == transformForLevelsComboBox)  {
			if (parent0.vSequence != null) {
				kymosDisplayFiltered1();
				firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
			}
		}
		else if (o == detectTopButton) {
			kymosDisplayFiltered1();
			CapBuildDetect_Limits detect = new CapBuildDetect_Limits();
			detect.detectCapillaryLevels(parent0);
			firePropertyChange("KYMO_DETECT_TOP", false, true);
		}
		else if (o== displayTransform1Button) {
			kymosDisplayFiltered1();
			firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
		}
	}
	
	// -------------------------------------------------
	
	public double getDetectLevelThreshold() {
		double detectLevelThreshold = 0;
		try { detectLevelThreshold =  Double.parseDouble( detectTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }
		return detectLevelThreshold;
	}

	public void setDetectLevelThreshold (double threshold) {
		detectTopTextField.setText(Double.toString(threshold));
	}
	
	public int getSpanDiffTop() {
		int spanDiffTop = 0;
		try { spanDiffTop = Integer.parseInt( spanTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
		return spanDiffTop;
	}
		
	public void kymosDisplayFiltered1() {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new DrosoTools.SequenceNameComparator()); 
		EnumImageTransformOp transform= (EnumImageTransformOp) transformForLevelsComboBox.getSelectedItem();
		parent0.detectPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
}
