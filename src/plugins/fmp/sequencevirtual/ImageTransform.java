package plugins.fmp.sequencevirtual;

import java.awt.Color;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;

public class ImageTransform {
	
	private static String[] availableTransforms 	= new String[] {
			"(G+B)/2-R", 
			"(B+R)/2-G",
			"XDiffn", "XYDiffn", 
			"R", "G", "B",
			"H(HSB)", "S(HSB)", "B(HSB)",
			"subtract t0",
			"subtract n-1",
			"subtract ref"
			};
	private static IcyBufferedImage referenceImage = null;
	private static int spanDiffTop = 3;	// adjust this parameter eventually through user's interface
	private static	int	spanDiffTransf2 = 3;

	// -------------------------------------
	public static String[] getAvailableTransforms() 
	{
		return availableTransforms;
	}
	
	public static void setReferenceImage(IcyBufferedImage img) {
		referenceImage = img;
	}
	
	public static IcyBufferedImage transformImage (IcyBufferedImage img, int transform) {
		IcyBufferedImage img2 = null;
		switch (transform) {
		case 1: // "(G+B)/2-R" 
			img2 = functionRGB_sumGBMinus2R(img);
			break;
		case 2: // "(B+R)/2-G",
			img2 = functionRGB_sumBRMinus2G (img);
			break;
		case 3: //"XDiffn", 
			img2 = computeXDiffn (img);
			break;
		case 4: //"XYDiffn",
			img2 = computeXYDiffn (img);
			break;
		case 5: //"R", 
			img2 = functionRGB_RorGorB(img, 0);
			break;
		case 6: //"G", 
			img2 = functionRGB_RorGorB(img, 1);
			break;
		case 7: //"B",
			img2 = functionRGB_RorGorB(img, 2);
			break;
		case 8: //"H(HSB)", 
			img2 = functionRGB_HorSorB(img, 0);
			break;
		case 9: //"S(HSB)", 
			img2 = functionRGB_HorSorB(img, 1);
			break;
		case 10: //"B(HSB)",
			img2 = functionRGB_HorSorB(img, 2);
			break;
		case 11: //"subtract t0",
			//break;
		case 12: //"subtract n-1",
			//break;
		case 13: //"subtract ref"
			//break;
		default:
			img2 = IcyBufferedImageUtil.getCopy(img);
			break;
		}
		return img2;
	}
	
	private static IcyBufferedImage functionRGB_sumGBMinus2R (IcyBufferedImage sourceImage) {

		IcyBufferedImage img2 = IcyBufferedImageUtil.getCopy(sourceImage); 
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
		return img2;
	}
	
	private static IcyBufferedImage functionRGB_sumBRMinus2G (IcyBufferedImage sourceImage) {

		IcyBufferedImage img2 = IcyBufferedImageUtil.getCopy(sourceImage); 
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
		return img2;
	}
	
	private static IcyBufferedImage computeXDiffn(IcyBufferedImage sourceImage) {

			int chan0 = 0;
			int chan1 =  sourceImage.getSizeC();
			IcyBufferedImage img2 = IcyBufferedImageUtil.getCopy(sourceImage); 

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
			// end of loop
			return img2;
	}
	
	private static IcyBufferedImage computeXYDiffn(IcyBufferedImage sourceImage) {

		int chan0 = 0;
		int chan1 =  sourceImage.getSizeC();

		IcyBufferedImage img2 = IcyBufferedImageUtil.getCopy(sourceImage); 

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
		
		return img2;
	}

	private static IcyBufferedImage functionRGB_RorGorB (IcyBufferedImage sourceImage, int RorBorC) {

		IcyBufferedImage img2 = IcyBufferedImageUtil.getCopy(sourceImage); 
		int R = RorBorC; 	// R = 0 or 1 or 2
		int G = (R+1)%3; 	// G = 1
		int B = (R+2)%3; 	// B = 2
		
		// duplicate channel R to chan B & G
		img2.copyData(img2, R, B);
		img2.copyData(img2, R, G);
		return img2;
	}
	
	private static IcyBufferedImage functionRGB_HorSorB(IcyBufferedImage sourceImage, int HorSorB) {
		
		IcyBufferedImage img2 = IcyBufferedImageUtil.getCopy(sourceImage); 

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
		// end of loop
		return img2;
	}
}
