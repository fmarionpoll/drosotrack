package plugins.fmp.sequencevirtual;

import java.awt.Color;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;

public class ImageTransformTools {

	public enum TransformOp { 
		None("none"),
		R_RGB("R(RGB)"), G_RGB("G(RGB)"), B_RGB("B(RGB)"),  
		GBMINUS2R ("(G+B)-2R"), RBMINUS2G("(R+B)-2G"), RGMINUS2B("(R+G)-2B"),
		RGB ("(R+G+B)/3"),
		H_HSB ("H(HSB)"), S_HSB ("S(HSB)"), B_HSB("B(HSB)"),  
		XDIFFN("XDiffn"),  XYDIFFN( "XYDiffn"), 
		REFt0("subtract t0"), REFn("subtract n-1"), REF("subtract ref"),
		NORM_BRmG("F. Rebaudo"),
		COLORARRAY1("color array"), RGB_TO_HSV("HSV"), RGB_TO_H1H2H3("H1H2H3"), 
		RTOGB ("R to G&B") ;
		
		private String label;
		TransformOp (String label) { this.label = label; }
		public String toString() { return label; }
		
		public static TransformOp findByText(String abbr){
		    for(TransformOp v : values()){ if( v.toString().equals(abbr)) { return v; } }
		    return null;
		}
	}
	public double factorR=1.;
	public double factorG=1.;
	public double factorB=1.;

	private IcyBufferedImage referenceImage = null;
	private int spanDiff = 3;
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
	
	public void setSequenceOfReferenceImage (SequenceVirtual vinputSeq) {
		vinputSequence = vinputSeq;
		referenceImage = vinputSequence.loadVImage(0);
	}
	
	public IcyBufferedImage transformImage (IcyBufferedImage inputImage, TransformOp transformop) {
		
		IcyBufferedImage transformedImage = null;
		
		switch (transformop) {
		case None: 
		case COLORARRAY1: /*System.out.println("transform image - " + transformop);*/
			transformedImage = inputImage;
			break;
		
		case R_RGB: 	transformedImage= functionRGB_keepOneChan(inputImage, 0); break;
		case G_RGB: 	transformedImage= functionRGB_keepOneChan(inputImage, 1); break;
		case B_RGB: 	transformedImage= functionRGB_keepOneChan(inputImage, 2); break;
		case RGB: 		transformedImage= functionRGB_grey (inputImage);
		
		case H_HSB: 	transformedImage= functionRGBtoHSB(inputImage, 0); break;
		case S_HSB: 	transformedImage= functionRGBtoHSB(inputImage, 1); break;
		case B_HSB: 	transformedImage= functionRGBtoHSB(inputImage, 2); break;

		case GBMINUS2R: transformedImage= functionRGB_C1C2Minus2C3 (inputImage, 1, 2, 0); break;
		case RBMINUS2G: transformedImage= functionRGB_C1C2Minus2C3 (inputImage, 0, 2, 1); break;
		case RGMINUS2B: transformedImage= functionRGB_C1C2Minus2C3 (inputImage, 0, 1, 2); break;
		case NORM_BRmG: transformedImage= functionNormRGB_sumC1C2Minus2C3(inputImage, 1, 2, 0); break;
		case RTOGB: 	transformedImage= functionTransferRedToGreenAndBlue(inputImage); break;
			
		case REFt0: 	transformedImage= functionSubtractRef(inputImage); break;
		case REF: 		transformedImage= functionSubtractRef(inputImage); break;
		case REFn: 
			int t = vinputSequence.currentFrame;
			if (t>0) 
				{referenceImage = vinputSequence.loadVImage(t-1); 
				transformedImage= functionSubtractRef(inputImage);} 
			break;
			
		case XDIFFN: 	transformedImage= computeXDiffn (inputImage); break;
		case XYDIFFN: 	transformedImage= computeXYDiffn (inputImage); break;

		case RGB_TO_HSV: transformedImage= functionRGBtoHSV(inputImage); break;
		case RGB_TO_H1H2H3: transformedImage= functionRGBtoH1H2H3(inputImage); break;
		}
		
		return transformedImage;
	}
	
	public IcyBufferedImage transformImageFromSequence (int t, TransformOp transformop) {
		return transformImage(vinputSequence.loadVImage(t), transformop);
	}
		
