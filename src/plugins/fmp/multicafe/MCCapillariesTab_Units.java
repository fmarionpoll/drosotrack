package plugins.fmp.multicafe;

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
import plugins.fmp.sequencevirtual.Capillaries;

public class MCCapillariesTab_Units  extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;
	
		JCheckBox					visibleCheckBox				= new JCheckBox("ROIs visible", true);
		private JTextField 			capillaryVolumeTextField	= new JTextField("5");
		private JTextField 			capillaryPixelsTextField	= new JTextField("5");
				
		private Multicafe parent0;
		
		void init(GridLayout capLayout, Multicafe parent0) {
			setLayout(capLayout);
			
			add( GuiUtil.besidesPanel(
					visibleCheckBox,
					new JLabel("volume (µl) ", SwingConstants.RIGHT), 
					capillaryVolumeTextField,  
					new JLabel("length (pixels) ", SwingConstants.RIGHT), 
					capillaryPixelsTextField));
									
			this.parent0 = parent0;
			defineActionListeners();
		}
				
		private void defineActionListeners() {
			visibleCheckBox.addActionListener(this);
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
		
		void setCapillariesInfos(Capillaries cap) {
			capillaryVolumeTextField.setText( Double.toString(cap.volume));
			capillaryPixelsTextField.setText( Double.toString(cap.pixels));
			
		}

		
		private double getCapillaryVolume() {
			double capillaryVolume = 0;
			try { 
				capillaryVolume = Double.parseDouble(capillaryVolumeTextField.getText());
			} catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }
			return capillaryVolume;
		}
		
		
		private double getCapillaryPixelLength() {
			double capillaryPixels=0;
			try { 
				capillaryPixels = Double.parseDouble(capillaryPixelsTextField.getText()); 
			} catch( Exception e ) { new AnnounceFrame("Can't interpret capillary volume value."); }
			return capillaryPixels;
		}
		
		void getCapillariesInfos(Capillaries cap) {
			cap.volume = getCapillaryVolume();
			cap.pixels = getCapillaryPixelLength();
		}
}
