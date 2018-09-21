package plugins.fmp.sequencevirtual;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageThresholdTools;
import plugins.fmp.sequencevirtual.ImageTransformTools;

public class OverlayThreshold extends Overlay
{
	public IcyBufferedImage binaryMap;
	
	private ImageTransformTools imgTransf = null;
	private ImageThresholdTools imgThresh = null;
	private SequenceVirtual vinputSequence 	= null;
	private TransformOp transformop;
	private ThresholdType thresholdtype = ThresholdType.SINGLE;
	private float opacity = 0.3f;

	// ---------------------------------------------
	
	public OverlayThreshold()
	{
		super("ThresholdOverlay");
	}
	
	public void setThresholdSequence (SequenceVirtual sseq)
	{
		this.vinputSequence = sseq; 
		this.imgTransf = new ImageTransformTools();
		this.imgThresh = new ImageThresholdTools();
		this.imgTransf.setSequenceOfReferenceImage(sseq);
	}
	
	public void setThresholdOverlayParameters (int sthreshold, TransformOp stransf)
	{
		imgThresh.setThresholdOverlayParameters(sthreshold, stransf);
		this.transformop = stransf;
	}
	
	public void setThresholdOverlayParametersColors (ThresholdType thresholdtype, int distanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		imgThresh.setThresholdOverlayParametersColors(distanceType, colorthreshold, colorarray);
		this.thresholdtype = thresholdtype;
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		// check if we are dealing with a 2D canvas and we have a valid Graphics object
		if ((canvas instanceof IcyCanvas2D) && (g != null))
		{
			IcyBufferedImage workImage = imgTransf.transformImageFromSequence(vinputSequence.currentFrame, transformop);
			if (workImage == null)
				return;
			if (thresholdtype == ThresholdType.COLORARRAY)
				binaryMap = imgThresh.getBinaryInt_FromColorsThreshold(workImage);
			else 
				binaryMap = imgThresh.getBinaryInt_FromThreshold(workImage);
			
			OverlayThresholdMap map = new OverlayThresholdMap ("", new Color(0xFFFF0000, true));
			binaryMap.setColorMap(0, map);
			BufferedImage bufferedImage = IcyBufferedImageUtil.getARGBImage(binaryMap);
			if (bufferedImage != null) {								
				Composite bck = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
				g.drawImage(bufferedImage, 0, 0, null);
				g.setComposite(bck);

			}
		}
	}

}
/*
 * 		if (needRedraw) {
			if (cache == null) {
				cache = createCache(getColor());
			} else {
				fillCache(getColor(), cache);
			}
			needRedraw = false;
		}
		Composite bck = g.getComposite();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getOpacity()));
		g.drawImage(cache, 0, 0, null);
		g.setComposite(bck);
 * */
