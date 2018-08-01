package plugins.fmp.sequencevirtual;

import java.awt.Color;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;

public class ImageTransform {

	public enum TransformOp { None("none"),
		RGBall("RGB"), R_RGB("R(RGB)"), G_RGB("G(RGB)"), B_RGB("B(RGB)"), Grey_RGB("Grey (R+G+B)/3"), 
		GBmR ("(G+B)/2-R"), BRmG("(B+R)/2-G"),
		H_HSB ("H(HSB)"), S_HSB ("S(HSB)"), B_HSB("B(HSB)"),  
		XDIFFN("XDiffn"), XYDIFFN( "XYDiffn"), 
		REFt0("subtract t0"), REFn("subtract n-1"), REF("subtract ref"),
		NORM_BRmG("norm (B+R)/2-G");
		private String label;
		TransformOp (String label) {
			this.label = label;
			}
		public String toString() {
			return label;
			}	
	}

	private IcyBufferedImage referenceImage = null;
	private int spanDiffTop = 3;	// adjust this parameter eventually through user's interface
	//private int	spanDiffTransf2 = 3;
	private	IcyBufferedImage img2;
	private SequenceVirtual vinputSequence 	= null;

	// -------------------------------------

	public void setReferenceImage(IcyBufferedImage img) {
		referenceImage = IcyBufferedImageUtil.getCopy(img);
	}
	
	public void setSequence (SequenceVirtual vinputSeq) {
		vinputSequence = vinputSeq;
		referenceImage = vinputSequence.loadVImage(0);
	}
	
	public IcyBufferedImage transformImage (int t, TransformOp transformop) {
		
		IcyBufferedImage img = vinputSequence.loadVImage(t);
		img2 = IcyBufferedImageUtil.getCopy(img);
		
		switch (transformop) {
		case R_RGB: functionRGB_keepOneChan(img, 0); break;
		case G_RGB: functionRGB_keepOneChan(img, 1); break;
		case B_RGB: functionRGB_keepOneChan(img, 2); break;
		case Grey_RGB: functionRGB_grey(img); break;
		case GBmR: functionRGB_sumC1C2Minus2C3(img, 1, 2, 0); break;
		case BRmG: functionRGB_sumC1C2Minus2C3 (img, 0, 2, 1); break;

		case H_HSB: functionRGB_HSB(img, 0); break;
		case S_HSB: functionRGB_HSB(img, 1); break;
		case B_HSB: functionRGB_HSB(img, 2); break;

		case XDIFFN: computeXDiffn (img); break;
		case XYDIFFN: computeXYDiffn (img); break;

		case REFt0: functionSubtractRef(img); break;
		case REFn: if (t>0) {referenceImage = vinputSequence.loadVImage(t-1); functionSubtractRef(img);} break;
		case REF: functionSubtractRef(img); break;
		
		case NORM_BRmG: functionNormRGB_sumC1C2Minus2C3(img, 1, 2, 0); break;
		
		case None: 
		case RGBall:
			break;
		}
		
		return img2;
	}
		
	// function proposed by François 
	
