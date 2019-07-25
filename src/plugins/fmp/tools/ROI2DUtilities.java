package plugins.fmp.tools;


import java.util.ArrayList;
import java.util.Collections;

import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class ROI2DUtilities  {
	
	public static ArrayList<ROI2D> getListofCagesFromSequence (SequenceVirtual vSequence) {
		if (vSequence == null)
			 return null;
		
		ArrayList<ROI2D> roiList = vSequence.getROI2Ds();
		Collections.sort(roiList, new Tools.ROI2DNameComparator());
		ArrayList<ROI2D> cageLimitROIList		= new ArrayList<ROI2D>();
		for ( ROI2D roi : roiList )
		{
			String csName = roi.getName();
			if (( csName.contains( "cage") 
				|| csName.contains("Polygon2D")) 
				&& ( roi instanceof ROI2DPolygon ))
				cageLimitROIList.add(roi);
		}
		return cageLimitROIList;
	}
	
	public static ArrayList<BooleanMask2D> getMask2DFromRoiList (ArrayList<ROI2D> roiList) {
		ArrayList<BooleanMask2D> cageMaskList = new ArrayList<BooleanMask2D>();
		for ( ROI2D roi : roiList ) {
			cageMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));
		}
		return cageMaskList;
	}
	
	public static void removeROIsFromSequence (SequenceVirtual vSequence, ArrayList<ROI2D> roiList) {
		for ( ROI2D roi : roiList ) {
			vSequence.removeROI(roi);
		}
	}

}
