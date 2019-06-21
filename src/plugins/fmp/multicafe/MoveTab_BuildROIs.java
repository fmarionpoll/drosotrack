package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.roi.ROI;
import icy.roi.ROI2D;
import plugins.fmp.tools.Tools;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class MoveTab_BuildROIs extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5257698990389571518L;
	private JButton 	addPolygon2DButton 		= new JButton("Draw Polygon2D");
	private JButton createROIsFromPolygonButton = new JButton("Create/add (from Polygon 2D)");
	private JTextField nbcagesTextField 	= new JTextField("10");
	private JTextField width_cageTextField 	= new JTextField("10");
	private JTextField width_intervalTextField = new JTextField("2");
	private int 	nbcages 				= 10;
	private int 	width_cage 				= 10;
	private int 	width_interval 			= 2;


	private Multicafe parent0;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		
		add( GuiUtil.besidesPanel(addPolygon2DButton, createROIsFromPolygonButton));
		JLabel ncagesLabel = new JLabel("N cages ");
		JLabel cagewidthLabel = new JLabel("cage width ");
		JLabel btwcagesLabel = new JLabel("between cages ");
		ncagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		cagewidthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		btwcagesLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		add( GuiUtil.besidesPanel( ncagesLabel, nbcagesTextField, cagewidthLabel,  width_cageTextField));
		add( GuiUtil.besidesPanel( btwcagesLabel, width_intervalTextField, new JLabel(" "), new JLabel(" ") ));
		
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		
		createROIsFromPolygonButton.addActionListener(this);
		addPolygon2DButton.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == createROIsFromPolygonButton)  {
			addROISCreatedFromSelectedPolygon();
		}
		else if ( o == addPolygon2DButton) {
			create2DPolygon();
		}
	}
	
	public void updateFromSequence() {
		int nrois = parent0.vSequence.cages.cageLimitROIList.size();	
		if (nrois > 0) {
			nbcagesTextField.setText(Integer.toString(nrois));
			nbcages = nrois;
		}
	}

	private void create2DPolygon() {
		
		final String dummyname = "perimeter_enclosing_capillaries";
		ArrayList<ROI2D> listRois = parent0.vSequence.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName() .equals(dummyname))
				return;
		}

		Rectangle rect = parent0.vSequence.getBounds2D();
		List<Point2D> points = new ArrayList<Point2D>();
		points.add(new Point2D.Double(rect.x + rect.width /6, rect.y + rect.height *2/3));
		points.add(new Point2D.Double(rect.x + rect.width*5 /6, rect.y + rect.height *2/3));
		points.add(new Point2D.Double(rect.x + rect.width*5 /6, rect.y + rect.height - 4));
		points.add(new Point2D.Double(rect.x + rect.width /6, rect.y + rect.height - 4 ));
		ROI2DPolygon roi = new ROI2DPolygon(points);
		roi.setName(dummyname);
		parent0.vSequence.addROI(roi);
		parent0.vSequence.setSelectedROI(roi);
	}
		
	private void addROISCreatedFromSelectedPolygon() {
		// read values from text boxes
		try { 
			nbcages = Integer.parseInt( nbcagesTextField.getText() );
			width_cage = Integer.parseInt( width_cageTextField.getText() );
			width_interval = Integer.parseInt( width_intervalTextField.getText() );
		}catch( Exception e ) { new AnnounceFrame("Can't interpret one of the ROI parameters value"); }

		ROI2D roi = parent0.vSequence.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame for the cages must be a ROI2D POLYGON");
			return;
		}
		Polygon roiPolygon = Tools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		parent0.vSequence.removeROI(roi);

		// generate cage frames
		int span = nbcages*width_cage + (nbcages-1)*width_interval;
		String cageRoot = "cage";
		int iRoot = -1;
		for (ROI iRoi: parent0.vSequence.getROIs()) {
			if (iRoi.getName().contains(cageRoot)) {
				String left = iRoi.getName().substring(4);
				int item = Integer.parseInt(left);
				iRoot = Math.max(iRoot, item);
			}
		}
		iRoot++;

		for (int i=0; i< nbcages; i++) {
			List<Point2D> points = new ArrayList<>();
			double span0 = (width_cage+ width_interval)*i;
			double xup = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * span0 /span;
			double yup = roiPolygon.ypoints[0] +  (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * span0 /span;
			Point2D.Double point0 = new Point2D.Double (xup, yup);
			points.add(point0);

			xup = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * span0 /span ;
			yup = roiPolygon.ypoints[1] +  (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span0 /span ;
			Point2D.Double point1 = new Point2D.Double (xup, yup);
			points.add(point1);

			double span1 = span0 + width_cage ;

			xup = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) *span1 /span;
			yup = roiPolygon.ypoints[1]+  (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) *span1 /span;
			Point2D.Double point4 = new Point2D.Double (xup, yup);
			points.add(point4);

			xup = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) *span1 /span;
			yup = roiPolygon.ypoints[0]+  (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) *span1 /span;
			Point2D.Double point3 = new Point2D.Double (xup, yup);
			points.add(point3);

			ROI2DPolygon roiP = new ROI2DPolygon (points);
			roiP.setName(cageRoot+String.format("%03d", iRoot));
			iRoot++;
			parent0.vSequence.addROI(roiP);
		}

		parent0.vSequence.cages.getCagesFromSequence(parent0.vSequence);
	}

}
