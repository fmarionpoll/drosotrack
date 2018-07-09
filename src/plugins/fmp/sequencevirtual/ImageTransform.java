package plugins.fmp.sequencevirtual;

import java.awt.Color;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.type.collection.array.Array1DUtil;

public class ImageTransform {
	private String[] availableTransforms 	= new String[] {
			"RGB",
			"R", "G", "B","Grey (R+G+B)/3",
			"(G+B)/2-R", 
			"(B+R)/2-G",
			"H(HSB)", "S(HSB)", "B(HSB)",
			"XDiffn", "XYDiffn", 
			"subtract t0",
			"subtract n-1",
			"subtract ref"
			};


	public enum TransformOp {
		RGBall("RGB"), R_RGB("R(RGB)"), G_RGB("G(RGB)"), B_RGB("B(RGB)"), Grey_RGB("Grey (R+G+B)/3"), 
		GBmR ("(G+B)/2-R"), BRmG("(B+R)/2-G"),
		H_HSB ("H(HSB)"), S_HSB ("S(HSB)"), B_HSB("B(HSB)"),  
		XDIFFN("XDiffn"), XYDIFFN( "XYDiffn"), 
		REFt0("subtract t0"), REFn("subtract n-1"), REF("subtract ref");
		private String label;
		TransformOp (String label) {
		       this.label = label;
		}
		public String toString() {
		       return label;
		}	
	}
	// ComboBox<Status> cbxStatus = new ComboBox<>();
	// cbxStatus.getItems().setAll(Status.values());

	private IcyBufferedImage referenceImage = null;
	private int spanDiffTop = 3;	// adjust this parameter eventually through user's interface
	//private int	spanDiffTransf2 = 3;
	private	IcyBufferedImage img2;

	// -------------------------------------
	public String[] getAvailableTransforms() 
	{
		return availableTransforms;
	}
	
	public void setReferenceImage(IcyBufferedImage img) {
		referenceImage = IcyBufferedImageUtil.getCopy(img);
	}
	
	public IcyBufferedImage transformImage (IcyBufferedImage img, int transform) {
		
		img2 = IcyBufferedImageUtil.getCopy(img);
		
		switch (transform) {
		case 1: functionRGB_keepOneChan(img, 0); break;
		case 2: functionRGB_keepOneChan(img, 1); break;
		case 3: functionRGB_keepOneChan(img, 2); break;
		case 4: functionRGB_grey(img); break;
		case 5: 
			functionRGB_sumC1C2Minus2C3(img, 1, 2, 0); 
			break;
		case 6: 
			functionRGB_sumC1C2Minus2C3 (img, 0, 2, 1); 
			break;
		
		case 7: functionRGB_HSB(img, 0); break;
		case 8: functionRGB_HSB(img, 1); break;
		case 9: functionRGB_HSB(img, 2); break;
		
		case 10: computeXDiffn (img); break;
		case 11: computeXYDiffn (img); break;
		
		case 12: functionSubtractRef(img); break;
		case 13: functionSubtractRef(img); break;
		case 14: functionSubtractRef(img); break;
		default: break;
		}
		
		return img2;
	}
	
