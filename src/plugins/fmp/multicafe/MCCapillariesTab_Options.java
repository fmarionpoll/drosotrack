package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.main.ActiveViewerListener;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.kernel.roi.roi2d.ROI2DShape;


public class MCCapillariesTab_Options extends JPanel implements ActionListener, ActiveViewerListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2103052112476748890L;
	JCheckBox 	viewKymosCheckBox 		= new JCheckBox("View kymos");
	JComboBox<String> kymographNamesComboBox = new JComboBox<String> (new String[] {"none"});
	JButton 		updateButton 			= new JButton("Update");
	JButton  	previousButton		 	= new JButton("<");
	JButton		nextButton				= new JButton(">");
	JCheckBox 	viewLevelsCheckbox 		= new JCheckBox("capillary levels", true);
	JCheckBox 	viewGulpsCheckbox 		= new JCheckBox("gulps", true);

	private Multicafe parent0 = null;
	private int previousupfront  = -1;

	void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		
		JPanel k2Panel = new JPanel();
		k2Panel.setLayout(new BorderLayout());
		k2Panel.add(previousButton, BorderLayout.WEST); 
		int bWidth = 30;
		int height = 10;
		previousButton.setPreferredSize(new Dimension(bWidth, height));
		k2Panel.add(kymographNamesComboBox, BorderLayout.CENTER);
		nextButton.setPreferredSize(new Dimension(bWidth, height)); 
		k2Panel.add(nextButton, BorderLayout.EAST);
		add(GuiUtil.besidesPanel( viewKymosCheckBox, k2Panel));
		
		add(GuiUtil.besidesPanel( viewLevelsCheckbox, viewGulpsCheckbox, updateButton));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		updateButton.addActionListener(this);
		kymographNamesComboBox.addActionListener(this);
		viewGulpsCheckbox.addActionListener(this);
		viewLevelsCheckbox.addActionListener(this);
		viewKymosCheckBox.addActionListener(this);
		nextButton.addActionListener(this);
		previousButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if (( o == updateButton) || (o == kymographNamesComboBox)) {
			displayUpdate();
		}
		else if (( o == viewGulpsCheckbox) || (o == viewLevelsCheckbox)) {
			roisDisplay();	
		}
		else if ( o == viewKymosCheckBox) {
			displayViews(viewKymosCheckBox.isSelected());
		}
		else if ( o == nextButton) {
			int isel = kymographNamesComboBox.getSelectedIndex()+1;
			if (isel < kymographNamesComboBox.getItemCount()) {
				selectKymograph(isel);
			}
		}
		else if ( o == previousButton) {
			int isel = kymographNamesComboBox.getSelectedIndex()-1;
			if (isel < kymographNamesComboBox.getItemCount()) {
				selectKymograph(isel);
			}
		}
	}
	
	// ---------------------------
	void transferFileNamesToComboBox() {
		kymographNamesComboBox.removeAllItems();
		if (parent0.kymographArrayList == null)
			return;
		for (SequencePlus kymographSeq: parent0.kymographArrayList) {
			kymographNamesComboBox.addItem(kymographSeq.getName());
		}
	}
	
	void transferRoisNamesToComboBox(ArrayList <ROI2DShape> roi2DArrayList) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				kymographNamesComboBox.removeAllItems();
				for (ROI2D roi:roi2DArrayList)
					kymographNamesComboBox.addItem(roi.getName());	
			}});
	}
	
	private void roisDisplay() {
		boolean displayTop = viewLevelsCheckbox.isSelected();
		boolean displayGulps = viewGulpsCheckbox.isSelected();
		for (SequencePlus seq: parent0.kymographArrayList) {
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
	}

	void displayON() {
		if (parent0.kymographArrayList == null 
			||parent0.kymographArrayList.size() < 1) 
			return;

		Rectangle rectMaster = parent0.vSequence.getFirstViewer().getBounds();
		int deltax = 5 + rectMaster.width;
		int deltay = 5;

		for(SequencePlus seq: parent0.kymographArrayList) 
		{
			ArrayList<Viewer>vList = seq.getViewers();
			if (vList.size() == 0) 
			{
				Viewer v = new Viewer(seq, true);
				v.addListener(parent0);
				Rectangle rectDataView = v.getBounds();
				rectDataView.height = rectMaster.height;
				IcyBufferedImage img = seq.getFirstImage();
				if (img != null)
					rectDataView.width = img.getSizeX() * rectMaster.height / img.getSizeY();
				rectDataView.translate(
						rectMaster.x + deltax - rectDataView.x, 
						rectMaster.y + deltay - rectDataView.y);
				v.setBounds(rectDataView);
			}
		}
		Icy.getMainInterface().addActiveViewerListener(this);
	}
	
	void displayOFF() {
		int nseq = parent0.kymographArrayList.size();
		if (nseq < 1) return;

		for(SequencePlus seq: parent0.kymographArrayList) 
		{
			ArrayList<Viewer>vList =  seq.getViewers();
			if (vList.size() > 0) 
			{
				for (Viewer v: vList) 
					v.close();
				vList.clear();
			}
		}
		previousupfront =-1;
		Icy.getMainInterface().removeActiveViewerListener(this);
	}
	
	void displayUpdate() {	
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (parent0.kymographArrayList.size() < 1 || kymographNamesComboBox.getItemCount() < 1)
					return;	
				
				displayON();
				int itemupfront = kymographNamesComboBox.getSelectedIndex();
				if (itemupfront < 0) {
					itemupfront = 0;
					kymographNamesComboBox.setSelectedIndex(0);
				}
				Viewer v = parent0.kymographArrayList.get(itemupfront).getFirstViewer();
				if (v != null) {
					if (previousupfront != itemupfront 
							&& previousupfront >= 0 
							&& previousupfront < parent0.kymographArrayList.size()) {
						
						SequencePlus seq0 =  parent0.kymographArrayList.get(previousupfront);
						// save changes and interpolate points if necessary
						if (seq0.hasChanged) {
							seq0.validateRois();
							seq0.hasChanged = false;
						}
						// update canvas size of all kymograph windows if size of window has changed
						Viewer v0 = parent0.kymographArrayList.get(previousupfront).getFirstViewer();
						Rectangle rect0 = v0.getBounds();
						Canvas2D cv0 = (Canvas2D) v0.getCanvas();
						int positionZ0 = cv0.getPositionZ(); 
								
						Rectangle rect = v.getBounds();
						if (rect != rect0) {
							v.setBounds(rect0);
							int i= 0;
							int imax = 500;
							while (!v.isInitialized() && i < imax) { i ++; }
							if (!v.isInitialized())
								System.out.println("Viewer still not initialized after " + imax +" iterations");
							
							for (SequencePlus seq: parent0.kymographArrayList) {
								Viewer vi = seq.getFirstViewer();
								Rectangle recti = vi.getBounds();
								if (recti != rect0)
									vi.setBounds(rect0);
							}
						}
						// copy zoom and position of image from previous canvas to the one selected
						Canvas2D cv = (Canvas2D) v.getCanvas();
						cv.setScale(cv0.getScaleX(), cv0.getScaleY(), false);
						cv.setOffset(cv0.getOffsetX(), cv0.getOffsetY(), false);
						cv.setPositionZ(positionZ0);
					}
					v.toFront();
					v.requestFocus();
					previousupfront = itemupfront;
				}
			}
		});
	}

	void displayViews (boolean bEnable) {
		updateButton.setEnabled(bEnable);
		previousButton.setEnabled(bEnable);
		nextButton.setEnabled(bEnable);
		kymographNamesComboBox.setEnabled(bEnable);
		if (bEnable)
			displayUpdate(); 
		else
			displayOFF();
	}

	void selectKymograph(int isel) {
		int icurrent = kymographNamesComboBox.getSelectedIndex();
		if (icurrent != isel) {
			kymographNamesComboBox.setSelectedIndex(isel);
			firePropertyChange("KYMOS_DISPLAY_UPDATE", false, true);
		}
	}

	@Override
	public void viewerActivated(Viewer viewer) {
	}

	@Override
	public void viewerDeactivated(Viewer viewer) {
		if (viewer != null) {
			Sequence seq = viewer.getSequence();
			if (seq != null)
				seq.setSelectedROI(null);
		}
	}

	@Override
	public void activeViewerChanged(ViewerEvent event) {		
	}
}
