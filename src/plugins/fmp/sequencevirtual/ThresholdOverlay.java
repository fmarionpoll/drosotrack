package plugins.fmp.sequencevirtual;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.point.Point5D;

public class ThresholdOverlay extends Overlay
{
	
	public boolean [] boolMap;
	private IcyBufferedImage binaryMap = null;
	boolean thresholdedImage = true;
	int threshold = 0;
	int transf = 0;
	SequenceVirtual vinputSequence 	= null;
	public ImageTransform imgTransf = new ImageTransform(); 
	public int imageTransformSelected = 0;
	
	public ThresholdOverlay()
	{
		super("ThresholdOverlay: where is this message displayed anyway?");
	}
	
	public void setThresholdOverlayParameters (SequenceVirtual sseq, boolean sthresholded, int sthreshold, int stransf)
	{
		thresholdedImage = sthresholded;
		threshold = sthreshold;
		transf = stransf;
		vinputSequence = sseq;
		imgTransf.setSequence(sseq);
	}

	public void getBinaryOverThresholdFromDoubleImage(IcyBufferedImage img, int threshold) {
		
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
	
	void convertBoolMapIntoBinaryMap() {

		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);
		byte val;
		for (int i= 0; i< binaryMapDataBuffer.length; i++)
		{
			val = (byte) 0xFF;
			if (boolMap[i])
				val = 0;
			binaryMapDataBuffer[i] = val;
		}
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
			if (thresholdedImage) {

				IcyBufferedImage bufImage = imgTransf.transformImage(vinputSequence.currentFrame, transf);
				getBinaryOverThresholdFromDoubleImage(bufImage, threshold);
				convertBoolMapIntoBinaryMap();

				if (boolMap != null) {
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

