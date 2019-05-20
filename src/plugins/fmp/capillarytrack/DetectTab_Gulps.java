package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class DetectTab_Gulps  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5590697762090397890L;
	public JCheckBox	detectAllGulpsCheckBox 	= new JCheckBox ("all", true);
	private JButton		displayTransform2Button	= new JButton("Display");
	private JTextField	spanTransf2TextField	= new JTextField("3");
	public JTextField 	detectGulpsThresholdTextField 	= new JTextField("90");
	private JButton 	detectGulpsButton 		= new JButton("Detect gulps");
	public JComboBox<TransformOp> transformForGulpsComboBox = new JComboBox<TransformOp> (new TransformOp[] {
			TransformOp.XDIFFN /*, TransformOp.YDIFFN, TransformOp.XYDIFFN	*/});

	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		add( GuiUtil.besidesPanel( new JLabel("threshold ", SwingConstants.RIGHT), detectGulpsThresholdTextField, transformForGulpsComboBox, displayTransform2Button));
		add( GuiUtil.besidesPanel(  new JLabel(" "), detectAllGulpsCheckBox, new JLabel("span ", SwingConstants.RIGHT), spanTransf2TextField));
		add( GuiUtil.besidesPanel( detectGulpsButton,new JLabel(" ") ));

		transformForGulpsComboBox.setSelectedItem(TransformOp.XDIFFN);
		defineActionListeners();
		
	}
	
	private void defineActionListeners() {
//		transformForGulpsComboBox.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) {
//			kymosDisplayFiltered(2);
//		}});
//		detectGulpsButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
//			parseTextFields();
//			detectGulpsButton.setEnabled( false);
//			final TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
//			kymosBuildFiltered(0, 2, transform, spanDiffTransf2);
//			kymosDetectGulps();
//			buttonsVisibilityUpdate(StatusAnalysis.MEASUREGULPS_OK );
//		}});
//		
//		displayTransform2Button.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
//			parseTextFields();
//			detectGulpsButton.setEnabled( false);
//			final TransformOp transform = (TransformOp) transformForGulpsComboBox.getSelectedItem();
//			kymosBuildFiltered(0, 2, transform, spanDiffTransf2);
//			kymosDisplayUpdate();
//			optionsKymoTab.displayKymosCheckBox.setSelected(true);
//			detectGulpsButton.setEnabled( true);
//		}});

	}
	
	public void enableItems(boolean enabled) {

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
//		if ( o == openMeasuresButton)  {
//			firePropertyChange("MEASURES_OPEN", false, true);	
//		}
//		else if ( o == saveMeasuresButton) {
//			firePropertyChange("MEASURES_SAVE", false, true);	
//		}		
	}
	
	// get/set
	
	public double getDetectGulpsThreshold() {
		double detectLevelThreshold = 0;
		try { detectLevelThreshold =  Double.parseDouble( detectGulpsThresholdTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the top threshold value."); }
		return detectLevelThreshold;
	}

}
