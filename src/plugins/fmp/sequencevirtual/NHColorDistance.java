package plugins.fmp.sequencevirtual;


/**
 * The Class ColorDistance.
 * 
 * @author Nicolas HERVE - nicolas.herve@pasteur.fr
 */
public abstract class NHColorDistance implements NHDistance<double[]> {
	
	/* (non-Javadoc)
	 * @see plugins.nherve.toolbox.image.feature.Distance#computeDistance(java.lang.Object, java.lang.Object)
	 */
	public abstract double computeDistance(double[] c1, double[] c2);
	
	/**
	 * Gets the max distance.
	 * 
	 * @return the max distance
	 */
	public double getMaxDistance() {
		return computeDistance(new double[] { 0.0, 0.0, 0.0 }, new double[] { 255.0, 255.0, 255.0 });
	}
}



