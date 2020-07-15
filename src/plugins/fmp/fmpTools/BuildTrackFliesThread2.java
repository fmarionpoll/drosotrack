package plugins.fmp.fmpTools;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.SwingUtilities;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.system.profile.Chronometer;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.fmpSequence.Cages;
import plugins.fmp.fmpSequence.SequenceVirtual;
import plugins.fmp.fmpSequence.XYTaSeries;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DRectangle;

public class BuildTrackFliesThread2  implements Runnable {
	
	private ArrayList<BooleanMask2D> 	cageMaskList 	= new ArrayList<BooleanMask2D>();
	private ArrayList<Boolean>			initialflyRemoved = new ArrayList<Boolean> ();
	private int							analyzeStep ;
	private int 						startFrame;
	private int 						endFrame;
	private int 						nbframes;
	private Viewer 						viewer;
	public Rectangle					rectangleAllCages = null;
		
	public SequenceVirtual 				vSequence 		= null;	
	public boolean 						stopFlag 		= false;
	public boolean 						threadRunning 	= false;
	public boolean						buildBackground	= true;
	public boolean						detectFlies		= true;
	
	public DetectFliesParameters 		detect 			= new DetectFliesParameters();
	public Cages 						cages 			= new Cages();
	public SequenceVirtual 				seqNegative 	= null;
	public SequenceVirtual 				seqPositive 	= null;
	public SequenceVirtual				seqReference	= null;
	public boolean						viewInternalImages = false;

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
		threadRunning = true;

		System.out.println("Computation over frames: " + startFrame + " - " + endFrame );

		// create arrays for storing position and init their value to zero
	
		initParametersForDetection();

		if (buildBackground || vSequence.refImage == null)
			buildBackgroundImage();
		
		if (detectFlies)
			detectFlies();
			
		threadRunning = false;

		System.out.println("Computation finished.");
		if (seqNegative  != null) {
			seqNegative.close();
			seqNegative = null;
		}
		
		if (seqPositive != null ) {
			seqPositive.close();
			seqPositive = null;
		}
		
