package plugins.fmp.sequencevirtual;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.tools.Tools;

import plugins.kernel.roi.roi2d.ROI2DShape;
import plugins.kernel.roi.roi2d.ROI2DPolygon;


public class Cages {
	
	public DetectFliesParameters 		detect 					= new DetectFliesParameters();
	public ArrayList<ROI2D> 			cageLimitROIList		= new ArrayList<ROI2D>();
	public ArrayList<Integer>			lastTime_it_MovedList 	= new ArrayList<Integer>();
	public ArrayList<PositionsXYT> 		cagePositionsList 		= new ArrayList<PositionsXYT>();
	

	public void clear() {
		lastTime_it_MovedList.clear();
		cagePositionsList.clear();
	}
	
	public boolean xmlWriteCagesToFile(String name, String directory) {

		String csFile = Tools.saveFileAs(name, directory, "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlWriteCagesToFileNoQuestion(csFile);
	}
	
	public boolean xmlWriteCagesToFileNoQuestion(String csFile) {
		if (csFile == null) 
			return false;
		final Document doc = XMLUtil.createDocument(true);
		if (doc == null)
			return false;
		
		xmlWriteCages (doc);
		XMLUtil.saveDocument(doc, csFile);
		return true;
	}
	
	public boolean xmlReadCagesFromFile(SequenceVirtual seq) {

		String [] filedummy = null;
		String filename = seq.getFileName();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = Tools.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i= 0; i< filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadCagesFromFileNoQuestion(csFile, seq);
			}
		}
		return wasOk;

	}
	
	public boolean xmlReadCagesFromFileNoQuestion(String csFileName, SequenceVirtual seq) {

		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				xmlLoadCages(doc);
				replaceROIsInSequence(seq);
				return true;
			}
		}
		return false;
	}
	
	private boolean xmlLoadCages (Document doc) {
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
		int i=0;
		for (ROI roi: cageLimitROIList) {
			String name = "roi"+i;
			Element subnode = XMLUtil.addElement(xmlVal, name);
			roi.saveToXML(subnode);
			i++;
		}
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
			ROI2DPolygon roi = (ROI2DPolygon) ROI.create("plugins.kernel.roi.roi2d.ROI2DPolygon");
			String name = "roi"+i;
			Element subnode = XMLUtil.getElement(xmlVal, name);
			roi.loadFromXML(subnode);
			cageLimitROIList.add((ROI2D) roi);
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
		XMLUtil.setAttributeIntValue(xmlVal, "nb_items", cagePositionsList.size());
		for (PositionsXYT pos: cagePositionsList)
			pos.saveToXML(xmlVal);
		return true;
	}
	
	private boolean xmlLoadCagePositionsList(Node node) {
		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "PositionsInCages");
		if (xmlVal == null) 
			return false;
		
		cagePositionsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(xmlVal, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			PositionsXYT pos = new PositionsXYT();
			pos.loadFromXML(node);
			cagePositionsList.add(pos);
		}
		return true;
	}
	
	private void replaceROIsInSequence(SequenceVirtual seq) {
		ArrayList<ROI2D> list = seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("cage"))
				continue;
			seq.removeROI(roi);
		}
		seq.addROIs(cageLimitROIList, true);
	}
	
	public void getCagesFromSequence(SequenceVirtual seq) {
		cageLimitROIList.clear();
		ArrayList<ROI2D> list = seq.getROI2Ds();
		for (ROI2D roi: list) {
			if (!(roi instanceof ROI2DShape))
				continue;
			if (!roi.getName().contains("cage"))
				continue;
			cageLimitROIList.add(roi);
		}
		Collections.sort(cageLimitROIList, new Tools.ROI2DNameComparator());
	}


}
