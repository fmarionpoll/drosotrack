package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI2D;
import plugins.fmp.sequencevirtual.Capillaries;
import plugins.fmp.tools.Tools;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class MCCapillariesTab_Build extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	
	private JButton 	addPolygon2DButton 		= new JButton("Draw Polygon2D");
	private JButton 	createROIsFromPolygonButton2 = new JButton("Generate ROIs (from Polygon 2D)");
	private JRadioButton selectGroupedby2Button = new JRadioButton("grouped by 2");
	private JRadioButton selectRegularButton 	= new JRadioButton("evenly spaced");
	private ButtonGroup buttonGroup2 			= new ButtonGroup();
	private JTextField 	nbcapillariesTextField 	= new JTextField("20");
	private JTextField 	width_between_capillariesTextField = new JTextField("30");
	private JTextField 	width_intervalTextField = new JTextField("53");
	private Multicafe parent0;
	
	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		
		add( GuiUtil.besidesPanel( addPolygon2DButton, createROIsFromPolygonButton2));
		buttonGroup2.add(selectGroupedby2Button);
		buttonGroup2.add(selectRegularButton);
		selectGroupedby2Button.setSelected(true);
		add( GuiUtil.besidesPanel( 
				new JLabel ("N capillaries ", SwingConstants.RIGHT),  
				nbcapillariesTextField, 
				selectRegularButton, 
				selectGroupedby2Button)); 
		add( GuiUtil.besidesPanel( 
				new JLabel("Pixels btw. caps ", SwingConstants.RIGHT), 
				width_between_capillariesTextField, 
				new JLabel("btw. groups ", SwingConstants.RIGHT), 
				width_intervalTextField ) );
		
		defineActionListeners();
		this.parent0 = parent0;
	}
	
	private void defineActionListeners() {
		addPolygon2DButton.addActionListener(this);
		createROIsFromPolygonButton2.addActionListener(this);
		selectRegularButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == createROIsFromPolygonButton2)  {
			roisGenerateFromPolygon();
			parent0.vSequence.capillaries.extractLinesFromSequence(parent0.vSequence);
			firePropertyChange("CAPILLARIES_NEW", false, true);	
		}
		else if ( o == selectRegularButton) {
			boolean status = false;
			width_between_capillariesTextField.setEnabled(status);
			width_intervalTextField.setEnabled(status);	
		}
		else if (o == addPolygon2DButton) {
			create2DPolygon();
		}
	}
	
	// set/ get	
	private void setNbCapillaries(int nrois) {
		nbcapillariesTextField.setText(Integer.toString(nrois));
	}
	private int getNbCapillaries( ) {
		return Integer.parseInt( nbcapillariesTextField.getText() );
	}

	private int getWidthSmallInterval ( ) {
		return Integer.parseInt( width_between_capillariesTextField.getText() );
	}
	
	private int getWidthLongInterval() {
		return Integer.parseInt( width_intervalTextField.getText() );
	}
	
	private boolean getGroupedBy2() {
		return selectGroupedby2Button.isSelected();
	}
	
	private void setGroupedBy2(boolean flag) {
		selectGroupedby2Button.setSelected(flag);
		selectRegularButton.setSelected(!flag);
	}
	
	void setCapillariesInfos(Capillaries cap) {
		setNbCapillaries(cap.capillariesArrayList.size());
		setGroupedBy2(cap.grouping == 2);
	}
	
	Capillaries getCapillariesInfos(Capillaries cap) {
		//cap.capillariesArrayList.Setsize() getNbCapillaries();
		cap.grouping = getGroupedBy2() ? 2: 1;
		return cap;
	}

	// ---------------------------------
	private void create2DPolygon() {
		
		final String dummyname = "perimeter_enclosing_capillaries";
		ArrayList<ROI2D> listRois = parent0.vSequence.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName() .equals(dummyname))
				return;
		}
		
		Rectangle rect = parent0.vSequence.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		points.add(new Point2D.Double(rect.x + rect.width /5, rect.y + rect.height /5));
		points.add(new Point2D.Double(rect.x + rect.width*4 /5, rect.y + rect.height /5));
		points.add(new Point2D.Double(rect.x + rect.width*4 /5, rect.y + rect.height*2 /3));
		points.add(new Point2D.Double(rect.x + rect.width /5, rect.y + rect.height *2 /3));
		ROI2DPolygon roi = new ROI2DPolygon(points);
		roi.setName(dummyname);
		parent0.vSequence.addROI(roi);
		parent0.vSequence.setSelectedROI(roi);
	}
	
	private void roisGenerateFromPolygon() {

		boolean statusGroup2Mode = false;
		if (getGroupedBy2()) statusGroup2Mode = true;
		// read values from text boxes
		int nbcapillaries = 20;
		int width_between_capillaries = 1;	// default value for statusGroup2Mode = false
		int width_interval = 0;				// default value for statusGroup2Mode = false

		try { 
			nbcapillaries = getNbCapillaries();
			if(statusGroup2Mode) {
				width_between_capillaries = getWidthSmallInterval();
				width_interval = getWidthLongInterval();
			}

		}catch( Exception e ) { new AnnounceFrame("Can't interpret one of the ROI parameters value"); }

		ROI2D roi = parent0.vSequence.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI2D POLYGON");
			return;
		}
		
		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
			
		// clear Rois from sequence
		parent0.vSequence.removeROI(roi);

		// generate lines from polygon frame
		if (statusGroup2Mode) {	
			double span = (nbcapillaries/2)* (width_between_capillaries + width_interval) - width_interval;
			for (int i=0; i< nbcapillaries; i+= 2) {
				double span0 = (width_between_capillaries + width_interval)*i/2;
				double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				if (x < 0) 
					x= 0;
				if (y < 0) 
					y=0;				
				Point2D.Double point0 = new Point2D.Double (x, y);
				x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
				y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span ;
				// TODO: test here if out of bound
				Point2D.Double point1 = new Point2D.Double (x, y);
				ROI2DLine roiL1 = new ROI2DLine (point0, point1);
				roiL1.setName("line"+i/2+"L");
				roiL1.setReadOnly(false);
				parent0.vSequence.addROI(roiL1, true);

				span0 += width_between_capillaries ;
				x = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				y = roiPolygon.ypoints[0]+ (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				if (x < 0) 
					x= 0;
				if (y < 0) 
					y=0;				
				Point2D.Double point3 = new Point2D.Double (x, y);
				x = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span;
				y = roiPolygon.ypoints[1]+ (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * span0 /span;;
				Point2D.Double point4 = new Point2D.Double (x, y);
				ROI2DLine roiL2 = new ROI2DLine (point3, point4);
				roiL2.setName("line"+i/2+"R");
				roiL2.setReadOnly(false);
				parent0.vSequence.addROI(roiL2, true);
			}
		}
		else {
			double span = nbcapillaries-1;
			for (int i=0; i< nbcapillaries; i++) {
				double span0 = width_between_capillaries*i;
				double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
				double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
				Point2D.Double point0 = new Point2D.Double (x, y);
				x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
				y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span0 /span ;
				Point2D.Double point1 = new Point2D.Double (x, y);
				ROI2DLine roiL1 = new ROI2DLine (point0, point1);				
				roiL1.setName("line"+i);
				roiL1.setReadOnly(false);
				parent0.vSequence.addROI(roiL1, true);
			}
		}
	}
}
