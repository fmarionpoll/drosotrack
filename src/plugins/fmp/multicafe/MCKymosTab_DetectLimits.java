package plugins.fmp.multicafe;

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
import plugins.fmp.tools.Tools;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ImageTransformTools.TransformOp;

public class MCKymosTab_DetectLimits  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6329863521455897561L;
	
	JComboBox<String> 	directionComboBox 		= new JComboBox<String> (new String[] {" threshold >", " threshold <" });
	JCheckBox			detectAllLevelCheckBox 	= new JCheckBox ("all", true);
	private JTextField 	detectTopTextField 		= new JTextField("35");
	JComboBox<TransformOp> transformForLevelsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.R_RGB, TransformOp.G_RGB, TransformOp.B_RGB, 
			TransformOp.R2MINUS_GB, TransformOp.G2MINUS_RB, TransformOp.B2MINUS_RG, TransformOp.RGB,
			TransformOp.GBMINUS_2R, TransformOp.RBMINUS_2G, TransformOp.RGMINUS_2B, 
			TransformOp.H_HSB, TransformOp.S_HSB, TransformOp.B_HSB	});
	
	private JButton		displayTransform1Button	= new JButton("Display");
	private JTextField	spanTopTextField		= new JTextField("3");
	private JButton 	detectTopButton 		= new JButton("Detect");
	Multicafe 			parent0 				= null;
	
	
	void init(GridLayout capLayout, Multicafe parent0) {
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
			MCBuildDetect_Limits detect = new MCBuildDetect_Limits();
			detect.detectCapillaryLevels(parent0);
			firePropertyChange("KYMO_DETECT_TOP", false, true);
		}
		else if (o== displayTransform1Button) {
			kymosDisplayFiltered1();
			firePropertyChange("KYMO_DISPLAY_FILTERED1", false, true);
		}
	}
	
	// -------------------------------------------------
	
	double getDetectLevelThreshold() {
		double detectLevelThreshold = 0;
		try { detectLevelThreshold =  Double.parseDouble( detectTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }
		return detectLevelThreshold;
	}

	void setDetectLevelThreshold (double threshold) {
		detectTopTextField.setText(Double.toString(threshold));
	}
	
	int getSpanDiffTop() {
		int spanDiffTop = 0;
		try { spanDiffTop = Integer.parseInt( spanTopTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the analyze step value."); }
		return spanDiffTop;
	}
		
	void kymosDisplayFiltered1() {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new Tools.SequenceNameComparator()); 
		TransformOp transform;
		transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
		parent0.kymographsPane.kymosBuildFiltered(0, 1, transform, getSpanDiffTop());
	}
	
	void setInfos(SequencePlus seq) {
		transformForLevelsComboBox.setSelectedItem(seq.transformForLevels);
		directionComboBox.setSelectedIndex(seq.direction);
		setDetectLevelThreshold(seq.detectLevelThreshold);
		detectTopTextField.setText(Integer.toString(seq.detectLevelThreshold));
		detectAllLevelCheckBox.setSelected(seq.detectAllLevel);
	}
}
