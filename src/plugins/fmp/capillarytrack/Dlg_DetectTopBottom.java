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

public class Dlg_DetectTopBottom  extends JPanel implements ActionListener {

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
	public JButton 		detectTopButton 		= new JButton("Detect level");
	
	
	public void init(GridLayout capLayout) {
		setLayout(capLayout);
		
		((JLabel) directionComboBox.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		add( GuiUtil.besidesPanel(directionComboBox, detectTopTextField, transformForLevelsComboBox, displayTransform1Button )); 
		add( GuiUtil.besidesPanel(new JLabel("span ", SwingConstants.RIGHT), spanTopTextField, new JLabel(" "), new JLabel(" ")));
		add( GuiUtil.besidesPanel( detectTopButton,  detectAllLevelCheckBox, new JLabel(" ")));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
//		transformForLevelsComboBox.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
//			kymosDisplayFiltered(1);
//		}});
//		detectTopButton.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
//			parseTextFields();
//			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
//			final TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
//			detectTopButton.setEnabled( false);
//			kymosBuildFiltered(0, 1, transform, spanDiffTop);
//			
//			kymosDetectCapillaryLevels();
//			buttonsVisibilityUpdate(StatusAnalysis.MEASURETOP_OK); 
//		}});
//		
//		displayTransform1Button.addActionListener(new ActionListener() {	@Override public void actionPerformed(ActionEvent e) {
//			parseTextFields();
//			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
//			final TransformOp transform = (TransformOp) transformForLevelsComboBox.getSelectedItem();
//			detectTopButton.setEnabled( false);
//			kymosBuildFiltered(0, 1, transform, spanDiffTop);
//			kymosDisplayUpdate();
//			optionsKymoTab.displayKymosCheckBox.setSelected(true);
//			detectTopButton.setEnabled( true);
//		}});
		
	}
	
	public void enableItems(boolean enabled) {
		detectAllLevelCheckBox.setEnabled(enabled); // * benabled
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
//		if ( o == createROIsFromPolygonButton2)  {
//			firePropertyChange("CREATE_ROILINES", false, true);	
//		}

	}
	
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
}
