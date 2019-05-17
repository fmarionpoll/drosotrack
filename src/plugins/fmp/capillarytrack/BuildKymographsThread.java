package plugins.fmp.capillarytrack;

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
	public class BuildKymographsThread extends Thread  
	{
		public SequenceVirtual vSequence = null;
		public int analyzeStep = 1;
		public int startFrame = 1;
		public int endFrame = 99999999;
		public int diskRadius = 5;
		public ArrayList <SequencePlus> kymographArrayList 	= null;		// list of kymograph sequences
		
		private ArrayList<ArrayList<ArrayList<int[]>>> masksArrayList 	= new ArrayList<ArrayList<ArrayList<int[]>>>();
		private ArrayList<ArrayList <double []>> rois_tabValuesList = new ArrayList<ArrayList <double []>>();
		private Viewer sequenceViewer = null;
		
		@Override
		public void run () 
		{
			if (vSequence == null)
				return;

			// loop over Rois attached to the current sequence
			if (startFrame < 0) 
				startFrame = 0;
			if (endFrame >= (int) vSequence.nTotalFrames || endFrame < 0) 
				endFrame = (int) vSequence.nTotalFrames-1;
			initKymographs();

			// send some info
			int nbframes = endFrame - startFrame +1;
			ProgressFrame progress = new ProgressFrame("Processing started");
			progress.setLength(nbframes);
			progress.setPosition(startFrame);
			Chronometer chrono = new Chronometer("Tracking computation" );
			int  nbSeconds = 0;

			int vinputSizeX = vSequence.getSizeX();
			vSequence.beginUpdate();
			sequenceViewer = Icy.getMainInterface().getFirstViewer(vSequence);
			int ipixelcolumn = 0;

			for (int t = startFrame ; t <= endFrame && !isInterrupted(); t  += analyzeStep, ipixelcolumn++ )
			{
				// update progression bar
				int pos = (int)(100d * (double)t / nbframes);
				progress.setPosition( t );
				nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
				double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
				progress.setMessage( "Processing: " + pos + "% - Estimated time left: " + (int) timeleft + " s");

				// get image to be processed and transfer it into sourceValues array (1 per color chan)
				IcyBufferedImage workImage = getImageFromSequence(t); 
				sequenceViewer.setPositionT(t);
				sequenceViewer.setTitle(vSequence.getVImageName(t)); 
				
				if (workImage == null)
					continue;
				ArrayList<double []> sourceValuesList = new ArrayList<double []>();
				
				for (int chan = 0; chan < vSequence.getSizeC(); chan++) 
				{
					double [] sourceValues = Array1DUtil.arrayToDoubleArray(workImage.getDataXY(chan), workImage.isSignedDataType()); 
					sourceValuesList.add(sourceValues);
				}

				for (int iroi=0; iroi < vSequence.capillariesArrayList.size(); iroi++)
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
								sum += sourceValues[m[0] + m[1]*vinputSizeX]; //  m[0] and m[1] are xy coords
							if (mask.size() > 1)
								sum = sum/mask.size();
							tabValues[cnt*kymographSizeX + t_out] = sum; 
							cnt ++;
						}
					}
				}
			}
			vSequence.endUpdate();

			for (int iroi=0; iroi < vSequence.capillariesArrayList.size(); iroi++)
			{
				SequencePlus kymographSeq = kymographArrayList.get(iroi);
				kymographSeq.dataChanged();
			}
			/**/
			progress.close();
			System.out.println("Elapsed time (s):" + nbSeconds);
		}
		
		// -------------------------------------------
		private void initKymographs() {
			
			int sizex = vSequence.getSizeX();
			int sizey = vSequence.getSizeY();
			vSequence.keepOnly2DLines_CapillariesArrayList();
			int numC = vSequence.getSizeC();
			double fimagewidth =  1 + (endFrame - startFrame )/analyzeStep;
			int imagewidth = (int) fimagewidth;
		
			for (int iroi=0; iroi < vSequence.capillariesArrayList.size(); iroi++)
			{
				ROI2DShape roi = vSequence.capillariesArrayList.get(iroi);
				ArrayList<ArrayList<int[]>> mask = new ArrayList<ArrayList<int[]>>();
				masksArrayList.add(mask);
				initExtractionParametersfromROI(roi, mask, diskRadius, sizex, sizey);
				
				IcyBufferedImage bufImage = new IcyBufferedImage(imagewidth, mask.size(), numC, DataType.DOUBLE);
				SequencePlus kymographSeq = kymographArrayList.get(iroi);
				kymographSeq.addImage(bufImage);
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
		
		// TODO
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
				sequenceViewer.setTitle(vSequence.getVImageName(t)); 
			}
			return workImage;
		}

	}
