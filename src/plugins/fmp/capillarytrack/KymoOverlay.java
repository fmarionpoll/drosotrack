package plugins.fmp.capillarytrack;

import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.painter.Overlay;

public class KymoOverlay extends Overlay {

	public KymoOverlay() {
		super("Kymo Overlay");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void keyPressed(final KeyEvent e, final Point2D imagePoint, final IcyCanvas canvas) {

	}

	@Override
	public void keyReleased(final KeyEvent e, final Point2D imagePoint, final IcyCanvas canvas)
	{
		if (!(canvas instanceof Canvas2D)) return;

		Canvas2D cv = (Canvas2D) canvas;
		int x = cv.getOffsetX();
		int y = cv.getOffsetY();
		Rectangle2D rect = cv.getImageVisibleRect();

		if (e.getKeyChar() == 'd'|| e.getKeyChar()=='D' )
		{
			x += (int) rect.getWidth();
		}
		else if (e.getKeyChar() == 's'|| e.getKeyChar()=='S' )
		{
			x -= (int) rect.getWidth();
		}
		else if (e.getKeyChar() == 'e' || e.getKeyChar()=='E' )
		{
			y -= (int) rect.getHeight();
		}
		else if (e.getKeyChar() == 'x'|| e.getKeyChar()=='X' )
		{
			y += (int) rect.getHeight();
		}

		cv.setOffset(x, y, true);
	}
}
