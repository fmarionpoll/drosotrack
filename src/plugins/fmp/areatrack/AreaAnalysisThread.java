package plugins.fmp.areatrack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

import icy.canvas.Canvas2D;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;

import icy.sequence.Sequence;
import icy.sequence.SequenceDataIterator;
import icy.system.profile.Chronometer;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.sequencevirtual.ImageTransformTools.TransformOp;
import plugins.fmp.sequencevirtual.ImageThresholdTools.ThresholdType;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.ImageOperations;
import plugins.fmp.sequencevirtual.Tools;

public class AreaAnalysisThread extends Thread
{
	
	private TransformOp transformop;
	SequenceVirtual vSequence = null;
	private ArrayList<ROI2D> roiList = null;

	private int startFrame = 0;
	private int endFrame = 99999999;
	private int analyzeStep = 1;
	private boolean measureROIsEvolution;
	private boolean measureROIsMove;
	private int thresholdForHeatMap = 230;
	
	public IcyBufferedImage resultImage = null;
	public Sequence resultSequence = null;
	public Viewer resultViewer = null;
	public Canvas2D resultCanvas = null;
	public ArrayList<MeasureAndName> results = null;
	
	private ThresholdType thresholdtype = ThresholdType.SINGLE;
	private ImageOperations imgOp1;
	private ImageOperations imgOp2;
	 
	// --------------------------------------------------------------------------------------
		
	public void setAnalysisThreadParameters (SequenceVirtual virtualSequence, 
			ArrayList<ROI2D> roiList, 
			int startFrame, 
			int endFrame, 
			TransformOp transf, 
			int thresholdForSurface,
			int thresholdForHeatMap, 
			boolean measureROIsEvolution, 
			boolean measureROIsMove)
	{
		vSequence = virtualSequence;
		this.roiList = roiList;
		this.startFrame = startFrame;
		this.endFrame = endFrame;
		this.transformop = transf;
;
		this.thresholdForHeatMap = thresholdForHeatMap;
		this.measureROIsEvolution = measureROIsEvolution;
		this.measureROIsMove = measureROIsMove;
		
		imgOp1 = new ImageOperations (virtualSequence);
		imgOp1.setTransform(transformop);
		if (thresholdtype == ThresholdType.SINGLE) 
			imgOp1.setThreshold(thresholdtype, thresholdForSurface);
		
		imgOp2 = new ImageOperations (virtualSequence);
		imgOp2.setTransform(TransformOp.REF_PREVIOUS);
		imgOp2.setThreshold(ThresholdType.SINGLE, thresholdForHeatMap);
				
		IcyBufferedImage image = vSequence.loadVImage(vSequence.currentFrame);
		resultImage = new IcyBufferedImage(image.getSizeX(), image.getSizeY(),image.getSizeC(), DataType.DOUBLE);
		resultSequence = new Sequence(resultImage);
		resultSequence.setName("Heatmap thresh:"+this.thresholdForHeatMap);
		resultViewer = new Viewer(resultSequence, false);
		resultCanvas = new Canvas2D(resultViewer);
	}
	
	public void setAnalysisThreadParametersColors (ThresholdType thresholdtype, int distanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		imgOp1.setThreshold(thresholdtype, colorarray, distanceType, colorthreshold);
		this.thresholdtype = thresholdtype;
	}
	
