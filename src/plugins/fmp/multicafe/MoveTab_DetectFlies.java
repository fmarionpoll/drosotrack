package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import icy.system.thread.ThreadUtil;

import plugins.fmp.tools.OverlayThreshold;
import plugins.fmp.tools.ImageTransformTools.TransformOp;

public class MoveTab_DetectFlies extends JPanel implements ActionListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private Multicafe parent0;
	
	private JButton startComputationButton 	= new JButton("Start");
	private JButton stopComputationButton	= new JButton("Stop");
	private JComboBox<String> colorChannelComboBox = new JComboBox<String> (new String[] {"Red", "Green", "Blue"});
	private JComboBox<TransformOp> backgroundComboBox = new JComboBox<> (new TransformOp[]  {TransformOp.NONE, TransformOp.REF_PREVIOUS, TransformOp.REF_T0});
	private JSpinner thresholdSpinner		= new JSpinner(new SpinnerNumberModel(100, 0, 255, 10));
	private JTextField jitterTextField 		= new JTextField("5");
	private JCheckBox objectLowsizeCheckBox = new JCheckBox("object >");
	private JSpinner objectLowsizeSpinner	= new JSpinner(new SpinnerNumberModel(50, 0, 100000, 1));
	private JCheckBox objectUpsizeCheckBox 	= new JCheckBox("object <");
	private JSpinner objectUpsizeSpinner	= new JSpinner(new SpinnerNumberModel(500, 0, 100000, 1));
	private JCheckBox whiteMiceCheckBox 	= new JCheckBox("Track white on dark ");
	private JCheckBox thresholdedImageCheckBox = new JCheckBox("Display as overlay");
	
	private OverlayThreshold ov = null;
	private BuildTrackFliesThread trackAllFliesThread = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		add( GuiUtil.besidesPanel( startComputationButton, stopComputationButton ) );
		add( GuiUtil.besidesPanel(whiteMiceCheckBox, thresholdedImageCheckBox));
		JLabel videochannel = new JLabel("channel ");
		videochannel.setHorizontalAlignment(SwingConstants.RIGHT);
		colorChannelComboBox.setSelectedIndex(1);
		JLabel backgroundsubtraction = new JLabel("background ");
		backgroundsubtraction.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( videochannel, colorChannelComboBox, backgroundsubtraction, backgroundComboBox));
		JLabel thresholdLabel = new JLabel("threshold ");
		thresholdLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		objectLowsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( thresholdLabel, thresholdSpinner, objectLowsizeCheckBox, objectLowsizeSpinner));
		objectUpsizeCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel jitterlabel = new JLabel("jitter <= ");
		jitterlabel.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( jitterlabel, jitterTextField , objectUpsizeCheckBox, objectUpsizeSpinner) );
		
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
		
		thresholdedImageCheckBox.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				if (thresholdedImageCheckBox.isSelected()) {
					if (ov == null)
						ov = new OverlayThreshold(parent0.vSequence);
					if (parent0.vSequence != null)
						parent0.vSequence.addOverlay(ov);
					updateOverlay();
				}
				else {
					parent0.vSequence.removeOverlay(ov);
				}
			}});
		
		startComputationButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				startComputation();
			}});
		
		stopComputationButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				stopComputation();
				// TODO updateButtonsVisibility(StateD.STOP_COMPUTATION);
			}});

	}
	
	public void enableItems(boolean enabled) {
//		createROIsFromPolygonButton2.setEnabled(enabled);
//		selectGroupedby2Button.setEnabled(enabled);
//		selectRegularButton.setEnabled(enabled);
//		nbcapillariesTextField.setEnabled(enabled);
//		selectRegularButton.setEnabled(enabled);
//		selectGroupedby2Button .setEnabled(enabled);
//		width_between_capillariesTextField.setEnabled(enabled );
//		width_intervalTextField.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
	}
	
	private void updateOverlay () {
		if (ov == null) 
			ov = new OverlayThreshold(parent0.vSequence);
		else {
			parent0.vSequence.removeOverlay(ov);
			ov.setSequence(parent0.vSequence);
		}
		parent0.vSequence.addOverlay(ov);	
		ov.setTransform((TransformOp) backgroundComboBox.getSelectedItem());
		ov.setThresholdSingle(parent0.vSequence.threshold);
		if (ov != null) {
			ov.painterChanged();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == thresholdSpinner) {
			parent0.vSequence.threshold = Integer.parseInt(thresholdSpinner.getValue().toString());
			updateOverlay();
		}
	
	}
	
	private boolean initTrackParameters() {
		if (trackAllFliesThread == null)
			return false;
		
		trackAllFliesThread.vSequence = parent0.vSequence;		
		trackAllFliesThread.stopFlag = false;
		trackAllFliesThread.btrackWhite = whiteMiceCheckBox.isSelected();
		trackAllFliesThread.ichanselected = colorChannelComboBox.getSelectedIndex();
		trackAllFliesThread.blimitLow = objectLowsizeCheckBox.isSelected();
		trackAllFliesThread.blimitUp = objectUpsizeCheckBox.isSelected();
		trackAllFliesThread.limitLow = (int) objectLowsizeSpinner.getValue();
		trackAllFliesThread.limitUp = (int) objectUpsizeSpinner.getValue();
		try { trackAllFliesThread.jitter = Integer.parseInt( jitterTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret the jitter value."); return false; }
		trackAllFliesThread.transformop = (TransformOp) backgroundComboBox.getSelectedItem();
		
		return true;
	}

	public void startComputation() {
		
		// TODO transfer parameters to trackAllFliesThread
		trackAllFliesThread = new BuildTrackFliesThread();
		initTrackParameters();
		ThreadUtil.bgRun(trackAllFliesThread);
		
	}
	
	public void stopComputation() {
		if (trackAllFliesThread != null)
			trackAllFliesThread.stopFlag = true;
			
		// TODO updateButtonsVisibility(StateD.STOP_COMPUTATION);
//		if (trackAllFliesThread != null && trackAllFliesThread.isAlive()) {
//			trackAllFliesThread.interrupt();
//			try {
//				trackAllFliesThread.join();
//			} catch (InterruptedException e1) {
//				e1.printStackTrace();
//			}
	}

}