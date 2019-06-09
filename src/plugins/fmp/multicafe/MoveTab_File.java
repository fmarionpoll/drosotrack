package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;



public class MoveTab_File extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;

	private JButton	openROIsButton			= new JButton("Load...");
	private JButton	saveROIsButton			= new JButton("Save...");
	private Multicafe parent0;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;

		JLabel 	loadsaveText1 = new JLabel ("-> File (xml) ");
		loadsaveText1.setHorizontalAlignment(SwingConstants.RIGHT); 
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		JLabel emptyText1	= new JLabel (" ");
		add(GuiUtil.besidesPanel( emptyText1, loadsaveText1, openROIsButton, saveROIsButton));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
	
		openROIsButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				parent0.vSequence.cages.xmlReadCagesFromFile(parent0.vSequence);	
//				ArrayList<ROI2D> list = parent0.vSequence.getROI2Ds();
//				Collections.sort(list, new Tools.ROI2DNameComparator());
//				int nrois = list.size();
//				if (nrois > 0)
//					nbcagesTextField.setText(Integer.toString(nrois));
				firePropertyChange("LOAD_DATA", false, true);	
			}});
		
		saveROIsButton.addActionListener(new ActionListener () {
			@Override
			public void actionPerformed( final ActionEvent e ) { 
				parent0.vSequence.cages.xmlWriteCagesToFile("drosotrack.xml", parent0.vSequence.getDirectory());

			}});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
//		Object o = e.getSource();
//		if ( o == createROIsFromPolygonButton2)  {
//			roisGenerateFromPolygon();
//			parent0.vSequence.keepOnly2DLines_CapillariesArrayList();
//			firePropertyChange("CAPILLARIES_NEW", false, true);	
//		}
//		else if ( o == selectRegularButton) {
//			boolean status = false;
//			width_between_capillariesTextField.setEnabled(status);
//			width_intervalTextField.setEnabled(status);	
//		}
	}
	

	public boolean cageRoisOpen(String csFileName) {
		
		boolean flag = false;
		if (csFileName == null)
			flag = parent0.vSequence.cages.xmlReadCagesFromFile(parent0.vSequence);
		else
			flag = parent0.vSequence.cages.xmlReadCagesFromFileNoQuestion(csFileName, parent0.vSequence);
		return flag;
	}
	
	public boolean cageRoisSave() {
		return parent0.vSequence.cages.xmlWriteCagesToFile("drosotrack.xml", parent0.vSequence.getDirectory());
	}
}