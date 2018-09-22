package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.util.ArrayList;

import icy.image.IcyBufferedImage;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class ImageOperations {
	ImageOperationsStruct op = new ImageOperationsStruct();
	SequenceVirtual seq = null;
	private ImageTransformTools imgTransf = new ImageTransformTools();
	private ImageThresholdTools imgThresh = new ImageThresholdTools();
	
	public ImageOperations (SequenceVirtual seq) {
		setSequence(seq);
	}
	
	public void setSequence(SequenceVirtual seq) {
		this.seq = seq;
		imgTransf.setSequence(seq);
	}
	
	public void setTransform (TransformOp transformop) {
		op.transformop = transformop;
	}
	
	public void setThreshold(ThresholdType thresholdtype, int threshold) {
		op.thresholdtype = thresholdtype;
		op.threshold = threshold;
	}
	
	public void setThreshold (ThresholdType thresholdtype, ArrayList <Color> colorarray, int distancetype, int threshold) {
		op.thresholdtype = thresholdtype;
		if (op.colorarray != null)
			op.colorarray.clear();
		op.colorarray = colorarray;
		op.distanceType = distancetype;
		op.threshold = threshold;
	}
	
	public IcyBufferedImage run() {
		return run (seq.currentFrame);
	}
	
	public IcyBufferedImage run (int frame) {	
		// step 1
		op.fromFrame = frame;
		if (!op.isTransformEqual(seq.cacheTransformOp)) {
			seq.cacheTransformedImage = imgTransf.transformImageFromSequence(frame, op.transformop);
			if (seq.cacheTransformedImage == null)
				return null;
			seq.cacheTransformOp = op;
		}
		
		// step 2
		if (!op.isThresholdEqual(seq.cacheThresholdOp)) {
			if (op.thresholdtype == ThresholdType.COLORARRAY)
				seq.cacheThresholdedImage = imgThresh.getBinaryInt_FromColorsThreshold(seq.cacheTransformedImage); //+ distancetype, colorthreshold, colorarray
			else 
				seq.cacheThresholdedImage = imgThresh.getBinaryInt_FromThreshold(seq.cacheTransformedImage, op.threshold);
			seq.cacheThresholdOp = op;
		}
		return seq.cacheThresholdedImage;
	}
	
	public IcyBufferedImage run_nocache() {
		// step 1
		int frame = seq.currentFrame;
		IcyBufferedImage transformedImage = imgTransf.transformImageFromSequence(frame, op.transformop);
		if (transformedImage == null)
			return null;
		
		// step 2
		IcyBufferedImage thresholdedImage;
		if (op.thresholdtype == ThresholdType.COLORARRAY)
			thresholdedImage = imgThresh.getBinaryInt_FromColorsThreshold(seq.cacheTransformedImage); //+ distancetype, colorthreshold, colorarray
		else 
			thresholdedImage = imgThresh.getBinaryInt_FromThreshold(seq.cacheTransformedImage, op.threshold);

		return thresholdedImage;
	}

	public boolean[] convertToBoolean(IcyBufferedImage binaryMap) {
		return imgThresh.getBoolMap_FromBinaryInt(binaryMap);
	}
}
