package plugins.fmp.multicafe;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.system.profile.Chronometer;

import plugins.fmp.sequencevirtual.Cages;
import plugins.fmp.sequencevirtual.DetectFliesParameters;
import plugins.fmp.sequencevirtual.PositionsXYT;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.tools.Tools;
import plugins.fmp.tools.ROI2DUtilities;

import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;


class BuildTrackFliesThread implements Runnable {
	
	public SequenceVirtual vSequence 	= null;	

	public ArrayList<BooleanMask2D> cageMaskList = new ArrayList<BooleanMask2D>();
	public boolean stopFlag = false;
	public DetectFliesParameters detect = new DetectFliesParameters();
	public Cages cages = new Cages();


	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 * parameters:
	 * 		vSequence
	 * 		detect
	 *
	 * change stopFlag to stop process
	 */
	
	@Override
	public void run()
	{
		int	analyzeStep = vSequence.analysisStep;
		int startFrame 	= (int) vSequence.analysisStart;
		int endFrame 	= (int) vSequence.analysisEnd;
		
		if ( vSequence.nTotalFrames < endFrame+1 )
			endFrame = (int) vSequence.nTotalFrames - 1;
		int nbframes = (endFrame - startFrame +1)/analyzeStep +1;

		System.out.println("Computation over frames: " + startFrame + " - " + endFrame );
		Chronometer chrono = new Chronometer("Tracking computation" );
		ProgressFrame progress = new ProgressFrame("Checking ROIs...");

		cages.clear();

	
		// find ROI describing cage areas - remove all others
		cages.cageLimitROIList = ROI2DUtilities.getListofCagesFromSequence(vSequence);
		cageMaskList = ROI2DUtilities.getMask2DFromRoiList(cages.cageLimitROIList);
		Collections.sort(cages.cageLimitROIList, new Tools.ROI2DNameComparator());

		// create arrays for storing position and init their value to zero
		int nbcages = cages.cageLimitROIList.size();
		System.out.println("nb cages = " + nbcages);
		cages.lastTime_it_MovedList.ensureCapacity(nbcages); 		// t of slice where fly moved the last time
		ROI2DRectangle [] tempRectROI = new ROI2DRectangle [nbcages];
		int minCapacity = (endFrame - startFrame + 1) / analyzeStep;

		for (int i=0; i < nbcages; i++)
		{
			cages.lastTime_it_MovedList.add(0);
			tempRectROI[i] = new ROI2DRectangle(0, 0, 10, 10);
			tempRectROI[i].setName("fly_"+i);
			vSequence.addROI(tempRectROI[i]);
			PositionsXYT positions = new PositionsXYT();
			positions.ensureCapacity(minCapacity);
			cages.cagePositionsList.add(positions);
		}

		// create array for the results - 1 point = 1 slice
		ROI [][] resultFlyPositionArrayList = new ROI[nbframes][nbcages];
		int lastFrameAnalyzed = endFrame;
		
		try {
			final Viewer v = Icy.getMainInterface().getFirstViewer(vSequence);	
			vSequence.beginUpdate();

			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = startFrame ; t <= endFrame && !stopFlag; t  += analyzeStep, it++ )
			{				
				// update progression bar
				int pos = (int)(100d * (double)t / (double) nbframes);
				progress.setPosition( pos );
				int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
				int timeleft = (int) ((nbSeconds* nbframes /(t+1)) - nbSeconds);
				progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + timeleft + " s");

				// load next image and compute threshold
				IcyBufferedImage workImage = vSequence.loadVImageAndSubtractReference(t, detect.transformop); 			
				vSequence.currentFrame = t;
				v.setPositionT(t);
				v.setTitle(vSequence.getVImageName(t));

				ROI2DArea roiAll = findFly ( workImage, vSequence.cages.detect.threshold, detect.ichanselected, detect.btrackWhite );

				// ------------------------ loop over all the cages of the stack
				for ( int iroi = 0; iroi < cages.cageLimitROIList.size(); iroi++ )
				{
					ROI cageLimitROI = cages.cageLimitROIList.get(iroi);
					if ( cageLimitROI == null )
						continue;
					BooleanMask2D cageMask = cageMaskList.get(iroi);
					if (cageMask == null)
						continue;
					ROI2DArea roi = new ROI2DArea( roiAll.getBooleanMask( true ).getIntersection( cageMask ) );

					// find largest component in the threshold
					ROI2DArea flyROI = null;
					int max = 0;
					BooleanMask2D bestMask = null;
					for ( BooleanMask2D mask : roi.getBooleanMask( true ).getComponents() )
					{
						int len = mask.getPoints().length;
						if (detect.blimitLow && len < detect.limitLow)
							len = 0;
						if (detect.blimitUp && len > detect.limitUp)
							len = 0;
							
						if ( len > max )
						{
							bestMask = mask;
							max = len;
						}
					}
					if ( bestMask != null )
						flyROI = new ROI2DArea( bestMask );

					if ( flyROI != null ) {
						flyROI.setName("det"+iroi +" " + t );
					}
					else {
						Point2D pt = new Point2D.Double(0,0);
						flyROI = new ROI2DArea(pt);
						flyROI.setName("failed det"+iroi +" " + t );
					}
					flyROI.setT( t );
					resultFlyPositionArrayList[it][iroi] = flyROI;

					// tempRPOI
					Rectangle2D rect = flyROI.getBounds2D();
					tempRectROI[iroi].setRectangle(rect);

					// compute center and distance (square of)
					Point2D flyPosition = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
					if (it > 0) {
						double distance = flyPosition.distance(cages.cagePositionsList.get(iroi).get(it-1));
						if (distance > detect.jitter)
							cages.lastTime_it_MovedList.set(iroi, t);
					}
					cages.cagePositionsList.get(iroi).add(flyPosition);
				}
			}
		

		} finally {
			progress.close();
//			state = StateD.NORMAL;
			vSequence.endUpdate();
			for (int i=0; i < nbcages; i++)
				vSequence.removeROI(tempRectROI[i]);
		}

		//	 copy created ROIs to inputSequence
		System.out.println("Copying results to input sequence");
		try
		{
			vSequence.beginUpdate();
			int nrois = cages.cageLimitROIList.size();
			for ( int t = startFrame ; t <= lastFrameAnalyzed ; t  += analyzeStep )
				for (int iroi=0; iroi < nrois; iroi++) 
					vSequence.addROI( resultFlyPositionArrayList[t-startFrame][iroi] );
		}
		finally
		{
			vSequence.endUpdate();
		}

		chrono.displayInSeconds();
		System.out.println("Computation finished.");
		// TODO updateButtonsVisibility(StateD.STOP_COMPUTATION);
	}

	private ROI2DArea findFly(IcyBufferedImage img, int threshold , int chan, boolean white ) {

		if (img == null)
			return null;

		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];

		if ( white)
		{
			byte[] arrayRed 	= img.getDataXYAsByte( 0);
			byte[] arrayGreen 	= img.getDataXYAsByte( 1);
			byte[] arrayBlue 	= img.getDataXYAsByte( 2);

			for ( int i = 0 ; i < arrayRed.length ; i++ )
			{
				float r = ( arrayRed[i] 	& 0xFF );
				float g = ( arrayGreen[i] 	& 0xFF );
				float b = ( arrayBlue[i] 	& 0xFF );
				float intensity = (r+g+b)/3f;
				if ( Math.abs( r-g ) > 10 )	// why 10?
				{
					mask[i] = false;
					continue;
				}
				if ( Math.abs( r-b ) > 10 )
				{
					mask[i] = false;
					continue;
				}
				mask[i] = ( intensity ) > threshold ;
			}
		}
		else {

			byte[] arrayChan = img.getDataXYAsByte( chan);
			for ( int i = 0 ; i < arrayChan.length ; i++ )
			{
				mask[i] = ( ((int) arrayChan[i] ) & 0xFF ) < threshold ;
			}
		}
		BooleanMask2D bmask = new BooleanMask2D( img.getBounds(), mask); 
		ROI2DArea roiResult = new ROI2DArea( bmask );
		return roiResult;
	}

}

