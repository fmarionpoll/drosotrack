package plugins.fmp.capillarytrack;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.ComboBoxColorRenderer;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class DetectTab_Colors  extends JPanel implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 6652216082377109572L;
	
	private JComboBox<Color> colorPickCombo 	= new JComboBox<Color>();
	private ComboBoxColorRenderer colorPickComboRenderer = new ComboBoxColorRenderer(colorPickCombo);
	private String 		textPickAPixel 			= "Pick a pixel";
	private JButton		pickColorButton			= new JButton (textPickAPixel);
	private JButton		deleteColorButton		= new JButton ("Delete color");
	private JRadioButton		rbL1			= new JRadioButton ("L1");
	private JRadioButton		rbL2			= new JRadioButton ("L2");
	public JSpinner    	distanceSpinner 		= new JSpinner (new SpinnerNumberModel(10, 0, 800, 5));
	private JRadioButton		rbRGB			= new JRadioButton ("RGB");
	private JRadioButton		rbHSV			= new JRadioButton ("HSV");
	private JRadioButton		rbH1H2H3		= new JRadioButton ("H1H2H3");
	private JLabel 		distanceLabel 			= new JLabel("Distance  ");
	private JLabel 		colorspaceLabel 		= new JLabel("Color space ", SwingConstants.RIGHT);
	private JButton 	detectColorButton 		= new JButton("Detect limits");
	private JCheckBox	detectAllColorsCheckBox = new JCheckBox ("all", true);
