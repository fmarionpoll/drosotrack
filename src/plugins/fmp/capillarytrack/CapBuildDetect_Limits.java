package plugins.fmp.capillarytrack;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI;
import icy.system.profile.Chronometer;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.tools.ImageTransformTools.TransformOp;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class CapBuildDetect_Limits {
	
	public void detectCapillaryLevels(Capillarytrack parent0) {

		// send some info
		ProgressFrame progress = new ProgressFrame("Processing started");
		int len = parent0.kymographArrayList.size();
		int nbframes = (int) (parent0.vSequence.analysisEnd - parent0.vSequence.analysisStart +1);
		progress.setLength(len*nbframes);
		progress.setPosition(0);
		Chronometer chrono = new Chronometer("Tracking computation" );
		int  nbSeconds = 0;

		boolean bdetectUp = (parent0.detectPane.limitsTab.directionComboBox.getSelectedIndex() == 0);
		int jitter = 10;
		int firstkymo = 0;
		int lastkymo = parent0.kymographArrayList.size() -1;
		if (! parent0.detectPane.limitsTab.detectAllLevelCheckBox.isSelected()) {
			firstkymo = parent0.kymographsPane.optionsTab.kymographNamesComboBox.getSelectedIndex();
			lastkymo = firstkymo;
		}

		// scan each kymograph in the list
		for (int kymo=firstkymo; kymo <= lastkymo; kymo++) 
		{
			// update progression bar
			double pos = (100d * (double)kymo / len);
			progress.setPosition( kymo  );
			nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
			int nbSecondsNext = nbSeconds*10 + 1;
			double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
			progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " +  (int) timeleft + " s");
			int done = 0;

			SequencePlus kymographSeq = parent0.kymographArrayList.get(kymo);
			double detectLevelThreshold = parent0.detectPane.limitsTab.getDetectLevelThreshold();

			for (ROI roi:kymographSeq.getROIs()) {
				if (roi.getName().contains("topLevel"))
					kymographSeq.removeROI(roi);
			}
			kymographSeq.removeAllROI();
			
			// save parameters status
			getDialogBoxParametersForDetection(kymographSeq, true, false, parent0); 
			
			ROI2DPolyLine roiTopTrack = new ROI2DPolyLine ();
			roiTopTrack.setName("toplevel");
			kymographSeq.addROI(roiTopTrack);
			List<Point2D> ptsTop = new ArrayList<>();
			
			ROI2DPolyLine roiBottomTrack = new ROI2DPolyLine ();
			roiBottomTrack.setName("bottomlevel");
			kymographSeq.addROI(roiBottomTrack);
			List<Point2D> ptsBottom = new ArrayList<>();

			kymographSeq.beginUpdate();
			IcyBufferedImage image = null;
			int c = 0;
			image = kymographSeq.getImage(0, 1, c);
			double[] tabValues = image.getDataXYAsDouble(c);
			int xwidth = image.getSizeX();
			int yheight = image.getSizeY();
			double x = 0;
			double y = 0;
			int ix = 0;
			int iy = 0;
			int oldiytop = 0;		// assume that curve goes from left to right with jitter 
			int oldiybottom = yheight-1;
			
			boolean flagtop = true;
			boolean flagbottom = true; 

			// scan each image column
			for (ix = 0; ix < xwidth; ix++) 
			{
				// send some info
				nbSeconds =  (int) (chrono.getNanos() / 100000000f);
				if (nbSeconds > nbSecondsNext) {
					nbSecondsNext = nbSeconds*10 + 1;
					pos = (int)(100d * (double)((done +ix) / len));
					timeleft = ((double)nbSeconds)* (100d-pos) /pos;
					progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + (int) timeleft + " s");
				}

				// ---------------------------------------------------- detect top level
				if (flagtop) {
					// set flags for internal loop (part of the row)
					boolean found = false;
					x = ix;
					oldiytop -= jitter;
					if (oldiytop < 0) 
						oldiytop = 0;

					// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
					for (iy = oldiytop; iy < yheight; iy++) 
					{
						boolean flag = false;
						if (bdetectUp)
							flag = tabValues [ix + iy* xwidth] > detectLevelThreshold;
						else 
							flag = tabValues [ix + iy* xwidth] < detectLevelThreshold;

						if( flag) {
							y = iy;
							found = true;
							oldiytop = iy;
							break;
						}
					}
					if (!found) {
						oldiytop = 0;
					}
					// add new point to display as roi
					ptsTop.add(new Point2D.Double (x, y));
				}
				
				// --------------------------------------------------- detect bottom level
				if (flagbottom) {
					// set flags for internal loop (part of the row)
					boolean found = false;
					x = ix;
					oldiybottom = yheight - 1;

					// for each line, go from left to right - starting from the last position found minus "jitter" (set to 10)
					for (iy = oldiybottom; iy >= 0 ; iy--) 
					{
						boolean flag = false;
						if (bdetectUp)
							flag = tabValues [ix + iy* xwidth] > detectLevelThreshold;
						else 
							flag = tabValues [ix + iy* xwidth] < detectLevelThreshold;

						if (flag) {
							y = iy;
							found = true;
							oldiybottom = iy;
							break;
						}
					}
					if (!found) {
						oldiybottom = yheight - 1;
					}
					// add new point to display as roi
					ptsBottom.add(new Point2D.Double (x, y));
				}
			}
			
			roiTopTrack.setPoints(ptsTop);
			roiBottomTrack.setPoints(ptsBottom);
			kymographSeq.getArrayListFromRois(ArrayListType.cumSum);
			kymographSeq.endUpdate();
			done += xwidth;
		}

		// send some info
		progress.close();
		System.out.println("Elapsed time (s):" + nbSeconds);
	}
	
	private void getDialogBoxParametersForDetection(SequencePlus seq, boolean blevel, boolean bgulps, Capillarytrack parent0) {
		if (blevel) {
			seq.detectTop 				= true; 
			seq.detectBottom 			= true; 
			seq.transformForLevels 		= (TransformOp) parent0.detectPane.limitsTab.transformForLevelsComboBox.getSelectedItem();
			seq.direction 				= parent0.detectPane.limitsTab.directionComboBox.getSelectedIndex();
			seq.detectLevelThreshold 	= (int) parent0.detectPane.limitsTab.getDetectLevelThreshold();
			seq.detectAllLevel 			= parent0.detectPane.limitsTab.detectAllLevelCheckBox.isSelected();
		}
		seq.bStatusChanged = true;
	}
}
