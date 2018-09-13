package plugins.fmp.sequencevirtual;

import java.awt.Color;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;

public class ImageTransform {

	public enum TransformOp { None("none"),
		RGBall("RGB"), R_RGB("R(RGB)"), G_RGB("G(RGB)"), B_RGB("B(RGB)"),  
		GBMINUS2R ("(G+B)-2R"), RBMINUS2G("(R+B)-2G"), RGMINUS2B("(R+G)-2B"),
		RGB ("(R+G+B)/3"),
		H_HSB ("H(HSB)"), S_HSB ("S(HSB)"), B_HSB("B(HSB)"),  
		XDIFFN("XDiffn"), 
//		XYDIFFN( "XYDiffn"), 
		REFt0("subtract t0"), REFn("subtract n-1"), REF("subtract ref"),
		NORM_BRmG("F. Rebaudo");
		private String label;
		TransformOp (String label) {
			this.label = label;
			}
		public String toString() {
			return label;
			}	
	}
	public double factorR=1.;
	public double factorG=1.;
	public double factorB=1.;

	private IcyBufferedImage referenceImage = null;
	private int spanDiff = 3;
	//private int	spanDiffTransf2 = 3;
	private	IcyBufferedImage img2;
	private SequenceVirtual vinputSequence 	= null;

	// -------------------------------------

	public void setReferenceImage(IcyBufferedImage img) {
		referenceImage = IcyBufferedImageUtil.getCopy(img);
	}
	
	public void setSpanDiff(int spanDiff) {
		this.spanDiff = spanDiff;
	}
	
	public int getSpanDiff () {
		return spanDiff;
	}
	
	public void setSequence (SequenceVirtual vinputSeq) {
		vinputSequence = vinputSeq;
		referenceImage = vinputSequence.loadVImage(0);
	}
	
	public IcyBufferedImage transformImage (IcyBufferedImage img, TransformOp transformop) {
		
		img2 = IcyBufferedImageUtil.getCopy(img);
		
		switch (transformop) {
		case R_RGB: functionRGB_keepOneChan(img, 0); break;
		case G_RGB: functionRGB_keepOneChan(img, 1); break;
		case B_RGB: functionRGB_keepOneChan(img, 2); break;
		
		case GBMINUS2R: functionRGB_C1C2Minus2C3 (img, 1, 2, 0); break;
		case RBMINUS2G: functionRGB_C1C2Minus2C3 (img, 0, 2, 1); break;
		case RGMINUS2B: functionRGB_C1C2Minus2C3 (img, 0, 1, 2); break;
		
		case RGB: functionRGB_grey (img);
		
		case H_HSB: functionRGBtoHSB(img, 0); break;
		case S_HSB: functionRGBtoHSB(img, 1); break;
		case B_HSB: functionRGBtoHSB(img, 2); break;

		case XDIFFN: computeXDiffn (img); break;
//		case XYDIFFN: computeXYDiffn (img); break;

		case REFt0: functionSubtractRef(img); break;
		case REFn: //if (t>0) {referenceImage = vinputSequence.loadVImage(t-1); functionSubtractRef(img);} break;
			break;
		case REF: functionSubtractRef(img); break;
		
		case NORM_BRmG: functionNormRGB_sumC1C2Minus2C3(img, 1, 2, 0); break;
		
		case None: 
		case RGBall:
			break;
		}
		return img2;
	}
	
	public IcyBufferedImage transformImage (int t, TransformOp transformop) {
		return transformImage(vinputSequence.loadVImage(t), transformop);
	}
		
	// function proposed by François 
	
