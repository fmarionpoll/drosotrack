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
	
	private final byte byteFALSE = 0;
	private final byte byteTRUE = (byte) 0xFF;
	
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
			Object chanarray = colorRefTransformed.getDataXY(chan);
			double [] chancolor = Array1DUtil.arrayToDoubleArray(chanarray, colorRefTransformed.isSignedDataType());
			colorsarray.add (chancolor);
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
		IcyBufferedImage binaryMap = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);
		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);

		int [] imageSourceDataBuffer = null;
		DataType datatype = sourceImage.getDataType_();
		if (datatype != DataType.INT) {
			Object sourceArray = sourceImage.getDataXY(0);
			imageSourceDataBuffer = Array1DUtil.arrayToIntArray(sourceArray, sourceImage.isSignedDataType());
		}
		else
			imageSourceDataBuffer = sourceImage.getDataXYAsInt(0);
		
		for (int x = 0; x < binaryMapDataBuffer.length; x++)  {
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > thresholdValue)
				binaryMapDataBuffer[x] = byteFALSE;
			else
				binaryMapDataBuffer[x] = byteTRUE;
		}
		return binaryMap;
	}
	
	public boolean[] getBoolMap_FromBinaryInt(IcyBufferedImage img) {
		boolean[]	boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		
		byte [] imageSourceDataBuffer = null;
		DataType datatype = img.getDataType_();
		if (datatype != DataType.BYTE && datatype != DataType.UBYTE) {
			Object sourceArray = img.getDataXY(0);
			imageSourceDataBuffer = Array1DUtil.arrayToByteArray(sourceArray);
		}
		else
			imageSourceDataBuffer = img.getDataXYAsByte(0);
		
		for (int x = 0; x < boolMap.length; x++)  {

			if (imageSourceDataBuffer[x] == byteFALSE)
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
		if (sourceImage.getSizeC() <3 )
			return null;
		NHColorDistance distance; 
		if (distanceType == 1)
			distance = new NHL1ColorDistance();
		else
			distance = new NHL2ColorDistance();
			
		IcyBufferedImage binaryByte = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);	
		byte [] binaryResultBuffer = binaryByte.getDataXYAsByte(0);
		int npixels = binaryResultBuffer.length;
		byte [][] sourceBuffer = sourceImage.getDataXYCAsByte(); // [C][XY]
		double[] pixel = new double [3];
		double[] color = new double [3];
		
		for (int ipixel = 0; ipixel < npixels; ipixel++) {
			
			byte val = byteFALSE; 
			for (int i=0; i<3; i++)
				pixel[i] = sourceBuffer[i][ipixel];
		
			for (int k = 0; k < colorarray.size(); k++) {
				color = ccolor.get(k);
				if (distance.computeDistance(pixel, color) < colorthreshold) {
					val = byteTRUE; 
					break;
				}
			}
			binaryResultBuffer[ipixel] = val;
		}
		return binaryByte;
	}
	
}
