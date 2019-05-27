package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import plugins.fmp.sequencevirtual.SequencePlus;

public class KymosTab_Filter  extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4413321640245046423L;
	
	private Capillarytrack parent0;
	public JButton 	startButton = new JButton("Start");
	public JTextField spanText 	= new JTextField("10");

	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);	
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel(new JLabel("use N pixels="), spanText, new JLabel(" ")));
		add(GuiUtil.besidesPanel(new JLabel(" "), new JLabel(" "), startButton));
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		startButton.addActionListener(this);
	}

	public int getSpan( ) {
		return Integer.parseInt( spanText.getText() );
	}
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Object o = arg0.getSource();
		if ( o == startButton)  {
			int span = getSpan();
			for (SequencePlus kymoSequence: parent0.kymographArrayList) {
				crossCorrelatePixels(kymoSequence, span);
			}
		}
	}

	private void crossCorrelatePixels (SequencePlus kymographSeq, int span) {
		IcyBufferedImage image = null;
		int c = 0;
		image = kymographSeq.getImage(0, 0, c);
		double[] tabValues = image.getDataXYAsDouble(c);
		int xwidth = image.getSizeX();
		int yheight = image.getSizeY();
		double[] col0 = new double[yheight];
		double[] col1 = new double [yheight];
		int npoints = 2*span;
		int len = yheight-npoints;
		int startx = span;
		double [] correl = new double [npoints];
		int [] ishift = new int [xwidth];
		ishift[0] = 0;
		
		for (int ix = 1; ix<xwidth; ix++) {
			for (int iy = 0; iy < yheight; iy++) {
				col0[iy] = tabValues [ix-1 + iy* xwidth];
				col1[iy] = tabValues [ix + iy* xwidth];
			}
			for (int starty=0; starty<npoints; starty++) {
				correl[starty] = correlationBetween2Arrays (col0, col1, startx, starty, len);
			}
//			String cs = "col "+ix + ": " + Arrays.toString(correl);
//			System.out.println(cs);
			int imax = 0;
			double vmax = correl[0];
			for (int i=1; i<npoints; i++) {
				if (correl[i] > vmax) {
					vmax = correl[i];
					imax = i;
				}
			}
			ishift[ix] = imax-span;
			shiftArray(ishift[ix], ix, tabValues, xwidth, yheight);
		}
		String cs = "image shifts: " + Arrays.toString(ishift);
		System.out.println(cs);
	}
	
	private void shiftArray(int shift, int ix, double[] tabValues, int xwidth, int yheight) {
		int iydest = shift;
		for (int iy = 0; iy < yheight; iy++, iydest++) {
			if (iydest >= 0 && iydest < yheight)
				tabValues [ix + iydest* xwidth] = tabValues [ix + iy* xwidth];
		}

	}
	
	private double correlationBetween2Arrays(double[] xs, double[] ys, int startx, int starty, int len) {
	    double sx = 0.0;
	    double sy = 0.0;
	    double sxx = 0.0;
	    double syy = 0.0;
	    double sxy = 0.0;

	    for(int i = 0; i < len; ++i, startx++, starty++) {
	      double x = xs[startx];
	      double y = ys[starty];

	      sx += x;
	      sy += y;
	      sxx += x * x;
	      syy += y * y;
	      sxy += x * y;
	    }

	    // covariation
	    double cov = sxy / len - sx * sy / len / len;
	    // standard error of x
	    double sigmax = Math.sqrt(sxx / len -  sx * sx / len / len);
	    // standard error of y
	    double sigmay = Math.sqrt(syy / len -  sy * sy / len / len);

	    // correlation is just a normalized covariation
	    return cov / sigmax / sigmay;
	  }
}