	private void functionNormRGB_sumC1C2Minus2C3 (IcyBufferedImage sourceImage, int Rlayer, int Glayer, int Blayer) {
 
		double[] Rn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Rlayer), sourceImage.isSignedDataType());
		double[] Gn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Glayer), sourceImage.isSignedDataType());
		double[] Bn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Blayer), sourceImage.isSignedDataType());
		double[] ExG = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		double[] sum = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		
		ArrayMath.divide (Rn, 255, Rn);		// R = R/255
		ArrayMath.divide (Gn, 255, Gn);		// G = G/255
		ArrayMath.divide (Bn, 255, Bn);		// B = B/255
		
		ArrayMath.add (Rn, Gn, sum);		// sum = R+G
		ArrayMath.add (sum,  Bn, sum);		// sum = R+G+B
		
		ArrayMath.divide (Rn, sum, Rn);		// R = R/sum
		ArrayMath.divide (Gn, sum, Gn);		// G = G/sum
		ArrayMath.divide (Bn, sum, Bn);		// B = B/sum

		// compute ExG = 2*g - r - b
		ArrayMath.multiply(Gn, 2, ExG);		// ExG = 2 * G
		ArrayMath.subtract(ExG, Rn, ExG);	// ExG = 2 * G - R
		ArrayMath.subtract(ExG, Bn, ExG);	// ExG = 2 * G - R - B
		
		// from 0 to 255
		ArrayMath.multiply(ExG, 255, ExG);	// ExG = ExG * 255
		
		Array1DUtil.doubleArrayToSafeArray(ExG,  img2.getDataXY(0),  true); //true, img2.isSignedDataType());
	}
	
	
	private void functionRGB_C1C2Minus2C3 (IcyBufferedImage sourceImage, int addchan1, int addchan2, int subtractchan3) {
		 
		double[] tabSubtract = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(subtractchan3), sourceImage.isSignedDataType());
		double[] tabAdd1 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(addchan1), sourceImage.isSignedDataType());
		double[] tabAdd2 = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(addchan2), sourceImage.isSignedDataType());
		double[] tabResult = (double[]) Array1DUtil.createArray(DataType.DOUBLE, tabSubtract.length);
		double max = -(tabSubtract[1] * 2 - tabAdd1[1] - tabAdd2[1]);
		double min = max;
		for (int i = 0; i < tabResult.length; i++) {	
			tabResult [i] = -(tabSubtract[i]* 2 - tabAdd1[i] - tabAdd2[i]) ;
			double value = tabResult[i];
			if (value > max) max = value;
			if (value < min) min= value;
		}
		
		Array1DUtil.doubleArrayToSafeArray(tabResult,  img2.getDataXY(0),  true); //true, img2.isSignedDataType());
		System.out.println("max" + max + " min=" + min);
		
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
				for (int ix = 0; ix < spanDiff; ix++) {
					outValues[ix + iy* imageSizeX] = 0;
				}

				// compute values
				int deltay = iy* imageSizeX;
				for (int ix =spanDiff; ix < imageSizeX -spanDiff; ix++) {

					int kx = ix + deltay;
					int deltax =  0;
					double outVal = 0;
					for (int ispan = 1; ispan < spanDiff; ispan++) {
						deltax += 1; 
						outVal += tabValues [kx+deltax] - tabValues[kx-deltax];
					}
					outValues [kx] = Math.abs(outVal);
				}

				// erase border values
				for (int ix = imageSizeX-spanDiff; ix < imageSizeX; ix++) {
					outValues[ix + iy* imageSizeX] = 0;
				}
			}
			Array1DUtil.doubleArrayToSafeArray(outValues, img2.getDataXY(c), img2.isSignedDataType());
		}
	}
	
	/*
	private void computeXYDiffn(IcyBufferedImage sourceImage) {

		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();

		for (int c=chan0; c < chan1; c++) {

			double[] tabValues = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			double[] outValues = Array1DUtil.arrayToDoubleArray(img2.getDataXY(c), img2.isSignedDataType());			
			
			for (int ix =0; ix < imageSizeX; ix++) {	

				for (int iy = spanDiff; iy < imageSizeY-spanDiff; iy++) {

					int ky = ix + iy* imageSizeX;
					int deltay =  0;
					double outVal = 0;
					// loop vertically
					for (int ispan = 1; ispan < spanDiff; ispan++) {
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
				for (int iy = 0; iy < spanDiff; iy++) 
					outValues[ix + iy* imageSizeX] = 0;

				for (int iy = imageSizeY-spanDiff; iy < imageSizeY; iy++) 
					outValues[ix + iy* imageSizeX] = 0;
			}
			Array1DUtil.doubleArrayToSafeArray(outValues,  img2.getDataXY(c),  img2.isSignedDataType());
		}
	}
	*/

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
		
		for (int ky =0; ky < outValues0.length; ky++) {	
			outValues0 [ky] = (tabValuesR[ky]+tabValuesG[ky]+tabValuesB[ky])/3;
		}
				
		Array1DUtil.intArrayToSafeArray(outValues0,  img2.getDataXY(0),  false, img2.isSignedDataType());
		// duplicate channel 0 to chan 1 & 2
		img2.copyData(img2, 0, 1);
		img2.copyData(img2, 0, 2);
	}
	
	private void functionRGBtoHSB(IcyBufferedImage sourceImage, int HorSorB) {

		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		// compute values
		for (int ky = 0; ky < tabValuesR.length; ky++) {

			int R = (int) tabValuesR[ky];
			int G = (int) tabValuesG[ky];
			int B = (int) tabValuesB[ky];
			
			float[] hsb = Color.RGBtoHSB(R, G, B, null) ;
			double val = (double) hsb[HorSorB] * 100;
			outValues0 [ky] = val;
			outValues1 [ky] = val;
			outValues2 [ky] = val;
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
