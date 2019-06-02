package plugins.fmp.multiCAFE;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.system.profile.Chronometer;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;
import plugins.fmp.tools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class BuildDetect_Gulps {
	
	public void detectGulps(MultiCAFE parent0) {	
		// send some info
		ProgressFrame progress = new ProgressFrame("Gulp analysis started");
		progress.setLength(parent0.kymographArrayList.size() * (parent0.vSequence.analysisEnd - parent0.vSequence.analysisStart +1));
		progress.setPosition(0);
		Chronometer chrono = new Chronometer("Tracking computation" );
		int  nbSeconds = 0;
		int jitter = 5;

		// scan each kymograph in the list
		int firstkymo = 0;
		int lastkymo = parent0.kymographArrayList.size() -1;
		if (! parent0.kymographsPane.gulpsTab.detectAllGulpsCheckBox.isSelected()) {
			firstkymo = parent0.kymographsPane.optionsTab.kymographNamesComboBox.getSelectedIndex();
			lastkymo = firstkymo;
		}
		
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			// update progression bar
			int pos = (int)(100d * (double)kymo / parent0.kymographArrayList.size());
			progress.setPosition( kymo  );
			nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
			int nbSecondsNext = nbSeconds*10 + 1;
			double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
			progress.setMessage( "Processing gulps: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + (int) timeleft + " s");
			int done = 0;

			// clear old data
			SequencePlus kymographSeq = parent0.kymographArrayList.get(kymo);
			kymosInitForGulpsDetection(kymographSeq, parent0);
			ROI2DPolyLine roiTrack = new ROI2DPolyLine ();

			kymographSeq.beginUpdate();
			IcyBufferedImage image = kymographSeq.getImage(0, 2, 0);	// time=0; z=2; c=0

			double[] tabValues = image.getDataXYAsDouble(0);			// channel 0 - RED
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			int ix = 0;
			int iy = 0;
			List<Point2D> pts = new ArrayList<>();
			Collection<ROI> boutsRois = new ArrayList <> ();
			Point2D.Double pt = null;

			// scan each image row
			kymographSeq.derivedValuesArrayList.add(0);
			// once an event is detected, we will cut and save the corresponding part of topLevelArray
			ArrayList <Integer> topLevelArray = kymographSeq.getArrayListFromRois(ArrayListType.topLevel);

			for (ix = 1; ix < topLevelArray.size(); ix++) 
			{
				// send some info to the user
				nbSeconds =  (int) (chrono.getNanos() / 100000000f);
				if (nbSeconds > nbSecondsNext) {
					nbSecondsNext = nbSeconds*10 + 1;
					pos = (int)(100d * (double)((done +ix) / parent0.kymographArrayList.size()));
					timeleft = ((double)nbSeconds)* (100d-pos) /pos;
					progress.setMessage( "Processing gulps : " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + (int) timeleft + " s");
				}

				// for each point of topLevelArray, define a bracket of rows to look at ("jitter" = 10)
				int low = topLevelArray.get(ix)- jitter;
				int high = low + 2*jitter;
				if (low < 0) 
					low = 0;
				if (high >= yheight) 
					high = yheight-1;

				int max = (int) tabValues [ix + low*xwidth];
				for (iy = low+1; iy < high; iy++) 
				{
					int val = (int) tabValues [ix  + iy*xwidth];
					if (max < val) {
						max = val;
					}
				}

				// add new point to display as roi
				if (max > kymographSeq.detectGulpsThreshold) {
					if (pts.size() > 0) {
						Point2D prevPt = pts.get(pts.size() -1);
						if (prevPt.getX() != (double) (ix-1)) {
							roiTrack.setColor(Color.red);
							roiTrack.setName("gulp"+String.format("%07d", ix));
							roiTrack.setPoints(pts);
							boutsRois.add(roiTrack);
							roiTrack = new ROI2DPolyLine ();
							pts = new ArrayList<>();
							pt = new Point2D.Double (ix-1, topLevelArray.get(ix-1));
							pts.add(pt);
						}
					} 
					pt = new Point2D.Double (ix, topLevelArray.get(ix));
					pts.add(pt);
				}
				kymographSeq.derivedValuesArrayList.add(max);
			}

			if (pts.size() > 0) {
				roiTrack.setPoints(pts);
				roiTrack.setColor(Color.red);
				roiTrack.setName("gulp"+String.format("%07d", ix));
				boutsRois.add(roiTrack);
			}

			kymographSeq.addROIs(boutsRois, false);
			kymographSeq.endUpdate(); 

			done += xwidth;
		}

		// send some info
		progress.close();
		System.out.println("Elapsed time (s):" + nbSeconds);
	}	

	private void kymosInitForGulpsDetection(SequencePlus kymographSeq, MultiCAFE parent0) {
		
		getDialogBoxParametersForDetection(kymographSeq, false, true, parent0);
		for (ROI roi:kymographSeq.getROIs()) {
			if (roi.getName().contains("gulp"))
				kymographSeq.removeROI(roi);
		}
		kymographSeq.derivedValuesArrayList.clear();
	}
	
	private void getDialogBoxParametersForDetection(SequencePlus seq, boolean blevel, boolean bgulps, MultiCAFE parent0) {
		if (blevel) {
			seq.detectTop 				= true; 
			seq.detectBottom 			= true; 
			seq.transformForLevels 		= (TransformOp) parent0.kymographsPane.limitsTab.transformForLevelsComboBox.getSelectedItem();
			seq.direction 				= parent0.kymographsPane.limitsTab.directionComboBox.getSelectedIndex();
			seq.detectLevelThreshold 	= (int) parent0.kymographsPane.limitsTab.getDetectLevelThreshold();
			seq.detectAllLevel 			= parent0.kymographsPane.limitsTab.detectAllLevelCheckBox.isSelected();
		}
		
		if (bgulps) {
			seq.detectGulpsThreshold 	= (int) parent0.kymographsPane.gulpsTab.getDetectGulpsThreshold();
			seq.transformForGulps 		= (TransformOp) parent0.kymographsPane.gulpsTab.transformForGulpsComboBox.getSelectedItem();
			seq.detectAllGulps 			= parent0.kymographsPane.gulpsTab.detectAllGulpsCheckBox.isSelected();
		}
		seq.bStatusChanged = true;
	}
}
