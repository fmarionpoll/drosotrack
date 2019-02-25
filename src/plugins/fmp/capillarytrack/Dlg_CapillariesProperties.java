package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;

public class Dlg_CapillariesProperties extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4950182090521600937L;
		private JTextField 	capillaryVolumeTextField= new JTextField("5");
		private JTextField 	capillaryPixelsTextField= new JTextField("5");
		
		public void init(GridLayout capLayout) {
			setLayout(capLayout);
			add( GuiUtil.besidesPanel(new JLabel("volume (µl) ", SwingConstants.RIGHT), capillaryVolumeTextField,  new JLabel("length (pixels) ", SwingConstants.RIGHT), capillaryPixelsTextField));
			defineActionListeners();
		}
		
		private void defineActionListeners() {

		}
		
		public void enableItems(boolean enabled) {
			capillaryVolumeTextField.setEnabled(enabled);
			capillaryPixelsTextField.setEnabled(enabled);
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			//Object o = e.getSource();
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
