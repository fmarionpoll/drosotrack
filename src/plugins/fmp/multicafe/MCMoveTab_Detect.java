package plugins.fmp.multicafe;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI2D;
import icy.system.thread.ThreadUtil;
import plugins.fmp.tools.BuildTrackFliesThread;
import plugins.fmp.tools.DetectFliesParameters;
import plugins.fmp.tools.OverlayThreshold;
import plugins.fmp.tools.ImageTransformTools.TransformOp;


public class MCMoveTab_Detect extends JPanel implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private Multicafe parent0;
	
	private JButton startComputationButton 	= new JButton("Start / Stop");
	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	private JComboBox<TransformOp> backgroundComboBox = new JComboBox<> (new TransformOp[]  {TransformOp.NONE, TransformOp.REF_PREVIOUS, TransformOp.REF_T0});
	private JSpinner thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JTextField jitterTextField 		= new JTextField("5");
	private JCheckBox objectLowsizeCheckBox = new JCheckBox("object >");
	private JSpinner objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner objectUpsizeSpinner	= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JCheckBox whiteMiceCheckBox 	= new JCheckBox("white on dark ");
	public JCheckBox thresholdedImageCheckBox = new JCheckBox("overlay");
	
	private OverlayThreshold ov = null;
	private BuildTrackFliesThread trackAllFliesThread = null;
	
	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JPanel dummyPanel = new JPanel();
		dummyPanel.add( GuiUtil.besidesPanel(whiteMiceCheckBox, thresholdedImageCheckBox ) );
		FlowLayout layout = (FlowLayout) dummyPanel.getLayout();
		layout.setVgap(0);
		dummyPanel.validate();
		
		add( GuiUtil.besidesPanel(startComputationButton,  dummyPanel));
		
		colorChannelComboBox.setSelectedIndex(1);
		add( GuiUtil.besidesPanel( new JLabel("channel ", SwingConstants.RIGHT), 
				colorChannelComboBox, 
				new JLabel("background ", SwingConstants.RIGHT), 
				backgroundComboBox));
		
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel(new JLabel("threshold ", 
				SwingConstants.RIGHT), 
				thresholdSpinner, 
				objectLowsizeCheckBox, 
				objectLowsizeSpinner));
		
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( new JLabel("jitter <= ", SwingConstants.RIGHT), 
				jitterTextField , 
				objectUpsizeCheckBox, 
				objectUpsizeSpinner) );
		
		defineActionListeners();
		thresholdSpinner.addChangeListener(this);
	}
	
	private void defineActionListeners() {
		colorChannelComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				updateOverlay(); 
			} } );
		
		backgroundComboBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				updateOverlay(); 
			} } );
		
		thresholdedImageCheckBox.addItemListener(new ItemListener() {
		      public void itemStateChanged(ItemEvent e) {
		    	  if (thresholdedImageCheckBox.isSelected()) {
						if (ov == null)
							ov = new OverlayThreshold(parent0.vSequence);
						if (parent0.vSequence != null)
							parent0.vSequence.addOverlay(ov);
						updateOverlay();
					}
					else {
						removeOverlay();
					}
		      }
		    });
		
		
		startComputationButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				startComputation();
			}});
	}
	
	public void updateOverlay () {
		if (ov == null) 
			ov = new OverlayThreshold(parent0.vSequence);
		else {
			parent0.vSequence.removeOverlay(ov);
			ov.setSequence(parent0.vSequence);
		}
		parent0.vSequence.addOverlay(ov);	
		ov.setTransform((TransformOp) backgroundComboBox.getSelectedItem());
		ov.setThresholdSingle(parent0.vSequence.cages.detect.threshold);
		ov.painterChanged();
	}
	
	public void removeOverlay() {
		parent0.vSequence.removeOverlay(ov);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			parent0.vSequence.cages.detect.threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
			updateOverlay();
		}
	}
	
	private boolean initTrackParameters() {
		if (trackAllFliesThread == null)
			return false;
		
		trackAllFliesThread.vSequence = parent0.vSequence;		
		trackAllFliesThread.stopFlag = false;
		DetectFliesParameters detect = new DetectFliesParameters();
		detect.btrackWhite = whiteMiceCheckBox.isSelected();
		detect.ichanselected = colorChannelComboBox.getSelectedIndex();
		detect.blimitLow = objectLowsizeCheckBox.isSelected();
		detect.blimitUp = objectUpsizeCheckBox.isSelected();
		detect.limitLow = (int) objectLowsizeSpinner.getValue();
		detect.limitUp = (int) objectUpsizeSpinner.getValue();
		try { detect.jitter = Integer.parseInt( jitterTextField.getText() );
		} catch( Exception e ) { new AnnounceFrame("Can't interpret the jitter value."); return false; }
		detect.transformop = (TransformOp) backgroundComboBox.getSelectedItem();
		trackAllFliesThread.detect = detect;
		return true;
	}
	
	private void cleanPreviousDetections() {
		parent0.vSequence.cages.flyPositionsList.clear();
		ArrayList<ROI2D> list = parent0.vSequence.getROI2Ds();
		for (ROI2D roi: list) {
			if (roi.getName().contains("det")) {
				parent0.vSequence.removeROI(roi);
			}
		}
	}

	void startComputation() {
		if (trackAllFliesThread == null)
			trackAllFliesThread = new BuildTrackFliesThread();
		
		if (trackAllFliesThread.threadRunning) {
			stopComputation();
			return;
		}
		initTrackParameters();
		cleanPreviousDetections();
		ThreadUtil.bgRun(trackAllFliesThread);
	}
	
	void stopComputation() {
		if (trackAllFliesThread != null)
			trackAllFliesThread.stopFlag = true;
	}

}