		if (!buildBackground && seqReference != null ) {
			seqReference.close();
			seqReference = null;
		}
	}

	private void detectFlies () {
		
		Chronometer chrono = new Chronometer("Tracking computation" );
		ProgressFrame progress = new ProgressFrame("Detecting flies...");
		int nbcages = cages.cageLimitROIList.size();
		ROI2DRectangle [] tempRectROI = new ROI2DRectangle [nbcages];
		int minCapacity = (endFrame - startFrame + 1) / analyzeStep;		
		for (int i=0; i < nbcages; i++)
		{
			tempRectROI[i] = new ROI2DRectangle(0, 0, 10, 10);
			tempRectROI[i].setName("fly_"+i);
			vSequence.addROI(tempRectROI[i]);
			XYTaSeries positions = new XYTaSeries(cages.cageLimitROIList.get(i));
			positions.ensureCapacity(minCapacity);
			cages.flyPositionsList.add(positions);
		}

		// create array for the results - 1 point = 1 slice
		ROI [][] resultFlyPositionArrayList = new ROI[nbframes][nbcages];
		int lastFrameAnalyzed = endFrame;
		
		try {
			viewer = vSequence.getFirstViewer();	
			vSequence.beginUpdate();
			
			if (viewInternalImages) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						displayDetectViewer();
					}
				});
			}
			
			// ----------------- loop over all images of the stack
			int it = 0;
			for (int t = startFrame; t <= endFrame && !stopFlag; t  += analyzeStep, it++ )
			{				
				// update progression bar
				int pos = (int)(100d * (double)t / (double) nbframes);
				progress.setPosition( pos );
				int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
				int timeleft = (int) ((nbSeconds* nbframes /(t+1)) - nbSeconds);
				progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + timeleft + " s");

				// load next image and compute threshold
				IcyBufferedImage currentImage = vSequence.loadVImage(t);
				vSequence.currentFrame = t;
				viewer.setPositionT(t);
				viewer.setTitle(vSequence.getDecoratedImageName(t));
				
				IcyBufferedImage negativeImage = vSequence.subtractImages (vSequence.refImage, currentImage);
				if (seqNegative != null)
					seqNegative.setImage(0,  0, IcyBufferedImageUtil.getSubImage(negativeImage, rectangleAllCages));
				ROI2DArea roiAll = findFly (negativeImage, vSequence.cages.detect.threshold, detect.ichanselected, detect.btrackWhite );

				// ------------------------ loop over all the cages of the stack
				for ( int iroi = 0; iroi < cages.cageLimitROIList.size(); iroi++ )
				{		
					BooleanMask2D bestMask = findLargestComponent(roiAll, iroi);
					
					ROI2DArea flyROI = null;
					if ( bestMask != null ) {
						flyROI = new ROI2DArea( bestMask );
						flyROI.setName("det"+iroi +" " + t );
					}
					else {
						Point2D pt = new Point2D.Double(-1,-1);
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
						double distance = flyPosition.distance(cages.flyPositionsList.get(iroi).getPoint(it-1));
						if (distance > detect.jitter)
							cages.flyPositionsList.get(iroi).lastTimeAlive = t;
					}
					cages.flyPositionsList.get(iroi).add(flyPosition, t);
				}
			}
		} finally {
			progress.close();
			vSequence.endUpdate();
			for (int i=0; i < nbcages; i++)
				vSequence.removeROI(tempRectROI[i]);
		}

		//	 copy created ROIs to inputSequence
		System.out.println("Copying results to input sequence");
		try
		{
			vSequence.beginUpdate();
			vSequence.cages = cages;
			int nrois = cages.cageLimitROIList.size();
			int it = 0;
			for ( int t = startFrame ; t <= lastFrameAnalyzed ; t  += analyzeStep, it++ )
				for (int iroi=0; iroi < nrois; iroi++) 
					vSequence.addROI( resultFlyPositionArrayList[it][iroi] );
		}
		finally
		{
			chrono.displayInSeconds();
			vSequence.endUpdate();
		}
	}

	private ROI2DArea findFly(IcyBufferedImage img, int threshold , int chan, boolean white ) {

		if (img == null)
			return null;

		boolean[] mask = new boolean[ img.getSizeX() * img.getSizeY() ];

		if (white)
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
	
	private void patchRectToReferenceImage(IcyBufferedImage currentImage, Rectangle2D rect) {
		
		int cmax = currentImage.getSizeC();
		for (int c=0; c< cmax; c++) {
			int[] intCurrentImage = Array1DUtil.arrayToIntArray(currentImage.getDataXY(c), currentImage.isSignedDataType());
			int[] intRefImage = Array1DUtil.arrayToIntArray(vSequence.refImage.getDataXY(c), vSequence.refImage.isSignedDataType());
			int xwidth = currentImage.getSizeX();
			for (int x = 0; x < rect.getWidth(); x++) {
				for (int y=0; y< rect.getHeight(); y++) {
					int xi = (int) (rect.getX() + x);
					int yi = (int) (rect.getY() + y);
					int coord = xi + yi*xwidth;
					intRefImage[coord] = intCurrentImage[coord];
				}
			}
			Object destArray = vSequence.refImage.getDataXY(c);
			Array1DUtil.intArrayToSafeArray(intRefImage, destArray, vSequence.refImage.isSignedDataType(), vSequence.refImage.isSignedDataType());
			vSequence.refImage.setDataXY(c, destArray);
		}
		vSequence.refImage.dataChanged();
	}
	
	private void displayDetectViewer () {
		
		if (seqPositive != null ) {
			seqPositive.close();
			seqPositive=null;
		}
		
		if (seqNegative != null ) {
			seqNegative.close();
			seqNegative = null;
		}
		seqNegative = new SequenceVirtual();
		Viewer vNegative = new Viewer(seqNegative, false);
		seqNegative.setName("detectionImage");
		seqNegative.setImage(0,  0, IcyBufferedImageUtil.getSubImage(vSequence.refImage, rectangleAllCages));
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Point pt = viewer.getLocation();
		int height = viewer.getHeight();
		pt.y += height;

		if (vNegative != null) {
			vNegative.setLocation(pt);
			vNegative.setVisible(true);
		}
	}

	private void displayRefViewers () {
		
		if (seqPositive != null ) {
			seqPositive.close();
			seqPositive = null;
		}
		seqPositive = new SequenceVirtual();
		Viewer vPositive = new Viewer(seqPositive, false);
		seqPositive.setName("positiveImage");
		
		if (seqReference != null ) {
			seqReference.close();
			seqReference=null;
		}
		seqReference = new SequenceVirtual();
		Viewer vReference = new Viewer(seqReference, false);
		seqReference.setName("referenceImage");
		
		rectangleAllCages = null;
		for ( ROI2D roi: cages.cageLimitROIList) {
			Rectangle rect = roi.getBounds();
			if (rectangleAllCages == null)
				rectangleAllCages = new Rectangle(rect);
			else
				rectangleAllCages.add(rect);
		}
		seqReference.setImage(0,  0, IcyBufferedImageUtil.getSubImage(vSequence.refImage, rectangleAllCages));
		seqPositive.setImage(0,  0, IcyBufferedImageUtil.getSubImage(vSequence.refImage, rectangleAllCages));
		
		Point pt = viewer.getLocation();
		int height = viewer.getHeight();
		pt.y += height;
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (vReference != null) {
			vReference.setVisible(true);
			vReference.setLocation(pt);
			height = vReference.getHeight();
			pt.y += height;
			
			if (vPositive != null) {
				vPositive.setVisible(true);
				vPositive.setLocation(pt);
			}
		}
	}
	
	private void initParametersForDetection() {
		analyzeStep = vSequence.analysisStep;
		startFrame 	= (int) vSequence.analysisStart;
		endFrame 	= (int) vSequence.analysisEnd;
		if ( vSequence.nTotalFrames < endFrame+1 )
			endFrame = (int) vSequence.nTotalFrames - 1;
		nbframes = (endFrame - startFrame +1)/analyzeStep +1;
		
		cages.clear();
		cages.cageLimitROIList = ROI2DUtilities.getListofCagesFromSequence(vSequence);
		cageMaskList = ROI2DUtilities.getMask2DFromRoiList(cages.cageLimitROIList);
		Collections.sort(cages.cageLimitROIList, new DrosoTools.ROI2DNameComparator());
	}
	
	private void buildBackgroundImage() {

		ProgressFrame progress = new ProgressFrame("Build background image...");
		
		int nfliesRemoved = 0;
		vSequence.refImage = IcyBufferedImageUtil.getCopy(vSequence.loadVImage(startFrame));
		
		initParametersForDetection();
		initialflyRemoved.clear();
		for (int i=0; i < cages.cageLimitROIList.size(); i++)
			initialflyRemoved.add(false);
		
		viewer = vSequence.getFirstViewer();
		if (viewInternalImages) {	
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					displayRefViewers();
				}
			});
		}
		
		for (int t = startFrame +1 ; t <= endFrame && !stopFlag; t  += analyzeStep )
		{				
			IcyBufferedImage currentImage = vSequence.loadVImage(t);
			vSequence.currentFrame = t;
			viewer.setPositionT(t);
			viewer.setTitle(vSequence.getDecoratedImageName(t));
			
			IcyBufferedImage positiveImage = vSequence.subtractImages (currentImage, vSequence.refImage);
			if (seqPositive != null)
				seqPositive.setImage(0,  0, IcyBufferedImageUtil.getSubImage(positiveImage, rectangleAllCages));
			ROI2DArea roiAll = findFly (positiveImage, vSequence.cages.detect.threshold, detect.ichanselected, detect.btrackWhite );

			for ( int iroi = 1; iroi < cages.cageLimitROIList.size()-1; iroi++ )
			{
				BooleanMask2D bestMask = findLargestComponent(roiAll, iroi);		
				if ( bestMask != null ) {
					ROI2DArea flyROI = new ROI2DArea( bestMask );
					if (!initialflyRemoved.get(iroi)) {
						Rectangle2D rect = flyROI.getBounds2D();
						patchRectToReferenceImage(currentImage, rect);
						initialflyRemoved.set(iroi, true);
						nfliesRemoved ++;
						if (seqReference != null)
							seqReference.setImage(0,  0, IcyBufferedImageUtil.getSubImage(vSequence.refImage, rectangleAllCages));
						progress.setMessage( "Build background image: n flies removed =" + nfliesRemoved);
					}
				}
			}
			if (nfliesRemoved == cages.cageLimitROIList.size())
				break;
		}
		progress.close();
	}

	private BooleanMask2D findLargestComponent(ROI2DArea roiAll, int iroi) {
		
		ROI cageLimitROI = cages.cageLimitROIList.get(iroi);
		if ( cageLimitROI == null )
			return null;
		
		BooleanMask2D cageMask = cageMaskList.get(iroi);
		if (cageMask == null)
			return null;
		
		ROI2DArea roi = new ROI2DArea(roiAll.getBooleanMask( true ).getIntersection( cageMask ) );

		// find largest component in the threshold
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
		return bestMask;
	}
}