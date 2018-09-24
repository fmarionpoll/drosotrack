package plugins.fmp.sequencevirtual;

/**
 * Class NH L1ColorDistance.
 * @author Nicolas HERVE
 */
public class NHL1ColorDistance extends NHColorDistance {

	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.ColorDistance#computeDistance(double[], double[])
	 */
	@Override
	public double computeDistance(double[] c1, double[] c2) {
		double dr = c1[0] - c2[0];
		double dg = c1[1] - c2[1];
		double db = c1[2] - c2[2];

		return Math.abs(dr) + Math.abs(dg) + Math.abs(db);
	}

}
