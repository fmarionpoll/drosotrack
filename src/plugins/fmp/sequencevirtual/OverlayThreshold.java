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

public class OverlayThreshold extends Overlay
{
	private ImageOperations imgOp = null;
	private float opacity = 0.3f;
	private OverlayColorMask map = new OverlayColorMask ("", new Color(0x00FF0000, true));
	
	// ---------------------------------------------
	
	public OverlayThreshold()
	{
		super("ThresholdOverlay");	
	}
	
	public OverlayThreshold(SequenceVirtual seq) {
		super("ThresholdOverlay");
		setSequence(seq);
	}
	
	public void setSequence (SequenceVirtual seq)
	{
		if (seq == null)
			return;
		if (imgOp == null)
			imgOp = new ImageOperations (seq);
		else
			imgOp.setSequence(seq);
	}
	
	public void setTransform (TransformOp transf) {
		imgOp.setTransform( transf);
	}
	
	public void setThreshold (ThresholdType thresholdtype, int threshold)
	{
		imgOp.setThreshold(thresholdtype, threshold);
	}
	
	public void setThreshold (ThresholdType thresholdtype, ArrayList <Color> colorarray, int distancetype, int threshold)
	{
		imgOp.setThreshold(thresholdtype, colorarray, distancetype, threshold);
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		// check if we are dealing with a 2D canvas and we have a valid Graphics object
		if ((canvas instanceof IcyCanvas2D) && (g != null))
		{
			IcyBufferedImage thresholdedImage = imgOp.run();
			if (thresholdedImage != null) {
				thresholdedImage.setColorMap(0, map);
				BufferedImage bufferedImage = IcyBufferedImageUtil.getARGBImage(thresholdedImage);
				Composite bck = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
				g.drawImage(bufferedImage, 0, 0, null);
				g.setComposite(bck);

			}
		}
	}

}

