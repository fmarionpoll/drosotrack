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
	//private IcyBufferedImage binaryMap = null;
	private boolean [] boolMap;
	private IcyBufferedImage binaryMap = null;
	private int t_binaryFrame = -1;
	private int t_threshold = -1;
	boolean thresholdedImage = true;
	int threshold = 0;
	int chan = 0;
	int transf = 0;
	SequenceVirtual vinputSequence 	= null;
	
	public ThresholdOverlay()
	{
		super("where is this message displayed?");
	}
	
	public void setThresholdOverlayParameters (SequenceVirtual sseq, boolean sthresholded, int sthreshold, int schan, int stransf)
	{
		thresholdedImage = sthresholded;
		threshold = sthreshold;
		chan = schan;
		transf = stransf;
		vinputSequence = sseq;
	}

	static public boolean[] getBinaryOverThreshold(IcyBufferedImage img, int t, int chan, int threshold, boolean[] maskAll) {
		
		if (maskAll == null)
			maskAll = new boolean[ img.getSizeX() * img.getSizeY() ];
		
		if (chan >= 0) {
			final byte[] imageSourceDataBuffer = img.getDataXYAsByte(chan);
			for (int x = 0; x < maskAll.length; x++)  {
				int val = imageSourceDataBuffer[x] & 0xFF;
				if (val > threshold)
					maskAll[x] = false;
				else
					maskAll[x] = true;
			}
		}
		else if (chan < 0) {
			final byte[] arrayRed = img.getDataXYAsByte(0);
			final byte[] arrayGreen = img.getDataXYAsByte(1);
			final byte[] arrayBlue = img.getDataXYAsByte(2);
			
			for (int x = 0; x < maskAll.length; x++)  
			{
				float red = ( arrayRed[x] & 0xFF );
				float green = ( arrayGreen[x] & 0xFF );
				float blue = ( arrayBlue[x] & 0xFF );
				float val = (red+green+blue)/3f;

				if (val > threshold)
					maskAll[x] = false;
				else
					maskAll[x] = true;	
			}
		}
			
		return maskAll;
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

				if (t_binaryFrame != vinputSequence.getT() || threshold != t_threshold) {

					t_binaryFrame = vinputSequence.getT();
					t_threshold = threshold;
					int z = 0;
					int c = -1;
					IcyBufferedImage bufImage = vinputSequence.getImageTransf(vinputSequence.currentFrame, z, c, transf); 
					boolMap = getBinaryOverThreshold(bufImage, 
							t_binaryFrame, 
							chan, 
							threshold, 
							boolMap);
					
					if (binaryMap == null)
						binaryMap = new IcyBufferedImage(bufImage.getWidth(), bufImage.getHeight(), 1, DataType.UBYTE);
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

