package plugins.fmp.sequencevirtual;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.collection.array.Array2DUtil;
import icy.type.point.Point5D;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

import plugins.nherve.toolbox.image.feature.ColorDistance;
import plugins.nherve.toolbox.image.feature.L1ColorDistance;
import plugins.nherve.toolbox.image.feature.L2ColorDistance;
import plugins.nherve.toolbox.image.toolboxes.ColorSpaceTools;

public class ThresholdOverlay extends Overlay
{
	public IcyBufferedImage binaryMap = null;
	public boolean [] boolMap;
	public ImageTransformTools imgTransf = new ImageTransformTools(); 
	public int imageTransformSelected = 0;
	
	private boolean bTthresholdedImage = true;
	private int thresholdValue = 0;
	private TransformOp transformop;
	private SequenceVirtual vinputSequence 	= null;
	private int colorthreshold;
	private ArrayList <Color> colorarray;
	private int distanceType;
		
	public enum ThresholdType { SINGLE ("simple threshold"), COLORARRAY ("Color array");
		private String label;
		ThresholdType (String label) { this.label = label;}
		public String toString() { return label;}	
	}
	
	private ThresholdType thresholdtype = ThresholdType.SINGLE;
	final byte byteFALSE = (byte) 0;
	final byte byteTRUE = (byte) 255;

	// ---------------------------------------------
	
	public ThresholdOverlay()
	{
		super("ThresholdOverlay: where is this message displayed anyway?");
	}
	
	public void setThresholdOverlayParameters (SequenceVirtual sseq, boolean sbThresholded, int sthreshold, TransformOp stransf)
	{
		bTthresholdedImage = sbThresholded;
		thresholdValue = sthreshold;
		transformop = stransf;
		vinputSequence = sseq;
		imgTransf.setSequenceOfReferenceImage(sseq);
	}
	
	public void setThresholdOverlayParametersColors (SequenceVirtual sseq, 
			boolean bdisplay, int sthreshold, TransformOp stransf, ThresholdType thresholdtype,
			int distanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		this.bTthresholdedImage = bdisplay;
		this.thresholdValue = sthreshold;
		this.transformop = stransf;
		this.vinputSequence = sseq;
		
		imgTransf.setSequenceOfReferenceImage(sseq);
		this.thresholdtype = thresholdtype;
		this.colorarray = new ArrayList<Color> (colorarray);
		this.colorthreshold = colorthreshold;
	}

	public void getBinaryOverThresholdFromDoubleImage(IcyBufferedImage img) {
		
		int chan = 0;
		byte[] imageSourceDataBuffer = img.getDataXYAsByte(chan);
		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(chan);
		
		for (int x = 0; x < binaryMapDataBuffer.length; x++)  {
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > thresholdValue)
				binaryMapDataBuffer[x] = byteFALSE;
			else
				binaryMapDataBuffer[x] = byteTRUE;
		}		
	}
	
	public void getBoolMapOverThresholdFromDoubleImage(IcyBufferedImage img, int threshold) {
		
		if (boolMap == null)
			boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		
		if (binaryMap == null) 
			binaryMap = new IcyBufferedImage(img.getSizeX(), img.getSizeY(), 1, DataType.UBYTE);
		
		int chan = 0;
		final byte[] imageSourceDataBuffer = img.getDataXYAsByte(chan);
		for (int x = 0; x < boolMap.length; x++)  {
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > threshold)
				boolMap[x] = false;
			else
				boolMap[x] = true;
		}		
	}

	private void filter1(IcyBufferedImage sourceImage) {

//		ArrayList<double[]> csColors = new ArrayList<double[]>();
//		for (int k = 0; k < colorarray.size(); k++) {
//			csColors.add(ColorSpaceTools.getColorComponentsD_0_255(choosenCS, 
//					colorarray.get(k).getRed(), colorarray.get(k).getGreen(), colorarray.get(k).getBlue()));
//		}
		IcyBufferedImage colorRef = new IcyBufferedImage(colorarray.size(), 1, 3, DataType.DOUBLE);
		for (int k1 = 0; k1 < colorarray.size(); k1++) {
			colorRef.setData(k1, 1, 0, colorarray.get(k1).getRed());
			colorRef.setData(k1, 1, 1, colorarray.get(k1).getGreen());
			colorRef.setData(k1, 1, 2, colorarray.get(k1).getBlue());
		}
		IcyBufferedImage colorRefTransformed = imgTransf.transformImage(colorRef, transformop);
		double[][] csColors = Array2DUtil.arrayToDoubleArray(colorRefTransformed, colorRefTransformed.isSignedDataType());
		double[][] imgColors = Array2DUtil.arrayToDoubleArray(sourceImage, sourceImage.isSignedDataType());
		
		NHColorDistance distance; 
		if (distanceType == 1)
			distance = new NHL1ColorDistance();
		else
			distance = new NHL2ColorDistance();
		
		int chan = 0;
		byte[] binaryResultBuffer = binaryMap.getDataXYAsByte(chan);

		int idx_length = imgColors.length;
		for (int idx = 0; idx < idx_length; idx++) {
			
			byte val = byteFALSE;
			for (int k = 0; k < colorarray.size(); k++) {
				if (distance.computeDistance(imgColors[idx], csColors[idx]) < colorthreshold) {
					val = byteTRUE;
					break;
				}
			}
			binaryResultBuffer[idx] = val;
			
		}
		/*
		Color c = box.getAverageColor();
		int ir = 255 - c.getRed();
		int ig = 255 - c.getGreen();
		int ib = 255 - c.getBlue();
		m.setColor(new Color(ir, ig, ib));
		m.setOpacity(1f);
		*/
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		// check if we are dealing with a 2D canvas and we have a valid Graphics object
		if ((canvas instanceof IcyCanvas2D) && (g != null))
		{
			final Graphics2D g2 = (Graphics2D) g.create();
			g2.setStroke(new BasicStroke(0.3f));
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			if (bTthresholdedImage) {

				IcyBufferedImage bufImage = imgTransf.transformImageTFromSequence(vinputSequence.currentFrame, transformop);
				if (binaryMap == null) 
					binaryMap = new IcyBufferedImage(bufImage.getSizeX(), bufImage.getSizeY(), 1, DataType.UBYTE);

				if (thresholdtype == ThresholdType.COLORARRAY)
					filter1(bufImage);
				else 
					getBinaryOverThresholdFromDoubleImage(bufImage);

				if (binaryMap != null) {
					g2.drawImage(IcyBufferedImageUtil.toBufferedImage(binaryMap, null), null, 0, 0);
				}
			}
		}
	}

	@Override
	public void mouseClick(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
	{
		// check if we are dealing with a 2D canvas
		if (canvas instanceof IcyCanvas2D)
		{
			// remove painter from all sequence where it is attached
			remove();
		}
	}
}