//	private JButton		openFiltersButton	= new JButton("Load...");
//	private JButton		saveFiltersButton	= new JButton("Save...");
	public JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(70, 0, 255, 5));
	
	private Capillarytrack parent0;
	private DetectPane parent;
	
	public void init(GridLayout capLayout, Capillarytrack parent0, DetectPane parent) {
		setLayout(capLayout);
		this.parent0 = parent0;
		this.parent = parent;
		
		colorPickCombo.setRenderer(colorPickComboRenderer);
		add( GuiUtil.besidesPanel(pickColorButton, colorPickCombo, deleteColorButton));
		distanceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		ButtonGroup bgd = new ButtonGroup();
		bgd.add(rbL1);
		bgd.add(rbL2);
		add( GuiUtil.besidesPanel(distanceLabel, rbL1, rbL2, distanceSpinner));
		ButtonGroup bgcs = new ButtonGroup();
		bgcs.add(rbRGB);
		bgcs.add(rbHSV);
		bgcs.add(rbH1H2H3);
		add( GuiUtil.besidesPanel(colorspaceLabel, rbRGB, rbHSV, rbH1H2H3));
		add(GuiUtil.besidesPanel(detectColorButton, detectAllColorsCheckBox)); 
		
		rbL1.setSelected(true);
		rbRGB.setSelected(true);
		rbL1.setSelected(true);
		rbRGB.setSelected(true);
		
		defineListeners();
		
	}
	
	private void defineListeners() {
//		deleteColorButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//			if (colorPickCombo.getItemCount() > 0 && colorPickCombo.getSelectedIndex() >= 0)
//				colorPickCombo.removeItemAt(colorPickCombo.getSelectedIndex());
//			colorsUpdateThresholdOverlayParameters();
//		} } );
//
//		pickColorButton.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//			pickColor(); 
//		} } );
//		
//		rbRGB.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//			colortransformop = TransformOp.NONE;
//			colorsUpdateThresholdOverlayParameters();
//		} } );
//	
//		rbHSV.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//				colortransformop = TransformOp.RGB_TO_HSV;
//				colorsUpdateThresholdOverlayParameters();
//			} } );
//		
//		rbH1H2H3.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//				colortransformop = TransformOp.RGB_TO_H1H2H3;
//				colorsUpdateThresholdOverlayParameters();
//			} } );
//		
//		rbL1.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//				colorsUpdateThresholdOverlayParameters();
//			} } );
//		
//		rbL2.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) { 
//				colorsUpdateThresholdOverlayParameters();
//			} } );
		
		class ItemChangeListener implements ItemListener{
		    @Override
		    public void itemStateChanged(ItemEvent event) {
		       if (event.getStateChange() == ItemEvent.SELECTED) {
		    	   updateThresholdOverlayParameters();
		       }
		    }       
		}
		colorPickCombo.addItemListener(new ItemChangeListener());
		distanceSpinner.addChangeListener(this);
		thresholdSpinner.addChangeListener(this);

	}
	
	private void updateThresholdOverlayParameters() {
		
		if (parent0.vSequence == null)
			return;
		
		boolean activateThreshold = true;
		
		switch (parent.tabbedDetectionPane.getSelectedIndex()) {
				
			case 0:	// simple filter & single threshold
				parent.simpletransformop = (TransformOp) parent.transformsComboBox.getSelectedItem();
				parent.simplethreshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				parent.thresholdtype = ThresholdType.SINGLE;
				break;

			case 1:  // color array
				// TODO
//				colorthreshold = Integer.parseInt(distanceSpinner.getValue().toString());
//				thresholdtype = ThresholdType.COLORARRAY;
//				colorarray.clear();
//				for (int i=0; i<colorPickCombo.getItemCount(); i++) {
//					colorarray.add(colorPickCombo.getItemAt(i));
//				}
//				colordistanceType = 1;
//				if (rbL2.isSelected()) 
//					colordistanceType = 2;
				break;
				
			default:
				activateThreshold = false;
				break;
		}
		
		//--------------------------------
		colorsActivateSequenceThresholdOverlay(activateThreshold);
	}
	
	public void colorsUpdateThresholdOverlayParameters() {
		
		boolean activateThreshold = true;

		switch (parent.tabbedDetectionPane.getSelectedIndex()) {
		
			case 0:	// simple filter & single threshold
				parent.simpletransformop = (TransformOp) parent.transformsComboBox.getSelectedItem();
				parent.simplethreshold = Integer.parseInt(thresholdSpinner.getValue().toString());
				parent.thresholdtype = ThresholdType.SINGLE;
				break;
				
			case 1:  // color array
				// TODO
//				colorthreshold = Integer.parseInt(distanceSpinner.getValue().toString());
//				thresholdtype = ThresholdType.COLORARRAY;
//				colorarray.clear();
//				for (int i=0; i<colorPickCombo.getItemCount(); i++) {
//					colorarray.add(colorPickCombo.getItemAt(i));
//				}
//				colordistanceType = 1;
//				if (rbL2.isSelected()) 
//					colordistanceType = 2;
				break;

			default:
				activateThreshold = false;
				break;
		}
		colorsActivateSequenceThresholdOverlay(activateThreshold);
	}
	
	private void colorsActivateSequenceThresholdOverlay(boolean activate) {
		if (parent0.kymographArrayList.size() == 0)
			return;
		
		for (SequencePlus kSeq: parent0.kymographArrayList) {
			kSeq.setThresholdOverlay(activate);
			if (activate) {
				if (parent.thresholdtype == ThresholdType.SINGLE)
					kSeq.setThresholdOverlayParametersSingle(parent.simpletransformop, parent.simplethreshold);
				else
					kSeq.setThresholdOverlayParametersColors(
							parent.colortransformop, 
							parent.colorarray, 
							parent.colordistanceType, 
							parent.colorthreshold);
			}
		}
		//thresholdOverlayON = activate;
	}

	
	public void enableItems(boolean enabled) {
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
//		if ( o == transformForLevelsComboBox)  {
//			firePropertyChange("KYMO_DISPLAYFILTERED", false, true);	
//		}

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if ((   arg0.getSource() == thresholdSpinner)  

		|| (arg0.getSource() == distanceSpinner)) 
		colorsUpdateThresholdOverlayParameters();
		
	}
	

	
	private void pickColor() {
		
		boolean bActiveTrapOverlay = false;
		// TODO
//		if (pickColorButton.getText().contains("*") || pickColorButton.getText().contains(":")) {
//			pickColorButton.setBackground(Color.LIGHT_GRAY);
//			pickColorButton.setText(textPickAPixel);
//			bActiveTrapOverlay = false;
//		}
//		else
//		{
//			pickColorButton.setText("*"+textPickAPixel+"*");
//			pickColorButton.setBackground(Color.DARK_GRAY);
//			bActiveTrapOverlay = true;
//		}
////		System.out.println("activate mouse trap =" + bActiveTrapOverlay);
//		for (SequencePlus kSeq: kymographArrayList)
//			kSeq.setMouseTrapOverlay(bActiveTrapOverlay, pickColorButton, colorPickCombo);
	}


}
