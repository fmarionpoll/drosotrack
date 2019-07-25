package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import plugins.fmp.multicafeSequence.SequencePlus;

public class MCSequenceTab_Close  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7576474358794296471L;
	private JButton		closeAllButton			= new JButton("Close views");
	private Multicafe parent0 = null;
	
	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0  = parent0;
		add( GuiUtil.besidesPanel(closeAllButton, new JLabel(" ")));
		
		closeAllButton.addActionListener(this);
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == closeAllButton) {
			closeAll();
			firePropertyChange("SEQ_CLOSE", false, true);
		}
	}
	
	void closeAll() {
		
		if (parent0.kymographArrayList != null) {
			for (SequencePlus seq:parent0.kymographArrayList) {
				ArrayList<Viewer> viewerList = seq.getViewers();
				for (Viewer v: viewerList)
					v.close();
				seq.close();
			}
			parent0.kymographArrayList.clear();
		}
		
		parent0.movePane.graphicsTab.closeAll();
		parent0.kymographsPane.graphsTab.closeAll();

		if (parent0.vSequence != null) {
			parent0.vSequence.removeAllROI();
			parent0.vSequence.removeListener(parent0);
			parent0.vSequence.close();
			parent0.vSequence.capillaries.capillariesArrayList.clear();
		}

		parent0.capillariesPane.optionsTab.kymographNamesComboBox.removeAllItems();
	}

}
