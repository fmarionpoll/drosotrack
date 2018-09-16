package plugins.fmp.sequencevirtual;

/**
 * The Class L2ColorDistance.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public class NHL2ColorDistance extends NHColorDistance {

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.ColorDistance#computeDistance(double[], double[])
	 */
	@Override
	public double computeDistance(double[] c1, double[] c2) {
		double dr = c1[0] - c2[0];
		double dg = c1[1] - c2[1];
		double db = c1[2] - c2[2];

		return Math.sqrt(dr * dr + dg * dg + db * db);
	}

}