	@Override
	public void run()
	{
		// global parameters
		analyzeStep = vSequence.istep;
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
		int nrois = roiList.size();

		vSequence.data_raw = new int [nrois][nbframes];
		ArrayList<BooleanMask2D> areaMaskList = new ArrayList<BooleanMask2D>();
		vSequence.seriesname = new String[nrois];
		for (ROI2D roi: roiList)
		{
			String csName = roi.getName();
			vSequence.seriesname[iroi] = csName;
			areaMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));
			iroi++;
		}

		try {
			Viewer viewer = null;
			if (measureROIsEvolution)
				viewer = Icy.getMainInterface().getFirstViewer(vSequence);
			else 
				viewer = resultViewer;
			
			vSequence.beginUpdate();
							
			// ----------------- loop over all images of the stack

			for (int t = startFrame ; t <= endFrame && !isInterrupted(); t  += analyzeStep ) {				
				// update progression bar
				updateProgressionBar (t, nbframes, chrono, progress);

				if (measureROIsEvolution) {
					// load next image and compute threshold
					vSequence.currentFrame = t;
					viewer.setPositionT(t);
					viewer.setTitle(vSequence.getVImageName(t));

					// ------------------------ compute global mask
					IcyBufferedImage binaryMap = imgOp1.run();	
					boolean[] boolMap = imgOp1.convertToBoolean(binaryMap);
					BooleanMask2D maskAll2D = new BooleanMask2D(binaryMap.getBounds(), boolMap); 
					
					// ------------------------ loop over each ROI & count number of pixels above threshold
					for (int imask = 0; imask < areaMaskList.size(); imask++ )
					{
						BooleanMask2D areaMask = areaMaskList.get(imask);
						BooleanMask2D intersectionMask = maskAll2D.getIntersection( areaMask );
						int sum = intersectionMask.getNumberOfPoints();
						vSequence.data_raw[imask][t-startFrame]= sum;
					}
				}
				
				if (measureROIsMove) {
					// get difference image
					if (t < startFrame+20)
						continue;
					IcyBufferedImage binaryMap = imgOp2.run_nocache();
					int [] binaryArray = Array1DUtil.arrayToIntArray(binaryMap.getDataXY(0), binaryMap.isSignedDataType());
					double [] resultArray = resultImage.getDataXYAsDouble(0);
					for (int i= 0; i< binaryArray.length; i++) {
						if (binaryArray[i] != 0) 
							resultArray[i] += 1;
					}
				}
			}
		} 
		finally {
			progress.close();
			vSequence.endUpdate();
		}

		chrono.displayInSeconds();
		System.out.println("Computation finished.");
		
		if (measureROIsMove) {
			resultImage.dataChanged();
			resultViewer.setVisible(true);
			resultSequence.removeAllROI();
			resultSequence.addROIs(vSequence.getROI2Ds(), false);
			ArrayList<ROI2D> roiList2 = resultSequence.getROI2Ds();
			
			// ------ get big sum
			double sumall = 0;
			double countall = 0;
			int cmax = 3;
			for (int c=0; c< cmax; c++) {
				double[] resultDoubleArray = Array1DUtil.arrayToDoubleArray(resultImage.getDataXY(c), resultImage.isSignedDataType());
				for (int i=0; i< resultDoubleArray.length; i++) {
					sumall += resultDoubleArray[i];
				}
				countall += resultDoubleArray.length;
			}

			for (ROI2D roi: roiList2)
				areaMaskList.add(roi.getBooleanMask2D( 0 , 0, 1, true ));

			// ------------------------ loop over all the cages of the stack & count n pixels above threshold
			
			results = new ArrayList<MeasureAndName> ();
			for (ROI2D roi: roiList2) {
				SequenceDataIterator iterator = new SequenceDataIterator(resultSequence, roi, true, 0, 0 , -1);
				double sum = 0;
				double sample = 0;
				while (!iterator.done()) {
					sum += iterator.get();
					iterator.next();
					sample++;
				}
				sumall -= sum;
				countall -= sample;
				results.add(new MeasureAndName(roi.getName(), sum, sample));
			}
			results.add(new MeasureAndName("background", sumall, countall));
			
			// compute movements over the rest of the image and store it as reference
		}
	}
	
	void updateProgressionBar( int t, int nbframes, Chronometer chrono, ProgressFrame progress) {
		int pos = (int)(100d * (double)t / (double) nbframes);
		progress.setPosition( pos );
		int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
		int timeleft = (int) ((nbSeconds* nbframes /(t+1)) - nbSeconds);
		progress.setMessage( "Processing: " + pos + " % - Elapsed time: " + nbSeconds + " s - Estimated time left: " + timeleft + " s");
	}

}