	private void functionNormRGB_sumC1C2Minus2C3 (IcyBufferedImage sourceImage, int Rlayer, int Glayer, int Blayer) {
 
		double[] Rn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Rlayer), sourceImage.isSignedDataType());
		double[] Gn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Glayer), sourceImage.isSignedDataType());
		double[] Bn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Blayer), sourceImage.isSignedDataType());
		double[] ExG = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		
		ArrayMath.divide (Rn, 255, Rn);
		ArrayMath.divide (Gn, 255, Gn);
		ArrayMath.divide (Bn, 255, Bn);
		
		double[] sum = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		ArrayMath.add (Rn, Gn, sum);
		ArrayMath.add (sum,  Bn, sum);
		
		ArrayMath.divide (Rn, sum, Rn);
		ArrayMath.divide (Gn, sum, Gn);
		ArrayMath.divide (Bn, sum, Bn);

		// compute ExG = 2*g - r - b
		ArrayMath.multiply(Gn, 2, ExG);
		ArrayMath.subtract(ExG, Rn, ExG);
		ArrayMath.subtract(ExG, Bn, ExG);
		
		// from 0 to 255
		ArrayMath.multiply(ExG, 255, ExG);
		
		Array1DUtil.doubleArrayToSafeArray(ExG,  img2.getDataXY(0),  true); //true, img2.isSignedDataType());
	}
	
	private void functionRGB_sumC1C2Minus2C3 (IcyBufferedImage sourceImage, int ADD1, int ADD2, int SUB) {
		 
		double[] tabSubtract = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(SUB), sourceImage.isSignedDataType());
		double[] tabAdd1 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(ADD1), sourceImage.isSignedDataType());
		double[] tabAdd2 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(ADD2), sourceImage.isSignedDataType());
		double[] img2Values = (double[]) Array1DUtil.createArray(DataType.DOUBLE, tabSubtract.length);

		ArrayMath.add(tabAdd1, tabAdd2, img2Values);
		ArrayMath.divide(2, img2Values);
		ArrayMath.subtract(img2Values, tabSubtract, img2Values);
		
		Array1DUtil.doubleArrayToSafeArray(img2Values,  img2.getDataXY(0),  true); //true, img2.isSignedDataType());
		
		// duplicate channel 0 to chan 1 & 2
