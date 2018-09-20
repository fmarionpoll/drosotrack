package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class ImageThresholdTools {

	public enum ThresholdType { 
		SINGLE ("simple threshold"), COLORARRAY ("Color array");
		
		private String label;
		ThresholdType (String label) { this.label = label;}
		public String toString() { return label;}	
		
		public static ThresholdType findByText(String abbr){
		    for(ThresholdType v : values()){ if( v.toString().equals(abbr)) { return v; }  }
		    return null;
		}
	}
	private ImageTransformTools imgTransf = new ImageTransformTools();
	private int thresholdValue = 0;
	private int colorthreshold;
	private ArrayList <Color> colorarray = null;
	private int distanceType;
	private TransformOp transformop;
	private ArrayList<double[]> ccolor = null;
	
	private final int byteFALSE = 0;
	private final int byteTRUE = 0xFF;
	
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

	private ArrayList<double[]> transformColorArray(ArrayList<Color> colorarray_in, TransformOp transformop_in) 
	{
		// transform color array into an image with 3 components 
		IcyBufferedImage colorRef = new IcyBufferedImage(colorarray_in.size(), 1, 3, DataType.DOUBLE);
		for (int k1 = 0; k1 < colorarray_in.size(); k1++) {
			colorRef.setData(k1, 0, 0, colorarray_in.get(k1).getRed());
			colorRef.setData(k1, 0, 1, colorarray_in.get(k1).getGreen());
			colorRef.setData(k1, 0, 2, colorarray_in.get(k1).getBlue());
		}
		
		// create array of 3 channels for colors and transform it like sourceImage
		IcyBufferedImage colorRefTransformed = imgTransf.transformImage(colorRef, transformop_in);
		ArrayList<double[]> colorsarray = new ArrayList<double[]>();
		for (int chan = 0; chan < 3; chan++) {
			colorsarray.add (Array1DUtil.arrayToDoubleArray(colorRefTransformed.getDataXY(chan), colorRefTransformed.isSignedDataType()));
		}
		
		// transform back into a color array
		ArrayList<double []> cccolor = new ArrayList<double []>();
		for (int k = 0; k < colorarray_in.size(); k++) {
			double[] color = new double [3];
			for (int i=0; i<3; i++)
				color[i] = colorsarray.get(i)[k];
			cccolor.add(color);
		}
		return cccolor;
	}
	
	public IcyBufferedImage getBinaryInt_FromThreshold(IcyBufferedImage sourceImage) 
	{		
		int chan = 0;
		DataType datatype = sourceImage.getDataType_();
		IcyBufferedImage binaryMap = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.INT);
		int [] binaryMapDataBuffer = binaryMap.getDataXYAsInt(chan);
		
		if (datatype.getBitSize() >8) {
			int [] imageSourceDataBuffer = sourceImage.getDataXYAsInt(chan);
			for (int x = 0; x < binaryMapDataBuffer.length; x++)  {
				if ((imageSourceDataBuffer[x] & 0xFF) > thresholdValue)
					binaryMapDataBuffer[x] = byteFALSE;
				else
					binaryMapDataBuffer[x] = byteTRUE;
			}
		}
		else {
			byte [] imageSourceDataBuffer = sourceImage.getDataXYAsByte(chan);
			for (int x = 0; x < binaryMapDataBuffer.length; x++)  {
				int val = imageSourceDataBuffer[x] & 0xFF;
				if (val > thresholdValue)
					binaryMapDataBuffer[x] = byteFALSE;
				else
					binaryMapDataBuffer[x] = byteTRUE;
			}
		}
		return binaryMap;
	}
	
	public boolean[] getBoolMap_FromBinaryInt(IcyBufferedImage img) {
		boolean[]	boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		
		int [] imgByte = Array1DUtil.arrayToIntArray(img.getDataXYAsInt(0), img.isSignedDataType());
		for (int x = 0; x < boolMap.length; x++)  {

			if (imgByte[x] == byteFALSE)
				boolMap[x] =  false;
			else
				boolMap[x] =  true;
		}
		return boolMap;
	}
	
	public IcyBufferedImage getBinaryInt_FromColorsThreshold(IcyBufferedImage sourceImage) 
	{
		if (colorarray.size() == 0)
			return null;
		if (ccolor == null)
			ccolor = transformColorArray(colorarray, transformop);
		
		NHColorDistance distance; 
		if (distanceType == 1)
			distance = new NHL1ColorDistance();
		else
			distance = new NHL2ColorDistance();
		
		int valFALSE = (int) byteFALSE;
		int valTRUE = 0xFFF;
		// TODO: can we define the color here?
//		Color c = Color.red;
//		int ir = 255 - c.getRed();
//		int ig = 255 - c.getGreen();
//		int ib = 255 - c.getBlue();
//		c = new Color(ir, ig, ib);
//		valTRUE = c.getRGB();
		
		IcyBufferedImage binaryInt = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.INT);	
		ArrayList<double[]> imagearray = new ArrayList<double[]>();
		for (int chan = 0; chan < 3; chan++) {
			imagearray.add( Array1DUtil.arrayToDoubleArray(sourceImage.getDataXY(chan), sourceImage.isSignedDataType()));
		}
		int[] binaryResultBuffer = binaryInt.getDataXYAsInt(0);
		int npixels = imagearray.get(0).length;
		
		double[] pixel = new double [3];
		double[] color = new double [3];
		
		for (int ipixel = 0; ipixel < npixels; ipixel++) {
			
			int val = valFALSE; 
			for (int i=0; i<3; i++)
				pixel[i] = imagearray.get(i)[ipixel];
		
			for (int k = 0; k < colorarray.size(); k++) {
				color = ccolor.get(k);
				if (distance.computeDistance(pixel, color) < colorthreshold) {
					val = valTRUE; 
					break;
				}
			}
			binaryResultBuffer[ipixel] = val;
		}
		return binaryInt;
	}
	

}
