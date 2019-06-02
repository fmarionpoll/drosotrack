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
import plugins.fmp.tools.ImageTransformTools.TransformOp;

public class KymosTab_DetectGulps extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5590697762090397890L;
	public JCheckBox	detectAllGulpsCheckBox 	= new JCheckBox ("all", true);
	private JButton		displayTransform2Button	= new JButton("Display");
	private JTextField	spanTransf2TextField	= new JTextField("3");
	public JTextField 	detectGulpsThresholdTextField 	= new JTextField("90");
	private JButton 	detectGulpsButton 		= new JButton("Detect");
	public JComboBox<TransformOp> transformForGulpsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});
	private	int	spanDiffTransf2 			= 3;
	private double detectGulpsThreshold 	= 5.;
	private MultiCAFE parent0;
	
	public void init(GridLayout capLayout, MultiCAFE parent0) {
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
	
	public void enableItems(boolean enabled) {
		detectGulpsButton.setEnabled(enabled);
		detectAllGulpsCheckBox.setEnabled(enabled);
		transformForGulpsComboBox.setEnabled(enabled);
		detectGulpsThresholdTextField.setEnabled(enabled);
		displayTransform2Button.setEnabled(enabled);
		spanTransf2TextField.setEnabled(enabled);
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
			BuildDetect_Gulps detect = new BuildDetect_Gulps();
			detect.detectGulps(parent0);
			firePropertyChange("KYMO_DETECT_GULP", false, true);
		}
		else if (o == displayTransform2Button) {
			kymosDisplayFiltered2();
			parent0.kymographsPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
	}
	
	// get/set
	
	public double getDetectGulpsThreshold() {
		try { detectGulpsThreshold =  Double.parseDouble( detectGulpsThresholdTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }
		return detectGulpsThreshold;
	}
	
	public void kymosDisplayFiltered2() {
		if (parent0.kymographArrayList == null)
			return;
		Collections.sort(parent0.kymographArrayList, new Tools.SequenceNameComparator()); 
		TransformOp transform;
		transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
		parent0.kymographsPane.kymosBuildFiltered(0, 2, transform, spanDiffTransf2);
	}

}
