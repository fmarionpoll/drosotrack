package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.SequencePlus;

public class SequenceTab_Close  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7576474358794296471L;
	private JButton		closeAllButton			= new JButton("Close views");
	private MultiCAFE parent0 = null;
	
	public void init(GridLayout capLayout, MultiCAFE parent0) {
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
	
	public void closeAll() {
		for (SequencePlus seq:parent0.kymographArrayList)
			seq.close();
		
		parent0.resultsPane.graphicsTab.closeAll();

		if (parent0.vSequence != null) {
			parent0.vSequence.removeListener(parent0);
			parent0.vSequence.close();
			parent0.vSequence.capillariesArrayList.clear();
		}

		// clean kymographs & results
		parent0.kymographArrayList.clear();
		parent0.kymographsPane.optionsTab.kymographNamesComboBox.removeAllItems();
	}

}
