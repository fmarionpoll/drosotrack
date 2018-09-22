package plugins.fmp.sequencevirtual;

import java.awt.Color;
import java.util.ArrayList;

import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;

public class ImageOperationsStruct {
	
	TransformOp 		transformop		= TransformOp.NONE;
	ThresholdType 		thresholdtype 	= ThresholdType.NONE;
	int 				threshold		= 0;
	ArrayList <Color> 	colorarray 		= null;
	int 				distanceType 	= 0;
	int 				fromFrame		= -1;
		
	// -----------------------------------
	
	public ImageOperationsStruct () {
		this.fromFrame = -1;
		this.transformop = TransformOp.NONE;
		this.thresholdtype = ThresholdType.NONE;
		this.threshold = 0;
	}
	
	public ImageOperationsStruct (int framenumber, TransformOp transformop, ThresholdType thresholdtype, int thresholdvalue) {
		this.fromFrame = framenumber;
		this.transformop = transformop;
		this.thresholdtype = thresholdtype;
		this.threshold = thresholdvalue;
	}
	
	public ImageOperationsStruct (int framenumber, TransformOp transformop) {
		this.fromFrame = framenumber;
		this.transformop = transformop;
		this.thresholdtype = ThresholdType.NONE;
		this.threshold = 0;
	}

	public void setFrameNumber (int framenumber) {
		this.fromFrame= framenumber;
	}
	
	public void setTransformOp(TransformOp transformop) {
		this.transformop = transformop;
	}
	
	public void setTresholdType (ThresholdType thresholdtype, int thresholdvalue) {
		this.thresholdtype= thresholdtype;
		this.threshold = thresholdvalue;
	}
	
	public boolean isEqual (ImageOperationsStruct tag) {
		if (tag.fromFrame != fromFrame)
			return false;
		if (tag.transformop != transformop)
			return false;
		if (tag.thresholdtype != thresholdtype)
			return false;
		if (tag.threshold != threshold)
			return false;
		return true;
	}
	
	public boolean isTransformEqual(ImageOperationsStruct op) {
		if (op.fromFrame != this.fromFrame)
			return false;
		if (op.transformop != this.transformop)
			return false;
		return true;
	}
	
	public boolean isThresholdEqual(ImageOperationsStruct op) {
		if (op.fromFrame != this.fromFrame)
			return false;
		if (op.thresholdtype != this.thresholdtype)
			return false;
		if (op.threshold != this.threshold)
			return false;

		if (this.thresholdtype == ThresholdType.COLORARRAY) {
			if (op.distanceType != this.distanceType)
				return false;
			if (op.colorarray.size() != this.colorarray.size())
				return false;
		}
		return true;
	}
}
