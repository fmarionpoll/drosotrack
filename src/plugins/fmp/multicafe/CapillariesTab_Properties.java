package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.roi.ROI;


public class CapillariesTab_Properties extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;
	
		private JTextField 	capillaryVolumeTextField= new JTextField("5");
		private JTextField 	capillaryPixelsTextField= new JTextField("5");
		public JCheckBox	visibleCheckBox			= new JCheckBox("ROIs visible", true);
		
		public JCheckBox 	selectCapillaryCheckBox = new JCheckBox("select capillary");
		public JComboBox<String> kymographNamesComboBox = new JComboBox<String> (new String[] {"none"});
		public JButton 		updateButton 			= new JButton("Update");
		public JButton  	previousButton		 	= new JButton("<");
		public JButton		nextButton				= new JButton(">");
		
		private Multicafe parent0;
		
		public void init(GridLayout capLayout, Multicafe parent0) {
			setLayout(capLayout);
			add( GuiUtil.besidesPanel(
					new JLabel("volume (µl) ", SwingConstants.RIGHT), 
					capillaryVolumeTextField,  
					new JLabel("length (pixels) ", SwingConstants.RIGHT), 
					capillaryPixelsTextField));
			add( GuiUtil.besidesPanel(
					visibleCheckBox,  
					new JLabel(" ", SwingConstants.RIGHT)));
			
			JPanel k2Panel = new JPanel();
			k2Panel.setLayout(new BorderLayout());
			k2Panel.add(previousButton, BorderLayout.WEST); 
			int bWidth = 30;
			int height = 10;
			previousButton.setPreferredSize(new Dimension(bWidth, height));
			k2Panel.add(kymographNamesComboBox, BorderLayout.CENTER);
			nextButton.setPreferredSize(new Dimension(bWidth, height)); 
			k2Panel.add(nextButton, BorderLayout.EAST);
			
			add(GuiUtil.besidesPanel( selectCapillaryCheckBox, k2Panel));
			
			this.parent0 = parent0;
			defineActionListeners();
		}
		
		private void defineActionListeners() {
			visibleCheckBox.addActionListener(this);
		}

		public void enableItems(boolean enabled) {
			capillaryVolumeTextField.setEnabled(enabled);
			capillaryPixelsTextField.setEnabled(enabled);
			visibleCheckBox.setEnabled(enabled);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();
			if (o == visibleCheckBox) {
				roisDisplayLine(visibleCheckBox.isSelected());
			}
		}
		
		private void roisDisplayLine(boolean isVisible) {
			ArrayList<Viewer>vList =  parent0.vSequence.getViewers();
			Viewer v = vList.get(0);
			IcyCanvas canvas = v.getCanvas();
			List<Layer> layers = canvas.getLayers(false);
			if (layers == null)
				return;
			for (Layer layer: layers) {
				ROI roi = layer.getAttachedROI();
				if (roi == null)
					continue;
				String cs = roi.getName();
				if (cs.contains("line"))  
					layer.setVisible(isVisible);
			}
		}
			
		// set/ get
		
		public void setCapillaryVolume(double capillaryVolume) {
			capillaryVolumeTextField.setText( Double.toString(capillaryVolume));
		}
		public double getCapillaryVolume() {
			double capillaryVolume = 0;
			try { 
				capillaryVolume = Double.parseDouble(capillaryVolumeTextField.getText());
			}catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }
			return capillaryVolume;
		}
		
		public void setCapillaryPixelLength(double capillaryPixels) {
			capillaryPixelsTextField.setText( Double.toString(capillaryPixels));
		}
		public double getCapillaryPixelLength() {
			double capillaryPixels=0;
			try { 
				capillaryPixels = Double.parseDouble(capillaryPixelsTextField.getText()); 
			} catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }
			return capillaryPixels;
		}
		
		public void updateSequenceFromDialog() {
			parent0.vSequence.capillaries.capillaryVolume = getCapillaryVolume();
			parent0.vSequence.capillaries.capillaryPixels = getCapillaryPixelLength();
		}
}
