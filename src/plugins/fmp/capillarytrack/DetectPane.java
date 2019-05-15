package plugins.fmp.capillarytrack;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class DetectPane extends JPanel {
	
	public JTabbedPane tabbedDetectionPane	= new JTabbedPane();
	public DetectTab_TopBottom detectTopBottomTab = new DetectTab_TopBottom();
	public DetectTab_Colors detectColorsTab = new DetectTab_Colors();
	public DetectTab_Gulps detectGulpsTab = new DetectTab_Gulps();
	public DetectTab_File detectLoadSave = new DetectTab_File();
	
	public JRadioButton rbFilterbyColor		= new JRadioButton("filter by color array");
	public JRadioButton rbFilterbyFunction		= new JRadioButton("filter by function");
	// colors
	public TransformOp colortransformop 	= TransformOp.NONE;
	public int 		colordistanceType 	= 0;
	public int 		colorthreshold 		= 20;
	public ArrayList <Color> colorarray 	= new ArrayList <Color>();
	//private boolean 	thresholdOverlayON	= false;
	public ThresholdType thresholdtype 	= ThresholdType.COLORARRAY; 
	// TODO
	public TransformOp simpletransformop 	= TransformOp.R2MINUS_GB;
	public int 		simplethreshold 	= 20;

	
	public void init (JPanel mainPanel, String string, Capillarytrack parent) {
		final JPanel panel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(panel));
		panel.add( GuiUtil.besidesPanel(rbFilterbyFunction, rbFilterbyColor));
		ButtonGroup bgchoice = new ButtonGroup();
		bgchoice.add(rbFilterbyColor);
		bgchoice.add(rbFilterbyFunction);
		GridLayout capLayout = new GridLayout(4, 2);
		
		detectTopBottomTab.init(capLayout);
		detectTopBottomTab.addPropertyChangeListener(parent);
		tabbedDetectionPane.addTab("Filters", null, detectTopBottomTab, "thresholding a transformed image with different filters");
		
		detectColorsTab.init(capLayout);
		detectTopBottomTab.addPropertyChangeListener(parent);
		tabbedDetectionPane.addTab("Colors", null, detectColorsTab, "thresholding an image with different colors and a distance");
		
		detectGulpsTab.init(capLayout);	
		detectTopBottomTab.addPropertyChangeListener(parent);
		tabbedDetectionPane.addTab("Gulps", null, detectGulpsTab, "detect gulps");
		
		detectLoadSave.init (capLayout);
		detectTopBottomTab.addPropertyChangeListener(parent);
		tabbedDetectionPane.addTab("Load/Save", null, detectLoadSave, "load / save parameters");
		
		tabbedDetectionPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		panel.add(GuiUtil.besidesPanel(tabbedDetectionPane));
		
		detectTopBottomTab.addPropertyChangeListener(parent);
	
		detectTopBottomTab.transformForLevelsComboBox.setSelectedItem(TransformOp.G2MINUS_RB);
		colortransformop = TransformOp.NONE;
		tabbedDetectionPane.setSelectedIndex(0);
		rbFilterbyFunction.setSelected(true);
		
		rbFilterbyColor.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			if (rbFilterbyColor.isSelected())
				selectTab(1);
		} } );
		
		rbFilterbyFunction.addActionListener(new ActionListener () { @Override public void actionPerformed( final ActionEvent e ) {
			if (rbFilterbyFunction.isSelected())
				selectTab(0);
		} } );
	}
	
	private void selectTab(int index) {
		tabbedDetectionPane.setSelectedIndex(index);
	}

}

