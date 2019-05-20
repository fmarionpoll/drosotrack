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
	private JSpinner    distanceSpinner 		= new JSpinner (new SpinnerNumberModel(10, 0, 800, 5));
	private JRadioButton		rbRGB			= new JRadioButton ("RGB");
	private JRadioButton		rbHSV			= new JRadioButton ("HSV");
	private JRadioButton		rbH1H2H3		= new JRadioButton ("H1H2H3");
	private JLabel 		distanceLabel 			= new JLabel("Distance  ");
	private JLabel 		colorspaceLabel 		= new JLabel("Color space ", SwingConstants.RIGHT);
//	private JButton		openFiltersButton	= new JButton("Load...");
//	private JButton		saveFiltersButton	= new JButton("Save...");
	private JButton 	detectColorButton 		= new JButton("Detect limits");
	private JCheckBox	detectAllColorsCheckBox = new JCheckBox ("all", true);

	private Capillarytrack parent0;
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
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
//		    	   updateThresholdOverlayParameters();
		       }
		    }       
		}
		colorPickCombo.addItemListener(new ItemChangeListener());
		distanceSpinner.addChangeListener(this);

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
		// TODO Auto-generated method stub
		
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
