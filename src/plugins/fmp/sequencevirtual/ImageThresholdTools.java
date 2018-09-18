package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class ImageThresholdTools {

	public enum ThresholdType { SINGLE ("simple threshold"), COLORARRAY ("Color array");
		private String label;
		ThresholdType (String label) { this.label = label;}
		public String toString() { return label;}	
	}
	private ImageTransformTools imgTransf = new ImageTransformTools();
	private int thresholdValue = 0;
	private int colorthreshold;
	private ArrayList <Color> colorarray = null;
	private int distanceType;
	private TransformOp transformop;
	private ArrayList<double[]> ccolor = null;
	
	private final byte byteFALSE = (byte) 0;
	private final byte byteTRUE = (byte) 255;
	
	public void setThresholdOverlayParameters (int sthreshold, TransformOp stransf)
	{
		this.thresholdValue = sthreshold;
		this.transformop = stransf;
	}
	
	public void setThresholdOverlayParametersColors (int distanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		this.distanceType = distanceType;
		this.colorthreshold = colorthreshold;
		this.colorarray = new ArrayList <Color> (colorarray);
	}

	private void transformColorArray() {
		// transform color array into an image with 3 components 
		IcyBufferedImage colorRef = new IcyBufferedImage(colorarray.size(), 1, 3, DataType.DOUBLE);
		for (int k1 = 0; k1 < colorarray.size(); k1++) {
			colorRef.setData(k1, 0, 0, colorarray.get(k1).getRed());
			colorRef.setData(k1, 0, 1, colorarray.get(k1).getGreen());
			colorRef.setData(k1, 0, 2, colorarray.get(k1).getBlue());
		}
		// create array of 3 channels for colors and transform it like sourceImage
		IcyBufferedImage colorRefTransformed = imgTransf.transformImage(colorRef, transformop);
		ArrayList<double[]> colorsarray = new ArrayList<double[]>();
		for (int chan = 0; chan < 3; chan++) {
			colorsarray.add (Array1DUtil.arrayToDoubleArray(colorRefTransformed.getDataXY(chan), colorRefTransformed.isSignedDataType()));
		}
		
		// transform back into a color array
		ccolor = new ArrayList<double []>();
		for (int k = 0; k < colorarray.size(); k++) {
			double[] color = new double [3];
			for (int i=0; i<3; i++)
				color[i] = colorsarray.get(i)[k];
			ccolor.add(color);
		}
	}
	
	public IcyBufferedImage getBinaryOverThresholdFromDoubleImage(IcyBufferedImage img) {
		
		IcyBufferedImage binaryMap = new IcyBufferedImage(img.getSizeX(), img.getSizeY(), 1, DataType.UBYTE);
		int chan = 0;
		byte[] imageSourceDataBuffer = img.getDataXYAsByte(chan);
		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(chan);
		
		for (int x = 0; x < binaryMapDataBuffer.length; x++)  {
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > thresholdValue)
				binaryMapDataBuffer[x] = byteFALSE;
			else
				binaryMapDataBuffer[x] = byteTRUE;
		}
		return binaryMap;
	}
	
	public boolean[] getBoolMapFromUBYTEBinaryImage(IcyBufferedImage img) {
		boolean[]	boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		byte[] imgByte = img.getDataXYAsByte(0);
		for (int x = 0; x < boolMap.length; x++)  {
			if (imgByte[x] == byteTRUE)
				boolMap[x] =  true;
			else
				boolMap[x] =  false;
		}
		return boolMap;
	}
	
	public boolean[] getBoolMapOverThresholdFromDoubleImage(IcyBufferedImage img, int threshold) {
		
		boolean[]	boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		int chan = 0;
		final byte[] imageSourceDataBuffer = img.getDataXYAsByte(chan);
		for (int x = 0; x < boolMap.length; x++)  {
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > threshold)
				boolMap[x] = false;
			else
				boolMap[x] = true;
		}
		return boolMap;
	}

	public IcyBufferedImage getBinaryFromColorsOverThresholdAndDoubleImage(IcyBufferedImage sourceImage) {

		if (colorarray.size() == 0)
			return null;
		if (ccolor == null)
			transformColorArray();
		
		NHColorDistance distance; 
		if (distanceType == 1)
			distance = new NHL1ColorDistance();
		else
			distance = new NHL2ColorDistance();
		

		IcyBufferedImage binaryMap = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);	
		ArrayList<double[]> imagearray = new ArrayList<double[]>();
		for (int chan = 0; chan < 3; chan++) {
			imagearray.add( Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(chan), sourceImage.isSignedDataType()));
		}
		byte[] binaryResultBuffer = binaryMap.getDataXYAsByte(0);
		int npixels = imagearray.get(0).length;
		
		double[] pixel = new double [3];
		double[] color = new double [3];
		
		for (int ipixel = 0; ipixel < npixels; ipixel++) {
			
			byte val = byteFALSE;
			for (int i=0; i<3; i++)
				pixel[i] = imagearray.get(i)[ipixel];
		
			for (int k = 0; k < colorarray.size(); k++) {
				color = ccolor.get(k);
				if (distance.computeDistance(pixel, color) < colorthreshold) {
					val = byteTRUE;
					break;
				}
			}
			binaryResultBuffer[ipixel] = val;
		}
		/*
		Color c = box.getAverageColor();
		int ir = 255 - c.getRed();
		int ig = 255 - c.getGreen();
		int ib = 255 - c.getBlue();
		m.setColor(new Color(ir, ig, ib));
		m.setOpacity(1f);
		*/
		return binaryMap;
	}
	

}
