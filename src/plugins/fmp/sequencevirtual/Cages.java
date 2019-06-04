package plugins.fmp.sequencevirtual;


import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import icy.roi.BooleanMask2D;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.tools.Tools;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;


public class Cages {
	
	public DetectFliesParameters 		detect 					= new DetectFliesParameters();
	public ArrayList<ROI2D> 			cageLimitROIList		= new ArrayList<ROI2D>();
	public ArrayList<BooleanMask2D> 	cageMaskList 			= new ArrayList<BooleanMask2D>();
	public ArrayList<Integer>			lastTime_it_MovedList 	= new ArrayList<Integer>();
	public ArrayList<PositionsXYT> 		cagePositionsList 		= new ArrayList<PositionsXYT>();
	private String sourceName;
	

	public void clear() {
		lastTime_it_MovedList.clear();
		cagePositionsList.clear();
		cageMaskList.clear();
	}
	
	private boolean xmlReadCages (Document doc) {
		String nodeName = "drosoTrack";
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;

		detect.loadFromXML(node);
		xmlLoadCagesLimits(node);
		xmlLoadLastTimeList(node);
		xmlLoadCagePositionsList(node);
		return true;
	}
	
	private boolean xmlWriteCages (Document doc) {
		String nodeName = "drosoTrack";
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;
		
		detect.saveToXML(node);
		xmlSaveCagesLimits(node);
		xmlSaveLastTimeList(node);
		xmlSaveCagePositionsList(node);
		return true;
	}
	
	private boolean xmlSaveCagesLimits(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "CagesLimits");
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", cageLimitROIList.size());
		for (PositionsXYT pos: cagePositionsList)
			pos.saveToXML(xmlVal);
		return true;
	}
	
	private boolean xmlLoadCagesLimits(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "CagesLimits");
		if (xmlVal == null) 
			return false;
		
		cageLimitROIList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			PositionsXYT pos = new PositionsXYT();
			pos.loadFromXML(node);
			cagePositionsList.add(pos);
		}
		return true;
	}
	
	private boolean xmlSaveLastTimeList(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "LastTimes");
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", lastTime_it_MovedList.size());
		for (int lastTime: lastTime_it_MovedList) {
			XMLUtil.setAttributeIntValue(xmlVal, "val", lastTime);
		}
		return true;
	}
	
	private boolean xmlLoadLastTimeList(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "LastTimes");
		if (xmlVal == null) 
			return false;
		
		lastTime_it_MovedList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			int lastTime = XMLUtil.getAttributeIntValue(xmlVal, "val", 0);
			lastTime_it_MovedList.add(lastTime);
		}
		return true;
	}
	
	
	private boolean xmlSaveCagePositionsList(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.addElement(node, "PositionsInCages");
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", lastTime_it_MovedList.size());
		for (int lastTime: lastTime_it_MovedList) {
			XMLUtil.setAttributeIntValue(xmlVal, "val", lastTime);
		}
		return true;
	}
	
	private boolean xmlLoadCagePositionsList(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "PositionsInCages");
		if (xmlVal == null) 
			return false;
		
		lastTime_it_MovedList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			int lastTime = XMLUtil.getAttributeIntValue(xmlVal, "val", 0);
			lastTime_it_MovedList.add(lastTime);
		}
		return true;
	}
	
	public void keepOnly2DLines_CapillariesArrayList(SequenceVirtual seq) {

//		capillariesArrayList.clear();
//		ArrayList<ROI2D> list = seq.getROI2Ds();
//		 
//		for (ROI2D roi:list)
//		{
//			if ((roi instanceof ROI2DShape) == false)
//				continue;
//			if (!roi.getName().contains("line"))
//				continue;
//			if (roi instanceof ROI2DLine || roi instanceof ROI2DPolyLine)
//				capillariesArrayList.add((ROI2DShape)roi);
//		}
//		Collections.sort(capillariesArrayList, new Tools.ROI2DNameComparator()); 
	}
	
	public boolean xmlWriteROIsAndData(String name, String directory) {

		String csFile = Tools.saveFileAs(name, directory, "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlWriteROIsAndDataNoQuestion(csFile);
	}
	
	public boolean xmlWriteROIsAndDataNoQuestion(String csFile) {

		if (csFile == null) 
			return false;
			
		final Document doc = XMLUtil.createDocument(true);
		if (doc == null)
			return false;
		
		xmlWriteCages (doc);
		XMLUtil.saveDocument(doc, csFile);
		return true;
	}
	
	public boolean xmlReadROIsAndData(SequenceVirtual seq) {

		// TODO
//		String [] filedummy = null;
//		String filename = seq.getFileName();
//		File file = new File(filename);
//		String directory = file.getParentFile().getAbsolutePath();
//		filedummy = Tools.selectFiles(directory, "xml");
//		boolean wasOk = false;
//		if (filedummy != null) {
//			for (int i= 0; i< filedummy.length; i++) {
//				String csFile = filedummy[i];
//				wasOk &= xmlReadROIsAndData(csFile, seq);
//			}
//		}
//		return wasOk;
		return false;
	}
	
	public boolean xmlReadROIsAndData(String csFileName, SequenceVirtual seq) {

		//TODO
//		if (csFileName != null)  {
//			final Document doc = XMLUtil.loadDocument(csFileName);
//			if (doc != null) {
//				final List<ROI> rois = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
//				xmlReadCapillaryTrackParameters(doc, seq);
//				
//				Collections.sort(rois, new Tools.ROINameComparator()); 
//
//				try  {  
//					for (ROI roi : rois)  {
//						seq.addROI(roi);
//					}
//				}
//				finally {
//				}
//				// add to undo manager
//				seq.addUndoableEdit(new ROIAddsSequenceEdit(seq, rois) {
//					@Override
//					public String getPresentationName() {
//						if (getROIs().size() > 1)
//							return "ROIs loaded from XML file";
//						return "ROI loaded from XML file"; };
//				});
//				return true;
//			}
//		}
		return false;
	}


}