	// function proposed by François Rebaudo
	private IcyBufferedImage functionNormRGB_sumC1C2Minus2C3 (IcyBufferedImage sourceImage, int Rlayer, int Glayer, int Blayer) {
 
		double[] Rn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Rlayer), sourceImage.isSignedDataType());
		double[] Gn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Glayer), sourceImage.isSignedDataType());
		double[] Bn = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(Blayer), sourceImage.isSignedDataType());
		double[] ExG = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		double[] sum = (double[]) Array1DUtil.createArray(DataType.DOUBLE, Rn.length);
		
//		ArrayMath.divide (Rn, 255, Rn);		// R = R/255
//		ArrayMath.divide (Gn, 255, Gn);		// G = G/255
//		ArrayMath.divide (Bn, 255, Bn);		// B = B/255
		
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
//		ArrayMath.multiply(ExG, 255, ExG);	// ExG = ExG * 255
		
		IcyBufferedImage img = new IcyBufferedImage (sourceImage.getWidth(), sourceImage.getHeight(), 1, DataType.BYTE);
		Array1DUtil.doubleArrayToSafeArray(ExG,  img.getDataXY(0),  false); 
		return img;
	}
	
	private IcyBufferedImage functionTransferRedToGreenAndBlue(IcyBufferedImage sourceImage) {
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		img2.copyData(sourceImage, 0, 0);
		img2.copyData(sourceImage, 0, 1);
		img2.copyData(sourceImage, 0, 2);
		return img2;
	}
	
	private IcyBufferedImage functionRGB_C1C2Minus2C3 (IcyBufferedImage sourceImage, int addchan1, int addchan2, int subtractchan3) {
		
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 1, sourceImage.getDataType_());
		
		int[] tabSubtract = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(subtractchan3), sourceImage.isSignedDataType());
		int[] tabAdd1 = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(addchan1), sourceImage.isSignedDataType());
		int[] tabAdd2 = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(addchan2), sourceImage.isSignedDataType());
		int[] tabResult = (int[]) Array1DUtil.createArray(DataType.INT, tabSubtract.length);

		for (int i = 0; i < tabResult.length; i++) {	
			tabResult [i] = tabSubtract[i]* 2 - tabAdd1[i] - tabAdd2[i] ;
		}
		
		Array1DUtil.intArrayToSafeArray(tabResult, img2.getDataXY(0),  true, img2.isSignedDataType());
		return img2;
	}
	
	private IcyBufferedImage computeXDiffn(IcyBufferedImage sourceImage) {

		
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getSizeC(), sourceImage.getDataType_());
		
		for (int c=chan0; c < chan1; c++) {

			int[] tabValues = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			int[] outValues = Array1DUtil.arrayToIntArray(img2.getDataXY(c), img2.isSignedDataType());			

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
					outValues [kx] = (int) Math.abs(outVal);
				}

				// erase border values
				for (int ix = imageSizeX-spanDiff; ix < imageSizeX; ix++) {
					outValues[ix + iy* imageSizeX] = 0;
				}
			}
			//Array1DUtil.intArrayToSafeArray(outValues, img2.getDataXY(c), true, img2.isSignedDataType());
		}
		return img2;
	}
	
	
	private IcyBufferedImage computeXYDiffn(IcyBufferedImage sourceImage) {

		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();
		int imageSizeX = sourceImage.getSizeX();
		int imageSizeY = sourceImage.getSizeY();
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getSizeC(), sourceImage.getDataType_());
		
		for (int c=chan0; c < chan1; c++) {

			int[] tabValues = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(c), sourceImage.isSignedDataType());
			int[] outValues = Array1DUtil.arrayToIntArray(img2.getDataXY(c), img2.isSignedDataType());			
			
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
					outValues [ky] = (int) Math.abs(outVal);
				}

				// erase out-of-bounds points
				for (int iy = 0; iy < spanDiff; iy++) 
					outValues[ix + iy* imageSizeX] = 0;

				for (int iy = imageSizeY-spanDiff; iy < imageSizeY; iy++) 
					outValues[ix + iy* imageSizeX] = 0;
			}
			//Array1DUtil.intArrayToSafeArray(outValues,  img2.getDataXY(c), true, img2.isSignedDataType());
		}
		return img2;
	}
	

	private IcyBufferedImage functionRGB_keepOneChan (IcyBufferedImage sourceImage, int keepChan) {

		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 1, sourceImage.getDataType_());
		img2.copyData(sourceImage, keepChan, 0);
		return img2;
	}
	
	private IcyBufferedImage functionRGB_grey (IcyBufferedImage sourceImage) {

		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 1, sourceImage.getDataType_());
		
		int[] tabValuesR = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		int[] tabValuesG = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		int[] tabValuesB = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());
		int[] outValues0 = Array1DUtil.arrayToIntArray(img2.getDataXY(0), img2.isSignedDataType());
		
		for (int ky =0; ky < outValues0.length; ky++) {	
			outValues0 [ky] = (tabValuesR[ky]+tabValuesG[ky]+tabValuesB[ky])/3;
		}
				
		//Array1DUtil.intArrayToSafeArray(outValues0,  img2.getDataXY(0),  false, img2.isSignedDataType());
		return img2;
	}
	
	private IcyBufferedImage functionRGBtoHSB(IcyBufferedImage sourceImage, int HorSorB) {

		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 3, sourceImage.getDataType_());
		
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
		return img2;
	}
	
	private IcyBufferedImage functionSubtractRef(IcyBufferedImage sourceImage) {
		
		if (referenceImage == null)
			referenceImage = vinputSequence.loadVImage(0);
		
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(),sourceImage.getSizeC(), DataType.INT);
		
		for (int c=0; c<sourceImage.getSizeC(); c++){
			Object sourceArray = sourceImage.getDataXY(c);
			Object referenceArray = referenceImage.getDataXY(c);
			int [] imgSource = Array1DUtil.arrayToIntArray(sourceArray, sourceImage.isSignedDataType());
			int [] imgReference = Array1DUtil.arrayToIntArray(referenceArray, referenceImage.isSignedDataType());
			ArrayMath.subtract(imgSource, imgReference, imgSource);
		}
		return img2;
	}
	
	private IcyBufferedImage functionRGBtoHSV (IcyBufferedImage sourceImage) {
		
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 1, sourceImage.getDataType_());
		
		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		// compute values
		float [] hsb = new float [3];
		for (int ky = 0; ky < tabValuesR.length; ky++) {

			int R = (int) tabValuesR[ky];
			int G = (int) tabValuesG[ky];
			int B = (int) tabValuesB[ky];			
			hsb = Color.RGBtoHSB(R, G, B, hsb) ;
			outValues0 [ky] = (double) hsb[0] ;
			outValues1 [ky] = (double) hsb[1] ;
			outValues2 [ky] = (double) hsb[2] ;
		}

		Array1DUtil.doubleArrayToSafeArray(outValues0,  img2.getDataXY(0),  img2.isSignedDataType());
		Array1DUtil.doubleArrayToSafeArray(outValues1,  img2.getDataXY(1),  img2.isSignedDataType());
		Array1DUtil.doubleArrayToSafeArray(outValues2,  img2.getDataXY(2),  img2.isSignedDataType());
		return img2;
	}

	private IcyBufferedImage functionRGBtoH1H2H3 (IcyBufferedImage sourceImage) {
		
		IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), 1, sourceImage.getDataType_());
		
		double[] tabValuesR = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(0), sourceImage.isSignedDataType());
		double[] tabValuesG = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(1), sourceImage.isSignedDataType());
		double[] tabValuesB = Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(2), sourceImage.isSignedDataType());

		double[] outValues0 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(0), img2.isSignedDataType());
		double[] outValues1 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(1), img2.isSignedDataType());
		double[] outValues2 = Array1DUtil.arrayToDoubleArray(img2.getDataXY(2), img2.isSignedDataType());
		
		// compute values
		final double VMAX = 255.0;
		for (int ky = 0; ky < tabValuesR.length; ky++) {

			int r = (int) tabValuesR[ky];
			int g = (int) tabValuesG[ky];
			int b = (int) tabValuesB[ky];
			
			outValues0 [ky] = (r + g) / 2.0;
			outValues1 [ky] = (VMAX + r - g) / 2.0;
			outValues2 [ky] = (VMAX + b - (r + g) / 2.0) / 2.0;
		}

		Array1DUtil.doubleArrayToSafeArray(outValues0,  img2.getDataXY(0),  img2.isSignedDataType());
		Array1DUtil.doubleArrayToSafeArray(outValues1,  img2.getDataXY(1),  img2.isSignedDataType());
		Array1DUtil.doubleArrayToSafeArray(outValues2,  img2.getDataXY(2),  img2.isSignedDataType());
		return img2;
	}
	
}
