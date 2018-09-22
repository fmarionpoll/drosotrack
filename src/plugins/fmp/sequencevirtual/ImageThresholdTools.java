package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.util.ArrayList;
import icy.image.IcyBufferedImage;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;


public class ImageThresholdTools {

	public enum ThresholdType { 
		SINGLE ("simple threshold"), COLORARRAY ("Color array"), NONE("undefined");
		
		private String label;
		ThresholdType (String label) { this.label = label;}
		public String toString() { return label;}	
		
		public static ThresholdType findByText(String abbr){
		    for(ThresholdType v : values()){ if( v.toString().equals(abbr)) { return v; }  }
		    return null;
		}
	}
	
	private int colorthreshold;
	private ArrayList <Color> colorarray = null;
	private int distanceType;

	private final byte byteFALSE = 0;
	private final byte byteTRUE = (byte) 0xFF;
	ArrayList<double[]> colordoubleArray = null;
	
	public void setThresholdOverlayParametersColors (int distanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		this.distanceType = distanceType;
		this.colorthreshold = colorthreshold;
		this.colorarray = new ArrayList <Color> (colorarray);
		colordoubleArray = transformColorArrayintoDouble(colorarray);
	}

	private ArrayList<double[]> transformColorArrayintoDouble (ArrayList<Color> colorarray) {
		
		ArrayList<double[]> colordoubleArray = new ArrayList<double[]>(3);
		for (int i=0; i<colorarray.size(); i++) {
			Color color = colorarray.get(i);
			double [] coldouble = new double [3];
			coldouble [0] = color.getRed();
			coldouble [1] = color.getGreen();
			coldouble [2] = color.getBlue ();
			colordoubleArray.add(coldouble);
		}
		return colordoubleArray;
	}
	
	public IcyBufferedImage getBinaryInt_FromThreshold(IcyBufferedImage sourceImage, int thresholdvalue) 
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
			if (val > thresholdvalue)
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

		if (sourceImage.getSizeC() <3 ) {
			System.out.print("Failed operation: attempt to threshold image with colors while image has less than 3 color channels");
			return null;
		}
		NHColorDistance distance; 
		if (distanceType == 1)
			distance = new NHL1ColorDistance();
		else
			distance = new NHL2ColorDistance();
			
		IcyBufferedImage binaryByte = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);	
		byte [][] sourceBuffer = sourceImage.getDataXYCAsByte(); // [C][XY]
		byte [] binaryResultBuffer = binaryByte.getDataXYAsByte(0);
		
		int npixels = binaryResultBuffer.length;
		double[] pixel = new double [3];
		double[] color = new double [3];
		
		for (int ipixel = 0; ipixel < npixels; ipixel++) {
			
			byte val = byteFALSE; 
			for (int i=0; i<3; i++)
				pixel[i] = sourceBuffer[i][ipixel];
		
			for (int k = 0; k < colorarray.size(); k++) {
				color = colordoubleArray.get(k);
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
