package plugins.fmp.capillarytrack;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

import icy.gui.frame.progress.ProgressFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.system.profile.Chronometer;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.Tools;
import plugins.kernel.roi.roi2d.ROI2DShape;

import plugins.nchenouard.kymographtracker.Util;
import plugins.nchenouard.kymographtracker.spline.CubicSmoothingSpline;

//-------------------------------------------
	public class BuildKymographsThread extends Thread implements ActionListener 
	{
		public SequenceVirtual vinputSequence = null;
		public int analyzeStep = 1;
		public int startFrame = 1;
		public int endFrame = 99999999;
		public int diskRadius = 5;
		public ArrayList <SequencePlus> kymographArrayList 		= null; //new ArrayList <SequencePlus> ();		// list of kymograph sequences
		private Viewer sequenceViewer = null;
		public ProgressFrame progress = null;
		
		private ArrayList<ArrayList<ArrayList<int[]>>> masksArrayList 	= new ArrayList<ArrayList<ArrayList<int[]>>>();
		private ArrayList<ArrayList <double []>> rois_tabValuesList = new ArrayList<ArrayList <double []>>();
		private double nbframes = 0;
		private int nchannels = 0;
		
		@Override
		public void run () 
		{
			if (vinputSequence == null)
				return;
			sequenceViewer = Icy.getMainInterface().getFirstViewer(vinputSequence);

			// prepare loop
			kymographArrayList.clear();

			// loop over Rois attached to the current sequence
			if (startFrame < 0) 
				startFrame = 0;
			if (endFrame >= (int) vinputSequence.nTotalFrames || endFrame < 0) 
				endFrame = (int) vinputSequence.nTotalFrames-1;
			nbframes = endFrame - startFrame +1;
			
			// send some info
			progress = new ProgressFrame("Processing started");
			progress.setLength(nbframes);

			nchannels =  vinputSequence.getSizeC();
			initKymographs();
			
			progress.setPosition(startFrame);
			Chronometer chrono = new Chronometer("Tracking computation" );
			int  nbSeconds = 0;

			int vinputSizeX = vinputSequence.getSizeX();
			vinputSequence.beginUpdate();

			for (int t = startFrame ; t <= endFrame && !isInterrupted(); t  += analyzeStep )
			{
				// update progression bar
				int pos = (int)(100d * (double)t / nbframes);
				progress.setPosition( t );
				nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
				double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
				progress.setMessage( "Processing: " + pos + "% - Estimated time left: " + (int) timeleft + " s");

				// get image to be processed and transfer it into sourceValues array (1 per color chan)
				IcyBufferedImage workImage = getImageFromSeq(t);
				if (workImage == null)
					continue;
				ArrayList<double []> sourceValuesList = new ArrayList<double []>();
				for (int chan = 0; chan < nchannels; chan++) {
					double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
					sourceValuesList.add(sourceValues);
				}

				for (int iroi=0; iroi < vinputSequence.capillariesArrayList.size(); iroi++)
				{
					SequencePlus kymographSeq = kymographArrayList.get(iroi);
					ArrayList<ArrayList<int[]>> masks = masksArrayList.get(iroi);	
					ArrayList <double []> tabValuesList = rois_tabValuesList.get(iroi);
					final int kymographSizeX = kymographSeq.getSizeX();
					final int t_out = t - startFrame;

					for (int chan = 0; chan < nchannels; chan++) { 

						double [] tabValues = tabValuesList.get(chan); 
						double [] sourceValues = sourceValuesList.get(chan);
						int cnt = 0;
						for (ArrayList<int[]> mask:masks)
						{
							double sum = 0;
							for (int[] m:mask)
								sum += sourceValues[m[0] + m[1]*vinputSizeX]; //  m[0] and m[1] are xy coords

							if (mask.size() > 1)
								sum = sum/mask.size();
							tabValues[cnt*kymographSizeX + t_out] = sum; 
							cnt ++;
						}
					}
				}
			}
			vinputSequence.endUpdate();
			/**/
			for (int iroi=0; iroi < vinputSequence.capillariesArrayList.size(); iroi++)
			{
				SequencePlus kymographSeq = kymographArrayList.get(iroi);
				kymographSeq.dataChanged();
			}
			/**/
			progress.close();
			System.out.println("Elapsed time (s):" + nbSeconds);
		}
		
		private IcyBufferedImage getImageFromSeq(int t) {
			IcyBufferedImage workImage = vinputSequence.loadVImage(t);
			vinputSequence.currentFrame = t;
			if (workImage == null) {
				// try another time
				System.out.println("Error reading image: " + t + " ... trying again"  );
				vinputSequence.removeImage(t, 0);
				workImage = vinputSequence.loadVImage(t);
				if (workImage == null) {
					System.out.println("Fatal error occurred while reading file "+ vinputSequence.getFileName(t) + " -image: " + t);
					return null;
				}
			}
			else
			{
				sequenceViewer.setPositionT(t);
				sequenceViewer.setTitle(vinputSequence.getVImageName(t)); 
			}
			return workImage;
		}
		
		// -------------------------------------------
		private void initKymographs() {
			
			int sizex = vinputSequence.getSizeX();
			int sizey = vinputSequence.getSizeY();
			
			// build image kymographs which will be filled then
			vinputSequence.getCapillariesArrayList();
			int numC = vinputSequence.getSizeC();
			if (numC <1)
				numC = 3;
						
			for (ROI2DShape roi:vinputSequence.capillariesArrayList)
			{

				ArrayList<ArrayList<int[]>> masks = new ArrayList<ArrayList<int[]>>();
				masksArrayList.add(masks);
				initExtractionParametersfromROI(roi, masks, diskRadius, sizex, sizey);

				SequencePlus kymographSeq = new SequencePlus();
				kymographSeq.setName(roi.getName());
				kymographArrayList.add(kymographSeq);

				// load first image of each kymograph
				IcyBufferedImage bufImage = new IcyBufferedImage((int) nbframes, masks.size(), numC, DataType.DOUBLE);
				kymographSeq.setImage(0, 0, bufImage);
				ArrayList <double []> tabValuesList = new ArrayList <double []>();
				for (int chan = 0; chan < nchannels; chan++) 
				{
					double[] tabValues = kymographSeq.getImage(0, 0).getDataXYAsDouble(chan); 
					tabValuesList.add(tabValues);
				}
				rois_tabValuesList.add(tabValuesList);
			}
			Collections.sort(kymographArrayList, new Tools.SequenceNameComparator()); 
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
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	}
