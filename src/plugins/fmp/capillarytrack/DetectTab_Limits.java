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
import plugins.fmp.sequencevirtual.Tools;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class DetectTab_Limits  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -680464530512061091L;
	
	public JComboBox<String> directionComboBox = new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	public JCheckBox	detectAllLevelCheckBox 	= new JCheckBox ("all", true);
	public JTextField 	detectTopTextField 		= new JTextField("35");
	public JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
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
			Detect_Limits detect = new Detect_Limits();
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
		Collections.sort(parent0.kymographArrayList, new Tools.SequenceNameComparator()); 
		TransformOp transform;
		transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		parent0.detectPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
}
