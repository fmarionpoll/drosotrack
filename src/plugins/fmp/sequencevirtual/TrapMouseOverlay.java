package plugins.fmp.sequencevirtual;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.type.point.Point5D;
import icy.painter.Overlay;
import icy.painter.OverlayEvent;
import icy.painter.OverlayListener;
import icy.painter.OverlayEvent.OverlayEventType;

import java.awt.event.MouseEvent;


//our painter extends AbstractPainter as it provides painter facilities
public class TrapMouseOverlay extends Overlay
{
    Point5D.Double Pt;

    public TrapMouseOverlay()
    {
        super("Simple overlay");
    }
    
    public TrapMouseOverlay(OverlayListener listener) {
    	super("Simple overlay");
    	addOverlayListener(listener);
    }

    @Override
    public void mouseClick(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
    {
        // check if we are dealing with a 2D canvas and we have a valid image position
        if ((canvas instanceof IcyCanvas2D) && (imagePoint != null))
        {
            Pt = imagePoint;
            OverlayEvent event = new OverlayEvent(this, OverlayEventType.PROPERTY_CHANGED, "click");          
            this.fireOverlayChangedEvent(event);
            remove();
        }
    }

    @Override
    public void mouseMove(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
    {
        // check if we are dealing with a 2D canvas
        if ((canvas instanceof IcyCanvas2D) && (imagePoint != null))
        {
            Pt = imagePoint;
            OverlayEvent event = new OverlayEvent(this, OverlayEventType.PROPERTY_CHANGED, "move");          
            this.fireOverlayChangedEvent(event);        
        }
    }
    
    public Point5D.Double getClickPoint() {
    	return Pt;
    }
}
