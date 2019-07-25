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
import plugins.fmp.toolsMulticafe.Tools;
import plugins.fmp.toolsMulticafe.ImageTransformTools.TransformOp;
import plugins.fmp.toolsSequence.SequencePlus;

public class MCKymosTab_DetectGulps extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5590697762090397890L;
	JCheckBox	detectAllGulpsCheckBox 	= new JCheckBox ("all", true);
	private JButton		displayTransform2Button	= new JButton("Display");
	private JTextField	spanTransf2TextField	= new JTextField("3");
	private  JTextField 	detectGulpsThresholdTextField 	= new JTextField("90");
	private JButton 	detectGulpsButton 		= new JButton("Detect");
	JComboBox<TransformOp> transformForGulpsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	private	int	spanDiffTransf2 			= 3;
	private double detectGulpsThreshold 	= 5.;
	private Multicafe parent0;
	
	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		add( GuiUtil.besidesPanel( new JLabel("threshold ", SwingConstants.RIGHT), detectGulpsThresholdTextField, transformForGulpsComboBox, displayTransform2Button));
		add( GuiUtil.besidesPanel( new JLabel(" "), detectAllGulpsCheckBox, new JLabel("span ", SwingConstants.RIGHT), spanTransf2TextField));
		add( GuiUtil.besidesPanel( detectGulpsButton,new JLabel(" ") ));

		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		transformForGulpsComboBox.addActionListener(this); 
		detectGulpsButton.addActionListener(this);
		displayTransform2Button.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == transformForGulpsComboBox)  {
			kymosDisplayFiltered2();
		}
		else if (o == detectGulpsButton) {
			getDetectGulpsThreshold();
			kymosDisplayFiltered2();
			MCBuildDetect_Gulps detect = new MCBuildDetect_Gulps();
			detect.detectGulps(parent0);
			firePropertyChange("KYMO_DETECT_GULP", false, true);
		}
		else if (o == displayTransform2Button) {
			kymosDisplayFiltered2();
			parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
	}
	
	// get/set
	
	double getDetectGulpsThreshold() {
		try { detectGulpsThreshold =  Double.parseDouble( detectGulpsThresholdTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }
		return detectGulpsThreshold;
	}
	
	void kymosDisplayFiltered2() {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new Tools.SequenceNameComparator()); 
		TransformOp transform;
		transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		parent0.kymographsPane.kymosBuildFiltered(0, 2, transform, spanDiffTransf2);
	}
	
	void setInfos(SequencePlus seq) {
		detectGulpsThresholdTextField.setText(Integer.toString(seq.detectGulpsThreshold));
		transformForGulpsComboBox.setSelectedItem(seq.transformForGulps);
		detectAllGulpsCheckBox.setSelected(seq.detectAllGulps);
	}

}
