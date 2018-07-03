package plugins.fmp.areatrack;

import java.util.ArrayList;
import java.util.Collections;

import org.jfree.data.xy.XYSeries;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;

import icy.system.profile.Chronometer;
import plugins.fmp.sequencevirtual.ImageTransform;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ThresholdOverlay;
import plugins.fmp.sequencevirtual.Tools;


class AreaAnalysisThread extends Thread
{
	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * parameters:
	 * 		threshold 
	 * 		jitter
	 * 		ichanselected
	 * 		btrackWhite
	 *  *  blimitLow
	 *  blimitUp
	 *  limitLow
	 *  limitUp
	 */
	
	private int threshold = 0;
	private int ichanselected = 0;
	private int transf = 0;
	SequenceVirtual vSequence = null;
	private ArrayList<ROI2D> roiList = null;

	private int startFrame = 0;
	private int endFrame = 99999999;
	private int analyzeStep = 1;
	private int imageref = 0;
	 
	// --------------------------------------------------------------------------------------
	
	
	public void setAnalysisThreadParameters (SequenceVirtual sseq, 
			ArrayList<ROI2D> sroiList, 
			int sstartFrame, 
			int sendFrame, 
			int stransf,
			int simageref,
			int schan)
	{
		vSequence = sseq;
		roiList = sroiList;
		startFrame = sstartFrame;
		endFrame = sendFrame;
		
		ichanselected = schan;
		transf = stransf;
		imageref = simageref;
		if (transf == 2)
			vSequence.setRefImageForSubtraction(imageref);
	}
	
	@Override
	public void run()
	{
		// global parameters
		analyzeStep = vSequence.istep;
		threshold = vSequence.threshold;
		roiList = vSequence.getROI2Ds();
		Collections.sort(roiList, new Tools.ROI2DNameComparator());
		if ( vSequence.nTotalFrames < endFrame+1 )
			endFrame = (int) vSequence.nTotalFrames - 1;
		int nbframes = endFrame - startFrame +1;

		// verbose output
		System.out.println("Computation over frames: " + startFrame + " - " + endFrame );
		Chronometer chrono = new Chronometer("Tracking computation" );
		ProgressFrame progress = new ProgressFrame("Checking ROIs...");

		// create array for the results - 1 point = 1 slice
		int iroi = 0;
		vSequence.results = new XYSeries[roiList.size()];
		vSequence.pixels = new XYSeries[roiList.size()];
		ArrayList<ROI2D> 				areaROIList= new ArrayList<ROI2D>();
		ArrayList<BooleanMask2D> 		areaMaskList 	= new ArrayList<BooleanMask2D>();
		
		areaROIList.clear();
		areaMaskList.clear();
		for (ROI2D roi: roiList)
		{
			String csName = roi.getName();
			vSequence.results[iroi] = new XYSeries(csName);
			vSequence.results[iroi].clear();
			vSequence.pixels[iroi] = new XYSeries(csName);
			vSequence.pixels[iroi].clear();
			
			areaROIList.add(roi);
			areaMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));
			iroi++;
		}
		Collections.sort(areaROIList, new Tools.ROI2DNameComparator());

		try {
			final Viewer v = Icy.getMainInterface().getFirstViewer(vSequence);	
			vSequence.beginUpdate();
			ImageTransform transformImg = new ImageTransform();
					
			// ----------------- loop over all images of the stack

			for (int t = startFrame ; t <= endFrame && !isInterrupted(); t  += analyzeStep )
			{				
				// update progression bar
				updateProgressionBar (t, nbframes, chrono, progress);

				// load next image and compute threshold
				//IcyBufferedImage workImage = vinputSequence.loadVImageTransf(t, transf); 
				IcyBufferedImage workImage = transformImg.transformImage(vSequence.loadVImage(t), transf); 
				
				vSequence.currentFrame = t;
				v.setPositionT(t);
				v.setTitle(vSequence.getVImageName(t));

				// ------------------------ compute global mask
				boolean[] maskAll = ThresholdOverlay.getBinaryOverThreshold(workImage, t, ichanselected, threshold, null);
				BooleanMask2D maskAll2D = new BooleanMask2D( workImage.getBounds(), maskAll); 
				
				// ------------------------ loop over all the cages of the stack
				for (int imask = 0; imask < areaMaskList.size(); imask++ )
				{
					ROI areaLimitROI = areaROIList.get(imask);
					if ( areaLimitROI == null )
						continue;

					// count number of pixels over threshold 
					int sum = 0;
					int npixels = 0;
					BooleanMask2D areaMask = areaMaskList.get(imask);
					if (areaMask != null)
					{
						npixels = areaMask.getNumberOfPoints();
						BooleanMask2D intersectionMask = maskAll2D.getIntersection( areaMask );
						sum = intersectionMask.getNumberOfPoints();
					}
					vSequence.results[imask].add(t, sum);
					vSequence.pixels[imask].add(t, npixels);
				}
			}
		

		} finally {
			progress.close();
			vSequence.endUpdate();
		}

		chrono.displayInSeconds();
		System.out.println("Computation finished.");
	}
	
	void updateProgressionBar( int t, int nbframes, Chronometer chrono, ProgressFrame progress) {
		int pos = (int)(100d * (double)t / (double) nbframes);
		progress.setPosition( pos );
		int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
		int timeleft = (int) ((nbSeconds* nbframes /(t+1)) - nbSeconds);
		progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + timeleft + " s");
	}

}