//		img2.copyData(img2, 0, 1);
//		img2.copyData(img2, 0, 2);
	}
	
	private void computeXDiffn(IcyBufferedImage sourceImage) {

		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();

		for (int c=chan0; c < chan1; c++) {

			double[] tabValues = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			double[] outValues = Array1DUtil.arrayToDoubleArray(img2.getDataXY(c), img2.isSignedDataType());			

			for (int iy = 0; iy < imageSizeY; iy++) {	
				// erase border values
				for (int ix = 0; ix < spanDiffTop; ix++) {
					outValues[ix + iy* imageSizeX] = 0;
				}

				// compute values
				int deltay = iy* imageSizeX;
				for (int ix =spanDiffTop; ix < imageSizeX -spanDiffTop; ix++) {

					int kx = ix + deltay;
					int deltax =  0;
					double outVal = 0;
					for (int ispan = 1; ispan < spanDiffTop; ispan++) {
						deltax += 1; 
						outVal += tabValues [kx+deltax] - tabValues[kx-deltax];
					}
					outValues [kx] = Math.abs(outVal);
				}

				// erase border values
				for (int ix = imageSizeX-spanDiffTop; ix < imageSizeX; ix++) {
					outValues[ix + iy* imageSizeX] = 0;
				}
			}
			Array1DUtil.doubleArrayToSafeArray(outValues, img2.getDataXY(c), img2.isSignedDataType());
		}
	}
	
	private void computeXYDiffn(IcyBufferedImage sourceImage) {

		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();

		for (int c=chan0; c < chan1; c++) {

			double[] tabValues = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			double[] outValues = Array1DUtil.arrayToDoubleArray(img2.getDataXY(c), img2.isSignedDataType());			
			
			for (int ix =0; ix < imageSizeX; ix++) {	

				for (int iy = spanDiffTop; iy < imageSizeY-spanDiffTop; iy++) {

					int ky = ix + iy* imageSizeX;
					int deltay =  0;
					double outVal = 0;
					// loop vertically
					for (int ispan = 1; ispan < spanDiffTop; ispan++) {
						deltay += imageSizeX;
						outVal += tabValues [ky+deltay] - tabValues[ky-deltay];
					}

					// loop horizontally
					int deltax = 0;
					int yspan2 = 10;
					if (ix >yspan2 && ix < imageSizeX - yspan2) {
						for (int ispan = 1; ispan < yspan2; ispan++) {
							deltax += 1;
							outVal += tabValues [ky+deltax] - tabValues[ky-deltax];
						}
					}
					outValues [ky] = Math.abs(outVal);
				}

				// erase out-of-bounds points
				for (int iy = 0; iy < spanDiffTop; iy++) 
					outValues[ix + iy* imageSizeX] = 0;

				for (int iy = imageSizeY-spanDiffTop; iy < imageSizeY; iy++) 
					outValues[ix + iy* imageSizeX] = 0;
			}
			Array1DUtil.doubleArrayToSafeArray(outValues,  img2.getDataXY(c),  img2.isSignedDataType());
		}
	}

	private void functionRGB_keepOneChan (IcyBufferedImage sourceImage, int keepChan) {

		for (int i=0; i<3; i++)
			if (i != keepChan) 
				img2.copyData(img2, keepChan, i);
	}
	
	private void functionRGB_grey (IcyBufferedImage sourceImage) {

		int[] tabValuesR = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		int[] tabValuesG = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		int[] tabValuesB = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		int[] outValues0 = Array1DUtil.arrayToIntArray(img2.getDataXY(0), img2.isSignedDataType());
		
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();

		for (int ix =0; ix < imageSizeX; ix++) {	
			for (int iy = 0; iy < imageSizeY; iy++) {

				int ky = ix + iy* imageSizeX;
				int R = tabValuesR[ky];
				int G = tabValuesG[ky];
				int B = tabValuesB[ky];
				
				int val = (R+G+B)/3;
				outValues0 [ky] = val;
			}
		}
				
		Array1DUtil.intArrayToSafeArray(outValues0,  img2.getDataXY(0),  false, img2.isSignedDataType());
		// duplicate channel 0 to chan 1 & 2
//		img2.copyData(img2, 0, 1);
//		img2.copyData(img2, 0, 2);
	}
	
	private void functionRGB_HSB(IcyBufferedImage sourceImage, int HorSorB) {
		
		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();

		for (int ix =0; ix < imageSizeX; ix++) {	

			// compute values
			for (int iy = 0; iy < imageSizeY; iy++) {

				int ky = ix + iy* imageSizeX;
				int R = (int) tabValuesR[ky];
				int G = (int) tabValuesG[ky];
				int B = (int) tabValuesB[ky];
				
				float[] hsb = Color.RGBtoHSB(R, G, B, null) ;
				double val = (double) hsb[HorSorB] * 100;
				outValues0 [ky] = val;
				outValues1 [ky] = val;
				outValues2 [ky] = val;
			}
		}
		Array1DUtil.doubleArrayToSafeArray(outValues0,  img2.getDataXY(0),  img2.isSignedDataType());
		Array1DUtil.doubleArrayToSafeArray(outValues1,  img2.getDataXY(1),  img2.isSignedDataType());
		Array1DUtil.doubleArrayToSafeArray(outValues2,  img2.getDataXY(2),  img2.isSignedDataType());
	}
	
	private void functionSubtractRef(IcyBufferedImage sourceImage) {
		
		/* algorithm borrowed from  Perrine.Paul-Gilloteaux@univ-nantes.fr in  EC-CLEM
		 * original function: private IcyBufferedImage substractbg(Sequence ori, Sequence bg,int t, int z) 
		 */
		IcyBufferedImage img = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(),sourceImage.getSizeC(), sourceImage.getDataType_());
		double[] dummyzeros=Array1DUtil.arrayToDoubleArray(img.getDataXY(0), img.isSignedDataType());
		
		for (int c=0; c<sourceImage.getSizeC(); c++){
			Object sourceArray = sourceImage.getDataXY(c);
			Object referenceArray = referenceImage.getDataXY(c);
			double[] img1DoubleArray = Array1DUtil.arrayToDoubleArray(sourceArray, sourceImage.isSignedDataType());
			double[] img2DoubleArray = Array1DUtil.arrayToDoubleArray(referenceArray, referenceImage.isSignedDataType());
			ArrayMath.subtract(img1DoubleArray, img2DoubleArray, img1DoubleArray);

			ArrayMath.max(img1DoubleArray, dummyzeros, img1DoubleArray);
			Array1DUtil.doubleArrayToArray(img1DoubleArray, img2.getDataXY(c));
		}
	}
	
}
