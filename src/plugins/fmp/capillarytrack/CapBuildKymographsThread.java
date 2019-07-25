package plugins.fmp.capillarytrack;


import java.util.ArrayList;
import java.util.Collections;

import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.fmp.drosoSequence.SequencePlus;
import plugins.fmp.drosoSequence.SequenceVirtual;
import plugins.fmp.drosoTools.DufourRigidRegistration;
import plugins.fmp.drosoTools.ProgressChrono;
import plugins.fmp.drosoTools.DrosoTools;
import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;


//-------------------------------------------
//	public class BuildKymographsThread extends Thread  
public class CapBuildKymographsThread implements Runnable 
{
	public SequenceVirtual vSequence = null;
	public int analyzeStep = 1;
	public int startFrame = 1;
	public int endFrame = 99999999;
	public int diskRadius = 5;
	public ArrayList <SequencePlus> kymographArrayList 	= null;
	public boolean doRegistration = false;
	public boolean doStop = false;
	public boolean threadRunning = false;
	
	private ArrayList<double []> sourceValuesList = null;
	private ArrayList<ArrayList<ArrayList<int[]>>> masksArrayList = new ArrayList<ArrayList<ArrayList<int[]>>>();
	private ArrayList<ArrayList <double []>> rois_tabValuesList = new ArrayList<ArrayList <double []>>();
	private Viewer sequenceViewer = null;
	IcyBufferedImage workImage = null; 
	Sequence s = new Sequence();
	
	@Override
	public void run() {

		if (vSequence == null)
			return;
		if (startFrame < 0) 
			startFrame = 0;
		if (endFrame >= (int) vSequence.nTotalFrames || endFrame < 0) 
			endFrame = (int) vSequence.nTotalFrames-1;
		int nbframes = endFrame - startFrame +1;
		ProgressChrono progressBar = new ProgressChrono("Processing started");
		progressBar.initStuff(nbframes);
		doStop = false;
		threadRunning = true;

		initKymographs();
		int vinputSizeX = vSequence.getSizeX();
		vSequence.beginUpdate();
		sequenceViewer = Icy.getMainInterface().getFirstViewer(vSequence);
		int ipixelcolumn = 0;
		getImageAndUpdateViewer (startFrame);
		s.addImage(0, workImage);
		s.addImage(1, workImage);

		for (int t = startFrame ; t <= endFrame; t += analyzeStep, ipixelcolumn++ )
		{
			progressBar.updatePositionAndTimeLeft(t);
			if (!getImageAndUpdateViewer (t))
				continue;
			if (doRegistration ) {
				adjustImage();
			}
			transferWorkImageToDoubleArrayList ();
			
			for (int iroi=0; iroi < vSequence.capillaries.capillariesArrayList.size(); iroi++)
			{
				SequencePlus kymographSeq = kymographArrayList.get(iroi);
				ArrayList<ArrayList<int[]>> masks = masksArrayList.get(iroi);	
				ArrayList <double []> tabValuesList = rois_tabValuesList.get(iroi);
				final int kymographSizeX = kymographSeq.getSizeX();
				final int t_out = ipixelcolumn;

				for (int chan = 0; chan < vSequence.getSizeC(); chan++) 
				{ 
					double [] tabValues = tabValuesList.get(chan); 
					double [] sourceValues = sourceValuesList.get(chan);
					int cnt = 0;
					for (ArrayList<int[]> mask:masks)
					{
						double sum = 0;
						for (int[] m:mask)
							sum += sourceValues[m[0] + m[1]*vinputSizeX];
						if (mask.size() > 1)
							sum = sum/mask.size();
						tabValues[cnt*kymographSizeX + t_out] = sum; 
						cnt ++;
					}
				}
			}
			if (doStop) { 
				t=endFrame; 
			}
		}

		vSequence.endUpdate();
		System.out.println("Elapsed time (s):" + progressBar.getSecondsSinceStart());
		progressBar.close();
		
		for (int iroi=0; iroi < vSequence.capillaries.capillariesArrayList.size(); iroi++)
		{
			SequencePlus kymographSeq = kymographArrayList.get(iroi);
			kymographSeq.dataChanged();
		}
		threadRunning = false;
	}
	
	// -------------------------------------------
	private boolean getImageAndUpdateViewer(int t) {
		workImage = getImageFromSequence(t); 
		sequenceViewer.setPositionT(t);
		sequenceViewer.setTitle(vSequence.getDecoratedImageName(t));
		if (workImage == null)
			return false;
		return true;
	}
	
