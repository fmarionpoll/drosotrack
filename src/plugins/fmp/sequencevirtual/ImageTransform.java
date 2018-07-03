package plugins.fmp.sequencevirtual;

import java.awt.Color;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.type.collection.array.Array1DUtil;

public class ImageTransform {
	
	private String[] availableTransforms 	= new String[] {
			"(G+B)/2-R", 
			"(B+R)/2-G",
			"XDiffn", "XYDiffn", 
			"R", "G", "B",
			"H(HSB)", "S(HSB)", "B(HSB)",
			"subtract t0",
			"subtract n-1",
			"subtract ref"
			};
	private IcyBufferedImage referenceImage = null;
	private int spanDiffTop = 3;	// adjust this parameter eventually through user's interface
	private int	spanDiffTransf2 = 3;
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
		case 1: // "(G+B)/2-R" 
			functionRGB_sumGBMinus2R(img);
			break;
		case 2: // "(B+R)/2-G",
			functionRGB_sumBRMinus2G (img);
			break;
		case 3: //"XDiffn", 
			computeXDiffn (img);
			break;
		case 4: //"XYDiffn",
			computeXYDiffn (img);
			break;
		case 5: //"R", 
			functionRGB_RorGorB(img, 0);
			break;
		case 6: //"G", 
			functionRGB_RorGorB(img, 1);
			break;
		case 7: //"B",
			functionRGB_RorGorB(img, 2);
			break;
		case 8: //"H(HSB)", 
			functionRGB_HorSorB(img, 0);
			break;
		case 9: //"S(HSB)", 
			functionRGB_HorSorB(img, 1);
			break;
		case 10: //"B(HSB)",
			functionRGB_HorSorB(img, 2);
			break;
		case 11: //"subtract t0",
			functionSubtractRef(img);
			break;
		case 12: //"subtract n-1",
			functionSubtractRef(img);
			break;
		case 13: //"subtract ref"
			functionSubtractRef(img);
			break;
		default:
			IcyBufferedImageUtil.getCopy(img);
			break;
		}
		return img2;
	}
	
	private void functionRGB_sumGBMinus2R (IcyBufferedImage sourceImage) {
 
		int SUB = 0; 	// R = 0
		int ADD1 = 1; 	// G = 1
		int ADD2 = 2; 	// B = 2
		
		double[] tabSubtract = sourceImage.getDataXYAsDouble(SUB); 
		double[] tabValuesG = sourceImage.getDataXYAsDouble(ADD1);	 
		double[] tabValuesB = sourceImage.getDataXYAsDouble(ADD2);
		double[] img2Values = img2.getDataXYAsDouble(0);

		int xwidth = img2.getSizeX();
		int yheight = img2.getSizeY();

		// main loop
		for (int iy = 0; iy < yheight; iy++) {
			
			for (int ix =0; ix < xwidth; ix++) {
				
				int ky = ix + iy* xwidth;
				img2Values [ky] = (tabValuesG[ky] + tabValuesB[ky])/2 - tabSubtract [ky];
			}
		}
		
		// duplicate channel 0 to chan 1 & 2
		img2.copyData(img2, 0, 1);
		img2.copyData(img2, 0, 2);
	}
	
	private void functionRGB_sumBRMinus2G (IcyBufferedImage sourceImage) {

		int SUB = 1;  // G=1
		int ADD1 = 0; // R=0
		int ADD2 = 2; // B=2
		
		double[] tabSubtract = sourceImage.getDataXYAsDouble(SUB); 
		double[] tabValuesG = sourceImage.getDataXYAsDouble(ADD1);	 
		double[] tabValuesB = sourceImage.getDataXYAsDouble(ADD2);
		double[] img2Values = img2.getDataXYAsDouble(0);

		int xwidth = img2.getSizeX();
		int yheight = img2.getSizeY();

		// main loop
		for (int iy = 0; iy < yheight; iy++) {
			
			for (int ix =0; ix < xwidth; ix++) {
				
				int ky = ix + iy* xwidth;
				img2Values [ky] = (tabValuesG[ky] + tabValuesB[ky])/2 - tabSubtract [ky];
			}
		}
		
		// duplicate channel 0 to chan 1 & 2
		img2.copyData(img2, 0, 1);
		img2.copyData(img2, 0, 2);
	}
	
	private void computeXDiffn(IcyBufferedImage sourceImage) {

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
		}
	}
	
	private void computeXYDiffn(IcyBufferedImage sourceImage) {

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
		}
	}

	private void functionRGB_RorGorB (IcyBufferedImage sourceImage, int RorBorC) {

		int R = RorBorC; 	// R = 0 or 1 or 2
		int G = (R+1)%3; 	// G = 1
		int B = (R+2)%3; 	// B = 2
		
		// duplicate channel R to chan B & G
		img2.copyData(img2, R, B);
		img2.copyData(img2, R, G);
	}
	
	private void functionRGB_HorSorB(IcyBufferedImage sourceImage, int HorSorB) {
		
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
