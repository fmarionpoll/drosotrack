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
		imgTransf.setSequence(sseq);
	}
	
	public void setThresholdOverlayParametersColors (SequenceVirtual sseq, 
			boolean bdisplay, int sthreshold, TransformOp stransf, ThresholdType thresholdtype,
			int distanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		this.bTthresholdedImage = bdisplay;
		this.thresholdValue = sthreshold;
		this.transformop = stransf;
		this.vinputSequence = sseq;
		
		imgTransf.setSequence(sseq);
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

		ArrayList<double[]> csColors = new ArrayList<double[]>();
		for (int k = 0; k < colorarray.size(); k++) {
			csColors.add(ColorSpaceTools.getColorComponentsD_0_255(choosenCS, 
					colorarray.get(k).getRed(), colorarray.get(k).getGreen(), colorarray.get(k).getBlue()));
		}

		ColorDistance distance; 
		if (distanceType == 1)
			distance = new L1ColorDistance();
		else
			distance = new L2ColorDistance();
		
		int chan = 0;
		byte[] binaryResultBuffer = binaryMap.getDataXYAsByte(chan);

		int idx = 0;
		for (int j = 0; j < sourceImage.getHeight(); j++) {
			for (int i = 0; i < sourceImage.getWidth(); i++) {
				double[] cc = ColorSpaceTools.getColorComponentsD_0_255(sourceImage, choosenCS, i, j);
				byte val = byteFALSE;
				for (int k = 0; k < colorarray.size(); k++) {
					if (distance.computeDistance(cc, csColors.get(k)) < colorthreshold) {
						val = byteTRUE;
						break;
					}
				}
				binaryResultBuffer[idx] = val;
				idx++;
			}
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

				IcyBufferedImage bufImage = imgTransf.transformImage(vinputSequence.currentFrame, transformop);
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

