package plugins.fmp.sequencevirtual;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jfree.data.xy.XYSeries;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import icy.canvas.Canvas2D;
import icy.gui.dialog.LoaderDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.dialog.SaveDialog;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.image.ImageUtil;
import icy.main.Icy;
import icy.math.ArrayMath;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.sequence.edit.ROIAddsSequenceEdit;
import icy.system.thread.ThreadUtil;
import icy.type.collection.array.Array1DUtil;
import icy.util.XMLUtil;

import plugins.fab.MiceProfiler.XugglerAviFile;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class SequenceVirtual extends Sequence 
{

	private XugglerAviFile 	aviFile = null;
	private String [] 		listFiles = null;
	private String 			csFileName = null;
	private final static String[] acceptedTypes = {".jpg", ".jpeg", ".bmp"};
	private String			directory = null;
	private static final String XML_KEY_ID = "ID";
	private IcyBufferedImage refImage = null;
	
	public String	sourceFile = null;
	public enum Status { AVIFILE, FILESTACK, REGULAR, FAILURE };
	public boolean	bBufferON = false;
	public Status 	status;
	public int 		currentFrame = 0;
	public int		nTotalFrames = 0;
	public int 		istep = 1;
	public boolean 	flag = false;
	public String	comment = null;
	public double 	capillaryVolume = 1.;
	public double 	capillaryPixels = 1.;
	public int		capillariesGrouping = 1;
	public long		analysisStart = 0;
	public long 	analysisEnd	= 99999999;
	public int		threshold = -1;
	public VImageBufferThread bufferThread = null;
	public ArrayList <ROI2DShape> capillariesArrayList 	= new ArrayList <ROI2DShape>();			// list of ROIs describing capillaries (e.g. profiles to follow)
	public XYSeries[] results = null;
	public XYSeries[] pixels = null;
	
	
	// ----------------------------------------
	public SequenceVirtual () 
	{
		status = Status.REGULAR;
	}

	public SequenceVirtual (String csFile)
	{
		loadSequenceVirtualAVI(csFile);
	}

	public SequenceVirtual (String [] list, String directory)
	{
		loadSequenceVirtual(list, directory);
		filename = directory + ".xml";
	}

	public static boolean acceptedFileType(String name) {
		/* 
		 * Returns true if 'name' includes one of the accepted types stored in the "accepted" list 
		 */
		if (name==null) return false;
		for (int i=0; i<acceptedTypes.length; i++) {
			if (name.endsWith(acceptedTypes[i]))
				return true;
		}
		return false;
	}	

	@Override
	public void close()
	{
		vImageBufferThread_STOP();
		super.close();
	}
	
	public void displayRelativeFrame( int nbFrame )
	{
		int currentTime = getT()+ nbFrame ;
		if (currentTime < 0)
			currentTime = 0;
		if (currentTime > nTotalFrames-1)
			currentTime = (int) (nTotalFrames -1);

		final Viewer v = Icy.getMainInterface().getFirstViewer(this);
		if (v != null) 
			v.setPositionT(currentTime);
		displayImageAt(currentTime);
	}

	public void displayImageAt(int t)
	{
		currentFrame = t;
		if (getImage(t, 0) == null)
		{
			final boolean wasEmpty = getNumImage() == 0;
			setCurrentVImage (t);
			if (wasEmpty)
			{
				for (Viewer viewer : getViewers())
				{
					if (viewer.getCanvas() instanceof Canvas2D)
						((Canvas2D) viewer.getCanvas()).fitCanvasToImage();
				}
			}
		}
	}

	public String getDirectory () {
		return directory;
	}

	@Override
	public IcyBufferedImage getImage(int t, int z, int c) 
	{
		setVImageName(t);
		IcyBufferedImage image =  loadVImage(t);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
//	@Override
//	public IcyBufferedImage getImage(int t, int z)
//	{
//		IcyBufferedImage image;
//		if (t == currentFrame) {
//			image = super.getImage(t, 0);
//		}
//		else
//		{
//		  image =  loadVImage(t);
//		}
//		setVImageName(t);
//		return image;
//	}

	public IcyBufferedImage getImageTransf(int t, int z, int c, int transform) 
	{
		//setVImageName(t);
		IcyBufferedImage image =  loadVImageTransf(t, transform);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
	public IcyBufferedImage loadVImageTransf(int t, int transform)
	{
		IcyBufferedImage ibufImage = loadVImage(t);
		switch (transform) {
			// subtract image n-1
			case 1:
			{
				int t0 = t-1;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = loadVImage(t0);
				ibufImage = subtractImages (ibufImage, ibufImage0);
			}	
				break;
			// subtract reference image
			case 2:
			{
				if (refImage == null)
					refImage = loadVImage(0);
				ibufImage = subtractImages (ibufImage, refImage);
			}
				break;
			default:
				break;
		}
		return ibufImage;
	}
	
	public void setRefImageForSubtraction(int t)
	{
		if (t < 0)
			refImage = null;
		else
			refImage = loadVImage(t);
	}
	
	public String[] getListofFiles() {
		return listFiles;
	}

	@Override
	public int getSizeT() {
		return (int) nTotalFrames;
	}

	public int getT() {
		return currentFrame;
	}

	public double getVData(int t, int z, int c, int y, int x)
	{
		final IcyBufferedImage img = loadVImage(t);
		if (img != null)
			return img.getData(x, y, c);
		return 0d;
	}

	public String getVImageName(int t)
	{
		String csTitle = "["+t+ "/" + nTotalFrames + " V] : ";
		if (status == Status.FILESTACK) 
			csTitle += listFiles[t];
		else //  if ((status == Status.AVIFILE))
			csTitle += csFileName;
		return csTitle;
	}

	public String getFileName(int t) {
		String csName = null;
		if (status == Status.FILESTACK) 
			csName = listFiles[t];
		else if (status == Status.AVIFILE)
			csName = csFileName;
		return csName;
	}
	
	public boolean isFileStack() {
		return (status == Status.FILESTACK);
	}
	
	public String loadInputVirtualStack() {

		LoaderDialog dialog = new LoaderDialog(false);
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    if (selectedFiles[0].isDirectory())
	    	directory = selectedFiles[0].getAbsolutePath();
	    else
	    	directory = selectedFiles[0].getParentFile().getAbsolutePath();
	    	//FilenameUtils.getFullPathNoEndSeparator(selectedFiles[0].getAbsolutePath());
		if (directory == null)
			return null;
		
		String [] list;
		if (selectedFiles.length == 1) {
			list = (new File(directory)).list();
			if (list ==null)
				return null;
			
			if (!(selectedFiles[0].isDirectory()) && selectedFiles[0].getName().toLowerCase().contains(".avi")) {
				loadSequenceVirtualAVI(selectedFiles[0].getAbsolutePath());
				return directory;
			}
		}
		else
		{
			list = new String[selectedFiles.length];
			  for (int i = 0; i < selectedFiles.length; i++) {
				if (selectedFiles[i].getName().toLowerCase().contains(".avi"))
					continue;
			    list[i] = selectedFiles[i].getAbsolutePath();
			}
		}
		loadSequenceVirtual(list, directory);
		return directory;
	}

	public String loadInputVirtualFromNameSavedInRoiXML()
	{
		if (sourceFile != null)
			loadInputVirtualFromName(sourceFile);
		return sourceFile;
	}
	
	public void loadInputVirtualFromName(String name)
	{
		if (name.toLowerCase().contains(".avi"))
			loadSequenceVirtualAVI(name);
		else
			loadSequenceVirtualFromName(name);
	}
	
	public IcyBufferedImage loadVImage(int t)
	{
		IcyBufferedImage ibufImage = super.getImage(t, 0);
		// not found : load from file
		if (ibufImage == null)
		{
			BufferedImage buf =null;
			if (status == Status.FILESTACK) {
				buf = ImageUtil.load(listFiles[t]);
				ImageUtil.waitImageReady(buf);
				if (buf == null)
					return null;
								
			}
			else if (status == Status.AVIFILE) {
				buf = aviFile.getImage(t);
			}
			ibufImage=  IcyBufferedImage.createFrom(buf);
		}	
		return ibufImage;
	}
	
	public boolean setCurrentVImage(int t)
	{
		BufferedImage bimage = loadVImage(t);
		if (bimage == null)
			return false;

		super.setImage(t, 0, bimage);
		setVImageName(t);		
		currentFrame = t;
		return true;
	}

	@Override
	public void setImage(int t, int z, BufferedImage bimage) throws IllegalArgumentException 
	{
		/* setImage overloaded
		 * caveats: 
		 * (1) this routine deals only with 2D images i.e. z is not used (z= 0), 
		 * (2) the virtual stack is left untouched - no mechanism is provided to "save" modified images to the disk - so actually
		 * 	   setImage here is equivalent to "load image" from disk - the buffered image parameter is not used if the stack is virtual
		 * @see icy.sequence.Sequence#setImage(int, int, java.awt.image.BufferedImage)
		 */
		
		if ((status == Status.FILESTACK) || (status == Status.AVIFILE) )
			setCurrentVImage(t);
		else 
			super.setImage(t, 0, bimage);
	}

	public void setVImage(int t)
	{
		IcyBufferedImage ibuf = loadVImage(t);
		if (ibuf != null)
			super.setImage(t, 0, ibuf);
	}

	public String[] keepOnlyAcceptedNames(String[] rawlist) {
		// -----------------------------------------------
		// subroutines borrowed from FolderOpener
		/* Keep only "accepted" names (file extension)*/
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if ( !acceptedFileType(name) )
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;

		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}

	public void vImageBufferThread_START (int numberOfImageForBuffer) {
		vImageBufferThread_STOP();

		bufferThread = new VImageBufferThread(this, numberOfImageForBuffer);
		bufferThread.setName("Buffer Thread");
		bufferThread.setPriority(Thread.NORM_PRIORITY);
		bufferThread.start();
	}
	
	public void vImageBufferThread_STOP() {

		if (bufferThread != null)
		{
			bufferThread.interrupt();
			try {
				bufferThread.join();
			}
			catch (final InterruptedException e1) { e1.printStackTrace(); }
		}
		// TODO clean buffer by removing images?
	}

	public boolean xmlReadROIsAndData() {

		String [] filedummy = new String[1];
		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				filedummy[0] = Tools.openFile(directory,"xml");
			}
		}, true);
		String csFile = filedummy[0];
		return xmlReadROIsAndData(csFile);
	}
	
	public boolean xmlReadROIsAndData(String csFileName) {
		
		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				final List<ROI> rois = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
				Collections.sort(rois, new Tools.ROINameComparator()); 

				beginUpdate();
				try  {  
					for (ROI roi : rois)  {
						addROI(roi);
					}
					xmlReadCapillaryTrackParameters(doc);
				}
				finally {
					endUpdate();
				}
				// add to undo manager
				addUndoableEdit(new ROIAddsSequenceEdit(this, rois) {
					@Override
					public String getPresentationName() {
						if (getROIs().size() > 1)
							return "ROIs loaded from XML file";
						return "ROI loaded from XML file"; };
				});
				return true;
			}
		}
		return false;
	}

	public boolean xmlWriteROIsAndData(String name) {

		String [] filedummy = new String[1];
		ThreadUtil.invoke (new Runnable() {
			@Override
			public void run() {
				filedummy[0] = SaveDialog.chooseFile("Save roi(s)...", directory, name);
			}
		}, true);

		String csFile = filedummy[0];
		return xmlWriteROIsAndDataNoQuestion(csFile);
	}
	
	public boolean xmlWriteROIsAndDataNoQuestion(String csFile) {

		if (csFile != null) 
		{
			final List<ROI> rois = getROIs(true);
			if (rois.size() > 0)
			{
				final Document doc = XMLUtil.createDocument(true);
				if (doc != null)
				{
					ROI.saveROIsToXML(XMLUtil.getRootElement(doc), rois);
					xmlWriteCapillaryTrackParameters (doc);
					XMLUtil.saveDocument(doc, csFile);
					return true;
				}
			}
		}
		return false;
	}

	public class VImageBufferThread extends Thread {

		/**
		 * pre-fetch files / companion to SequenceVirtual
		 */

		private int fenetre = 100;
		private int span = 50;

		public VImageBufferThread() {
			bBufferON = true;
		}

		public VImageBufferThread(SequenceVirtual vseq, int depth) {
			fenetre = depth;
			span = fenetre/2;
			bBufferON = true;
		}
		
		public void setFenetre (int depth) {
			fenetre = depth;
			span = fenetre/2;
		}

		public void getFenetre (int depth) {
			fenetre = depth;
			span = fenetre/2;
		}

		public int getCurrentBufferLoadPercent()
		{
			int currentBufferPercent = 0;
			int frameStart = currentFrame-span; 
			int frameEnd = currentFrame + span;
			if (frameStart < 0) 
				frameStart = 0;
			if (frameEnd >= (int) nTotalFrames) 
				frameEnd = (int) nTotalFrames-1;

			float nbImage = 1;
			float nbImageLoaded = 1;
			for (int t = frameStart; t <= frameEnd; t+= istep)
			{
				nbImage++;
				if (getImage(t, 0) != null)
					nbImageLoaded++;

			}
			currentBufferPercent = (int) (nbImageLoaded * 100f / nbImage);
			return currentBufferPercent;
		}

		@Override
		public void run()
		{
			try
			{
				while (!isInterrupted())
				{
					final int cachedCurrentFrame = currentFrame;

					int frameStart 	= currentFrame - span;
					int frameEnd 	= currentFrame + span;
					if (frameStart < 0) 
						frameStart = 0;
					if (frameEnd > nTotalFrames) 
						frameEnd = nTotalFrames;
					
					ThreadUtil.sleep(100);

					// clean all images except those within the buffer 
					for (int t = 0; t < nTotalFrames-1 ; t++) {
						if (t < frameStart || t > frameEnd)
							removeImage(t, 0);
						
						if (isInterrupted())
							return;
						if (cachedCurrentFrame != currentFrame)
							break;
					}
					if (cachedCurrentFrame != currentFrame)
						continue;

					
					for (int t = frameStart; t < frameEnd ; t+= istep)
					{					
						setVImage(t);
						if(isInterrupted())
							return;
						if (cachedCurrentFrame != currentFrame)
							break;
					}
				}			
			}
			catch (final Exception e) 
			{ e.printStackTrace(); }
		}
	}

	public void getCapillariesArrayList() {

		capillariesArrayList.clear();
		ArrayList<ROI2D> list = getROI2Ds();
		 
		for (ROI2D roi:list)
		{
			if ((roi instanceof ROI2DShape) == false)
				continue;
			if (roi instanceof ROI2DLine && roi.getName().contains("line"))
				capillariesArrayList.add((ROI2DShape)roi);
		}
		Collections.sort(capillariesArrayList, new Tools.ROI2DNameComparator()); 
	}
	
	// -----------------------------------------------------------
	private IcyBufferedImage subtractImages (IcyBufferedImage image1, IcyBufferedImage image2) {
		/* algorithm borrowed from  Perrine.Paul-Gilloteaux@univ-nantes.fr in  EC-CLEM
		 * original function: private IcyBufferedImage substractbg(Sequence ori, Sequence bg,int t, int z) 
		 */
		IcyBufferedImage result = new IcyBufferedImage(image1.getSizeX(), image1.getSizeY(),image1.getSizeC(), image1.getDataType_());
		for (int c=0; c<image1.getSizeC(); c++){
			Object image1Array = image1.getDataXY(c);
			Object image2Array = image2.getDataXY(c);
			double[] img1DoubleArray = Array1DUtil.arrayToDoubleArray(image1Array, image1.isSignedDataType());
			double[] img2DoubleArray = Array1DUtil.arrayToDoubleArray(image2Array, image2.isSignedDataType());
			ArrayMath.subtract(img1DoubleArray, img2DoubleArray, img1DoubleArray);

			double[] dummyzeros=Array1DUtil.arrayToDoubleArray(result.getDataXY(c), result.isSignedDataType());
			ArrayMath.max(img1DoubleArray, dummyzeros, img1DoubleArray);
			Array1DUtil.doubleArrayToArray(img1DoubleArray, result.getDataXY(c));
		}
		result.dataChanged();
		return result;
	}
	
	private void loadSequenceVirtualFromName(String name) 
	{
		File filename = new File (name);
//		String ext = ".";
		if (filename.isDirectory())
	    	directory = filename.getAbsolutePath();
	    else {
	    	//directory = FilenameUtils.getFullPathNoEndSeparator(filename.getAbsolutePath());
	    	directory = filename.getParentFile().getAbsolutePath();
	    }
		if (directory == null) {
			status = Status.FAILURE;
			return;
		}
		String [] list;
		File fdir = new File(directory);
		boolean flag = fdir.isDirectory();
		if (!flag)
			return;
		list = fdir.list();
		// TODO: change directory into a pathname
		if (list != null)
			loadSequenceVirtual(list, directory);
	}
	
	private void loadSequenceVirtualAVI(String csFile) {
		try
		{
//			aviFile = new VideoImporter();
//			aviFile.open(csFile, 0);
			aviFile = new XugglerAviFile(csFile, true);
			status = Status.AVIFILE;
//            OMEXMLMetadata meta = aviFile.getOMEXMLMetaData();
//            nTotalFrames = MetaDataUtil.getSizeT(meta, 0);
			nTotalFrames = (int) aviFile.getTotalNumberOfFrame();
			csFileName = csFile;
		}
		catch (Exception exc)
		{
			MessageDialog.showDialog( "File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE );
			status = Status.FAILURE;
		}
	}
	
	private void loadSequenceVirtual(String[] list, String directory) {
		status = Status.FAILURE;
		list = keepOnlyAcceptedNames(list);
		if (list==null) 
			return;

		listFiles = new String [list.length];
		int j = 0;
		for (int i=0; i<list.length; i++) {
			if (list[i]!=null)
				listFiles [j++] = directory + '\\'+ list[i];
		}
		listFiles = StringSorter.sortNumerically(listFiles);
		nTotalFrames = listFiles.length;
		status = Status.FILESTACK;		
	}

	private void setVImageName(int t)
	{
		if (status != Status.REGULAR)
			setName(getVImageName(t));
	}

	private boolean xmlReadCapillaryTrackParameters (Document doc) {

		String nodeName = "capillaryTrack";
		// read local parameters
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;

		Element xmlElement = XMLUtil.getElement(node, "Parameters");
		if (xmlElement == null) 
			return false;

		Element xmlVal = XMLUtil.getElement(xmlElement, "file");
		sourceFile = XMLUtil.getAttributeValue(xmlVal, XML_KEY_ID, null);
		
		xmlVal = XMLUtil.getElement(xmlElement, "Grouping");
		capillariesGrouping = XMLUtil.getAttributeIntValue(xmlVal, "n", 2);
		
		xmlVal = XMLUtil.getElement(xmlElement, "capillaryVolume");
		capillaryVolume = XMLUtil.getAttributeDoubleValue(xmlVal, "volume_ul", Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, "capillaryPixels");
		capillaryPixels = XMLUtil.getAttributeDoubleValue(xmlVal, "npixels", Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, "analysis");
		analysisStart =  XMLUtil.getAttributeLongValue(xmlVal, "start", 0);
		analysisEnd = XMLUtil.getAttributeLongValue(xmlVal, "end", -1);
		threshold =  XMLUtil.getAttributeIntValue(xmlVal, "threshold", -1);

		return true;
	}
	
	private boolean xmlWriteCapillaryTrackParameters (Document doc) {

		// save local parameters
		String nodeName = "capillaryTrack";
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;
		
		Element xmlElement = XMLUtil.addElement(node, "Parameters");
		
		Element xmlVal = XMLUtil.addElement(xmlElement, "file");
		if (status == Status.FILESTACK) 
			XMLUtil.setAttributeValue(xmlVal, XML_KEY_ID, listFiles[0]);
		else //  if ((status == Status.AVIFILE))
			XMLUtil.setAttributeValue(xmlVal, XML_KEY_ID, csFileName);
		
		xmlVal = XMLUtil.addElement(xmlElement, "Grouping");
		XMLUtil.setAttributeIntValue(xmlVal, "n", capillariesGrouping);
		
		xmlVal = XMLUtil.addElement(xmlElement, "capillaryVolume");
		XMLUtil.setAttributeDoubleValue(xmlVal, "volume_ul", capillaryVolume);

		xmlVal = XMLUtil.addElement(xmlElement, "capillaryPixels");
		XMLUtil.setAttributeDoubleValue(xmlVal, "npixels", capillaryPixels);

		xmlVal = XMLUtil.addElement(xmlElement, "analysis");
		XMLUtil.setAttributeLongValue(xmlVal, "start", analysisStart);
		XMLUtil.setAttributeLongValue(xmlVal, "end", analysisEnd);
		XMLUtil.setAttributeIntValue(xmlVal, "threshold", threshold); 

		return true;
	}
	
	public XYSeries[] getResults () 
	{
		return results;
	}
	
	public XYSeries[] getPixels () 
	{
		return pixels;
	}

}