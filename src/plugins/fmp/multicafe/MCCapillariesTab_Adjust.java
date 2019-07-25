package plugins.fmp.multicafe;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.multicafeTools.Line2DPlus;
import plugins.kernel.roi.roi2d.ROI2DLine;


public class MCCapillariesTab_Adjust extends JPanel implements ActionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1756354919434057560L;
	
	JTextField			jitterTextField2	= new JTextField("10");
	private JButton 	adjustButton 		= new JButton("Align");
	private Multicafe 	parent0				= null;
	private Line2D		refLineUpper 		= null;
	private Line2D  	refLineLower 		= null;
	private ROI2DLine	roiRefLineUpper 	= new ROI2DLine ();
	private ROI2DLine	roiRefLineLower 	= new ROI2DLine ();

	void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		add( GuiUtil.besidesPanel(
				new JLabel(" "), 
				new JLabel("jitter ", SwingConstants.RIGHT), 
				jitterTextField2, 
				adjustButton));
		
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		adjustButton.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == adjustButton)  {
			roisCenterLinestoCapillaries();
		}
	}

	// -------------------------------------------------------
	private void roisCenterLinestoCapillaries() {
		
		if (parent0.vSequence.capillaries.capillariesArrayList == null || parent0.vSequence.capillaries.capillariesArrayList.size() == 0)
			return;

		refLineUpper = roiRefLineUpper.getLine();
		refLineLower = roiRefLineLower.getLine(); 
		
		int chan = 0;
		int jitter = Integer.parseInt( jitterTextField2.getText() );
		int t = parent0.vSequence.currentFrame;
		parent0.vSequence.setCurrentVImage(t);
		IcyBufferedImage vinputImage = parent0.vSequence.getImage(t, 0, chan) ;
		if (vinputImage == null) {
			System.out.println("An error occurred while reading image: " + t );
			return;
		}
		int xwidth = vinputImage.getSizeX();
		double [] sourceValues = Array1DUtil.arrayToDoubleArray(vinputImage.getDataXY(0), vinputImage.isSignedDataType());
		
		// loop through all lines
		for (int i=0; i< parent0.vSequence.capillaries.capillariesArrayList.size(); i++) {
			ROI2D roi = parent0.vSequence.capillaries.capillariesArrayList.get(i);
			if (roi instanceof ROI2DLine) 			{
				Line2D line = roisCenterLinetoCapillary(sourceValues, xwidth, (ROI2DLine) roi, jitter);
				((ROI2DLine) roi).setLine(line); // replace with the 5 following lines 
//				List <Anchor2D> pts = ((ROI2DLine) roi).getControlPoints();
//				Anchor2D p1 = pts.get(0);
//				Anchor2D p2 = pts.get(1);
//				p1.setPosition(line.getP1());
//				p2.setPosition(line.getP2());
			}
		}
	}
	
	private Line2D roisCenterLinetoCapillary(double [] sourceValues, int xwidth, ROI2DLine roi, int jitter) {
		
		Line2DPlus line = new Line2DPlus ();
		line.setLine(roi.getLine());
		
		//----------------------------------------------------------
		//  upper position (according to refBar)
		if (!refLineUpper.intersectsLine(line))
			return null;
		
		Point2D.Double pti = line.getIntersection(refLineUpper);
		double y = pti.getY();
		double x = pti.getX();
		
		int lowx = (int) x - jitter;
		if (lowx<0) 
			lowx= 0;
		int ixa = (int) x;
		int iya = (int) y;
		double sumVala = 0;
		double [] arrayVala = new double [2*jitter +1];
		int iarray = 0;
		for (int ix=lowx; ix<=(lowx+2*jitter); ix++, iarray++) {
			arrayVala[iarray] = sourceValues[iya*xwidth + ix];
			sumVala += arrayVala[iarray];
		}
		double avgVala = sumVala / (double) (2*jitter+1);
		
		// find first left < avg
		int ilefta = 0;
		for (int i=0; i< 2*jitter; i++) {
			if (arrayVala[i] < avgVala) {
				ilefta = i;
				break;
			}
		}
		
		// find first right < avg
		int irighta = 2*jitter;
		for (int i=irighta; i >= 0; i--) {
			if (arrayVala[i] < avgVala) {
				irighta = i;
				break;
			}
		}
		if (ilefta > irighta)
			return null;
		int index = (ilefta+irighta)/2;
		ixa = lowx + index;
		
		// find lower position 
		if (!refLineLower.intersectsLine(line))
			return null;
		pti = line.getIntersection(refLineLower);
		y = pti.getY();
		x = pti.getX();

		lowx = (int) x - jitter;
		if (lowx<0) 
			lowx= 0;
		int ixb = (int) x;
		int iyb = (int) y;
		
		double sumValb = 0;
		double [] arrayValb = new double [2*jitter +1];
		iarray = 0;
		for (int ix=lowx; ix<=(lowx+2*jitter); ix++, iarray++) {
			arrayValb[iarray] = sourceValues[iyb*xwidth + ix];
			sumValb += arrayValb[iarray];
		}
		double avgValb = sumValb / (double) (2*jitter+1);
		
		// find first left < avg
		int ileftb = 0;
		for (int i=0; i< 2*jitter; i++) {
			if (arrayValb[i] < avgValb) {
				ileftb = i;
				break;
			}
		}
		// find first right < avg
		int irightb = 2*jitter;
		for (int i=irightb; i >= 0; i--) {
			if (arrayValb[i] < avgValb) {
				irightb = i;
				break;
			}
		}
		if (ileftb > irightb)
			return null;
		
		index = (ileftb+irightb)/2;
		ixb = lowx + index;
		
		// store result
		double y1 = line.getY1();
		double y2 = line.getY2();
		line.x1 = (double) ixa;
		line.y1 = (double) iya;
		line.x2 = (double) ixb;
		line.y2 = (double) iyb;
		double x1 = line.getXfromY(y1);
		double x2 = line.getXfromY(y2);
		Line2D line_out = new Line2D.Double(x1, y1, x2, y2);

		return line_out;
	}

	void roisDisplayrefBar(boolean display) {
		if (parent0.vSequence == null || parent0.vSequence.capillaries.capillariesArrayList.size() == 0)
			return;
		
		if (display)
		{
			// take as ref the whole image otherwise, we won't see the lines if the use has not defined any capillaries
			int seqheight = parent0.vSequence.getHeight();
			int seqwidth = parent0.vSequence.getWidth();
			refLineUpper = new Line2D.Double (0, seqheight/3, seqwidth, seqheight/3);
			refLineLower = new Line2D.Double (0, 2*seqheight/3, seqwidth, 2*seqheight/3);
			
			Rectangle extRect = new Rectangle (parent0.vSequence.capillaries.capillariesArrayList.get(0).getBounds());
			for (ROI2D roi: parent0.vSequence.capillaries.capillariesArrayList)
			{
				Rectangle rect = roi.getBounds();
				extRect.add(rect);
			}
			extRect.grow(extRect.width*1/10, -extRect.height*2/10);
			refLineUpper.setLine(extRect.getX(), extRect.getY(), extRect.getX()+extRect.getWidth(), extRect.getY());
			refLineLower.setLine(extRect.getX(), extRect.getY()+extRect.getHeight(), extRect.getX()+extRect.getWidth(), extRect.getY()+extRect.getHeight());

			
			roiRefLineUpper.setLine(refLineUpper);
			roiRefLineLower.setLine(refLineLower);
			
			roiRefLineUpper.setName("refBarUpper");
			roiRefLineUpper.setColor(Color.YELLOW);
			roiRefLineLower.setName("refBarLower");
			roiRefLineLower.setColor(Color.YELLOW);
			
			parent0.vSequence.addROI(roiRefLineUpper);
			parent0.vSequence.addROI(roiRefLineLower);
		}
		else 
		{
			parent0.vSequence.removeROI(roiRefLineUpper);
			parent0.vSequence.removeROI(roiRefLineLower);
		}
	}
}