	private void functionRGB_sumC1C2Minus2C3 (IcyBufferedImage sourceImage, int ADD1, int ADD2, int SUB) {
 
		System.out.println("functionRGB_sumC1C2Minus2C3");
		int[] tabSubtract = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(SUB), false);
		int[] tabAdd1 = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(ADD1), false);
		int[] tabAdd2 = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(ADD2), false);
		int[] img2Values = Array1DUtil.arrayToIntArray(img2.getDataXY(0), false);

		int xwidth = sourceImage.getSizeX();
		int yheight = sourceImage.getSizeY();

		// main loop
		for (int iy = 0; iy < yheight; iy++) {
			
			for (int ix =0; ix < xwidth; ix++) {
				int ky = ix + iy* xwidth;
				img2Values [ky] = (tabAdd1[ky] + tabAdd2[ky])/2 - tabSubtract [ky];
			}
		}
		Array1DUtil.intArrayToSafeArray(img2Values,  img2.getDataXY(0),  true, true);
		
		// duplicate channel 0 to chan 1 & 2
		img2.copyData(img2, 0, 1);
		img2.copyData(img2, 0, 2);
	}
		
	private void computeXDiffn(IcyBufferedImage sourceImage) {

		System.out.println("computeXDiffn");
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();

		for (int c=chan0; c < chan1; c++) {

			double[] tabValues = sourceImage.getDataXYAsDouble(c);
			double[] outValues = img2.getDataXYAsDouble(c);

			int imageSizeX = sourceImage.getSizeX();
			int imageSizeY = sourceImage.getSizeY();

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
			Array1DUtil.doubleArrayToSafeArray(outValues,  img2.getDataXY(c),  true);
		}
	}
	
	private void computeXYDiffn(IcyBufferedImage sourceImage) {

		System.out.println("computeXYDiffn");
		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();

		for (int c=chan0; c < chan1; c++) {

			double[] tabValues = sourceImage.getDataXYAsDouble(c);
			double[] outValues = img2.getDataXYAsDouble(c);

			int imageSizeX = sourceImage.getSizeX();
			int imageSizeY = sourceImage.getSizeY();

			// main loop
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
			Array1DUtil.doubleArrayToSafeArray(outValues,  img2.getDataXY(c),  true);
		}
	}

	private void functionRGB_keepOneChan (IcyBufferedImage sourceImage, int keepChan) {

		System.out.println("functionRGB_keepOneChan");
		for (int i=0; i<3; i++)
			if (i != keepChan) 
				img2.copyData(img2, keepChan, i);
	}
	
	private void functionRGB_grey (IcyBufferedImage sourceImage) {

		System.out.println("functionRGB_grey");
		int[] tabValuesR = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(0), false);
		int[] tabValuesG = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(1), false);
		int[] tabValuesB = Array1DUtil.arrayToIntArray(sourceImage.getDataXY(2), false);

		int[] outValues0 = Array1DUtil.arrayToIntArray(img2.getDataXY(0), false);
		
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
		Array1DUtil.intArrayToSafeArray(outValues0,  img2.getDataXY(0),  true, true);
		// duplicate channel 0 to chan 1 & 2
		img2.copyData(img2, 0, 1);
		img2.copyData(img2, 0, 2);
	}
	
	private void functionRGB_HSB(IcyBufferedImage sourceImage, int HorSorB) {
		
		System.out.println("functionRGB_HSB");
		
		double[] tabValuesR = sourceImage.getDataXYAsDouble(0);
		double[] tabValuesG = sourceImage.getDataXYAsDouble(1);
		double[] tabValuesB = sourceImage.getDataXYAsDouble(2);

		double[] outValues0 = img2.getDataXYAsDouble(0);
		double[] outValues1 = img2.getDataXYAsDouble(1);
		double[] outValues2 = img2.getDataXYAsDouble(2);
		
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
		Array1DUtil.doubleArrayToSafeArray(outValues0,  img2.getDataXY(0),  true);
		Array1DUtil.doubleArrayToSafeArray(outValues1,  img2.getDataXY(1),  true);
		Array1DUtil.doubleArrayToSafeArray(outValues2,  img2.getDataXY(2),  true);
	}
	
	private void functionSubtractRef(IcyBufferedImage sourceImage) {
		
		/* algorithm borrowed from  Perrine.Paul-Gilloteaux@univ-nantes.fr in  EC-CLEM
		 * original function: private IcyBufferedImage substractbg(Sequence ori, Sequence bg,int t, int z) 
		 */
		//IcyBufferedImage img2 = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(),sourceImage.getSizeC(), sourceImage.getDataType_());
		for (int c=0; c<sourceImage.getSizeC(); c++){
			Object sourceArray = sourceImage.getDataXY(c);
			Object referenceArray = referenceImage.getDataXY(c);
			double[] img1DoubleArray = Array1DUtil.arrayToDoubleArray(sourceArray, sourceImage.isSignedDataType());
			double[] img2DoubleArray = Array1DUtil.arrayToDoubleArray(referenceArray, referenceImage.isSignedDataType());
			ArrayMath.subtract(img1DoubleArray, img2DoubleArray, img1DoubleArray);

			double[] dummyzeros=Array1DUtil.arrayToDoubleArray(img2.getDataXY(c), img2.isSignedDataType());
			ArrayMath.max(img1DoubleArray, dummyzeros, img1DoubleArray);
			Array1DUtil.doubleArrayToArray(img1DoubleArray, img2.getDataXY(c));
		}
	}
	
}
