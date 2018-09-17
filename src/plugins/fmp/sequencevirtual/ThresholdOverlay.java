package plugins.fmp.sequencevirtual;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.fmp.sequencevirtual.ThresholdImage.ThresholdType;
import plugins.fmp.sequencevirtual.ThresholdImage;
import plugins.fmp.sequencevirtual.ImageTransformTools;

public class ThresholdOverlay extends Overlay
{
	public IcyBufferedImage binaryMap;
	
	private ImageTransformTools imgTransf = null;
	private ThresholdImage imgThresh = null;
	private SequenceVirtual vinputSequence 	= null;
	private TransformOp transformop;
	private ThresholdType thresholdtype = ThresholdType.SINGLE;

	// ---------------------------------------------
	
	public ThresholdOverlay()
	{
		super("ThresholdOverlay");
	}
	
	public void setThresholdSequence (SequenceVirtual sseq)
	{
		this.vinputSequence = sseq; 
		this.imgTransf = new ImageTransformTools();
		this.imgThresh = new ThresholdImage();
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
			final Graphics2D g2 = (Graphics2D) g.create();
			g2.setStroke(new BasicStroke(0.3f));
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

			IcyBufferedImage workImage = imgTransf.transformImageTFromSequence(vinputSequence.currentFrame, transformop);
			if (thresholdtype == ThresholdType.COLORARRAY)
				binaryMap = imgThresh.filter1(workImage);
			else 
				binaryMap = imgThresh.getBinaryOverThresholdFromDoubleImage(workImage);

			if (binaryMap != null) {
				g2.drawImage(IcyBufferedImageUtil.toBufferedImage(binaryMap, null), null, 0, 0);
			}
		}

	}


}