	private boolean transferWorkImageToDoubleArrayList() {
		
		sourceValuesList = new ArrayList<double []>();
		for (int chan = 0; chan < vSequence.getSizeC(); chan++) 
		{
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
			sourceValuesList.add(sourceValues);
		}
		return true;
	}
	
	private void initKymographs() {

		int sizex = vSequence.getSizeX();
		int sizey = vSequence.getSizeY();
		vSequence.capillaries.extractLinesFromSequence(vSequence);
		int numC = vSequence.getSizeC();
		double fimagewidth =  1 + (endFrame - startFrame )/analyzeStep;
		int imagewidth = (int) fimagewidth;
	
		for (int iroi=0; iroi < vSequence.capillaries.capillariesArrayList.size(); iroi++)
		{
			ROI2DShape roi = vSequence.capillaries.capillariesArrayList.get(iroi);
			ArrayList<ArrayList<int[]>> mask = new ArrayList<ArrayList<int[]>>();
			masksArrayList.add(mask);
			initExtractionParametersfromROI(roi, mask, diskRadius, sizex, sizey);
			
			IcyBufferedImage bufImage = new IcyBufferedImage(imagewidth, mask.size(), numC, DataType.DOUBLE);
			SequencePlus kymographSeq = kymographArrayList.get(iroi);
			kymographSeq.addImage(0, bufImage);
			String cs = kymographSeq.getName();
			if (!cs.contentEquals(roi.getName()))
				kymographSeq.setName(roi.getName());
			ArrayList <double []> tabValuesList = new ArrayList <double []>();
			for (int chan = 0; chan < numC; chan++) 
			{
				double[] tabValues = kymographSeq.getImage(0, 0).getDataXYAsDouble(chan); 
				tabValuesList.add(tabValues);
			}
			rois_tabValuesList.add(tabValuesList);
		}
		Collections.sort(kymographArrayList, new DrosoTools.SequenceNameComparator()); 
	}
	
	private double initExtractionParametersfromROI( ROI2DShape roi, ArrayList<ArrayList<int[]>> masks,  double diskRadius, int sizex, int sizey)
	{
		CubicSmoothingSpline xSpline 	= Util.getXsplineFromROI((ROI2DShape) roi);
		CubicSmoothingSpline ySpline 	= Util.getYsplineFromROI((ROI2DShape) roi);
		double length 					= Util.getSplineLength((ROI2DShape) roi);
		double len = 0;
		while (len < length)
		{
			ArrayList<int[]> mask = new ArrayList<int[]>();
			double x = xSpline.evaluate(len);
			double y = ySpline.evaluate(len);
			double dx = xSpline.derivative(len);
			double dy = ySpline.derivative(len);
			double ux = dy/Math.sqrt(dx*dx + dy*dy);
			double uy = -dx/Math.sqrt(dx*dx + dy*dy);
			double tt = -diskRadius;
			while (tt <= diskRadius)
			{
				int xx = (int) Math.round(x + tt*ux);
				int yy = (int) Math.round(y + tt*uy);
				if (xx >= 0 && xx < sizex && yy >= 0 && yy < sizey)
					mask.add(new int[]{xx, yy});
				tt += 1d;
			}
			masks.add(mask);			
			len ++;
		}
		return length;
	}
	
	private IcyBufferedImage getImageFromSequence(int t) {
		IcyBufferedImage workImage = vSequence.loadVImage(t);
		vSequence.currentFrame = t;
		if (workImage == null) {
			System.out.println("Error reading image: " + t + " ... trying again"  );
			workImage = vSequence.loadVImage(t);
			if (workImage == null) {
				System.out.println("Fatal error occurred while reading file "+ vSequence.getFileName(t) + " -image: " + t);
				return null;
			}
		}
		else
		{
			sequenceViewer.setPositionT(t);
			sequenceViewer.setTitle(vSequence.getDecoratedImageName(t)); 
		}
		return workImage;
	}
	
	private void adjustImage() {
		s.setImage(1, 0, workImage);
		int referenceChannel = 1;
		int referenceSlice = 0;
		DufourRigidRegistration.correctTemporalTranslation2D(s, referenceChannel, referenceSlice);
        boolean rotate = DufourRigidRegistration.correctTemporalRotation2D(s, referenceChannel, referenceSlice);
        if (rotate) 
        	DufourRigidRegistration.correctTemporalTranslation2D(s, referenceChannel, referenceSlice);
//        RigidRegistration.correctTemporalTranslation2D(s, 0, 0);
//        boolean rotate = RigidRegistration.correctTemporalRotation2D(s, 0, 0);
//        if (rotate) RigidRegistration.correctTemporalTranslation2D(s, 0, 0);
        workImage = s.getLastImage(1);
	}

}
