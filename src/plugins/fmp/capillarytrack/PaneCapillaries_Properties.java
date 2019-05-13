package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
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
import plugins.fmp.sequencevirtual.SequencePlus;

public class PaneCapillaries_Properties extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;
	
		private JTextField 	capillaryVolumeTextField= new JTextField("5");
		private JTextField 	capillaryPixelsTextField= new JTextField("5");
		public JCheckBox	displayCapillariesCheckBox	= new JCheckBox("display capillaries", false);
		private Capillarytrack parent0;
		
		public void init(GridLayout capLayout, Capillarytrack parent0) {
			setLayout(capLayout);
			add( GuiUtil.besidesPanel(new JLabel("volume (µl) ", SwingConstants.RIGHT), capillaryVolumeTextField,  new JLabel("length (pixels) ", SwingConstants.RIGHT), capillaryPixelsTextField));
			add( GuiUtil.besidesPanel(displayCapillariesCheckBox,  new JLabel(" ", SwingConstants.RIGHT)));
			
			this.parent0 = parent0;
			defineActionListeners();
		}
		
		private void defineActionListeners() {
			displayCapillariesCheckBox.addActionListener(this);
		}

		public void enableItems(boolean enabled) {
			capillaryVolumeTextField.setEnabled(enabled);
			capillaryPixelsTextField.setEnabled(enabled);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			Object o = e.getSource();
			if (o == displayCapillariesCheckBox) {
				roisDisplay(displayCapillariesCheckBox.isSelected());
			}
		}
		
		private void roisDisplay(boolean displayON) {
			
			for (SequencePlus seq: kymographArrayList) {
				ArrayList<Viewer>vList =  seq.getViewers();
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
					if (cs.contains("level")) { 
						layer.setVisible(displayTop);
					}
					else 
						layer.setVisible(displayGulps);
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
			}catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }
			return capillaryPixels;
		}
}
