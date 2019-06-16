package plugins.fmp.sequencevirtual;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.edit.ROIAddsSequenceEdit;
import icy.util.XMLUtil;
import plugins.fmp.tools.Tools;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillaries {
	
	public double 	capillaryVolume = 1.;
	public double 	capillaryPixels = 1.;
	public int		capillariesGrouping = 1;
	public String 	sourceName = null;
	public ArrayList <ROI2DShape> capillariesArrayList 	= new ArrayList <ROI2DShape>();	
	public long analysisStart = 0;
	public long analysisEnd = 0;
	public int analysisStep = 1;
	
	public boolean xmlReadCapillaryParameters (Document doc) {
		String nodeName = "capillaryTrack";
		// read local parameters
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;

		Element xmlElement = XMLUtil.getElement(node, "Parameters");
		if (xmlElement == null) 
			return false;

		Element xmlVal = XMLUtil.getElement(xmlElement, "file");
		sourceName = XMLUtil.getAttributeValue(xmlVal, "ID", null);
		
		xmlVal = XMLUtil.getElement(xmlElement, "Grouping");
		capillariesGrouping = XMLUtil.getAttributeIntValue(xmlVal, "n", 2);
		
		xmlVal = XMLUtil.getElement(xmlElement, "capillaryVolume");
		capillaryVolume = XMLUtil.getAttributeDoubleValue(xmlVal, "volume_ul", Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, "capillaryPixels");
		capillaryPixels = XMLUtil.getAttributeDoubleValue(xmlVal, "npixels", Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, "analysis");
		analysisStart =  XMLUtil.getAttributeLongValue(xmlVal, "start", 0);
		analysisEnd = XMLUtil.getAttributeLongValue(xmlVal, "end", 0);
		analysisStep = XMLUtil.getAttributeIntValue(xmlVal, "step", 1);

		return true;
	}
	
	private boolean xmlWriteCapillaryParameters (Document doc, SequenceVirtual seq) {
		String nodeName = "capillaryTrack";
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;
		
		Element xmlElement = XMLUtil.addElement(node, "Parameters");
		
		Element xmlVal = XMLUtil.addElement(xmlElement, "file");
		
		sourceName = seq.getFileName();
		XMLUtil.setAttributeValue(xmlVal, "ID", sourceName);
	
		xmlVal = XMLUtil.addElement(xmlElement, "Grouping");
		XMLUtil.setAttributeIntValue(xmlVal, "n", capillariesGrouping);
		
		xmlVal = XMLUtil.addElement(xmlElement, "capillaryVolume");
		XMLUtil.setAttributeDoubleValue(xmlVal, "volume_ul", capillaryVolume);

		xmlVal = XMLUtil.addElement(xmlElement, "capillaryPixels");
		XMLUtil.setAttributeDoubleValue(xmlVal, "npixels", capillaryPixels);

		xmlVal = XMLUtil.addElement(xmlElement, "analysis");
		XMLUtil.setAttributeLongValue(xmlVal, "start", seq.analysisStart);
		XMLUtil.setAttributeLongValue(xmlVal, "end", seq.analysisEnd); 
		XMLUtil.setAttributeIntValue(xmlVal, "step", seq.analysisStep); 

		return true;
	}
	
	public void extractLinesFromSequence(SequenceVirtual seq) {

		capillariesArrayList.clear();
		ArrayList<ROI2D> list = seq.getROI2Ds();
		 
		for (ROI2D roi:list)
		{
			if ((roi instanceof ROI2DShape) == false)
				continue;
			if (!roi.getName().contains("line"))
				continue;
			if (roi instanceof ROI2DLine || roi instanceof ROI2DPolyLine)
				capillariesArrayList.add((ROI2DShape)roi);
		}
		Collections.sort(capillariesArrayList, new Tools.ROI2DNameComparator()); 
	}
	
	public boolean xmlWriteROIsAndData(String name, SequenceVirtual seq) {

		String csFile = Tools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlWriteROIsAndDataNoQuestion(csFile, seq);
	}
	
	public boolean xmlWriteROIsAndDataNoQuestion(String csFile, SequenceVirtual seq) {

		if (csFile != null) 
		{
			extractLinesFromSequence(seq);
			if (capillariesArrayList.size() > 0)
			{
				final Document doc = XMLUtil.createDocument(true);
				if (doc != null)
				{
					List<ROI> roisList = new ArrayList<ROI>();
					for (ROI roi: capillariesArrayList)
						roisList.add(roi);
					ROI.saveROIsToXML(XMLUtil.getRootElement(doc), roisList);
					xmlWriteCapillaryParameters (doc, seq);
					XMLUtil.saveDocument(doc, csFile);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean xmlReadROIsAndData(SequenceVirtual seq) {

		String [] filedummy = null;
		String filename = seq.getFileName();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = Tools.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i= 0; i< filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadROIsAndData(csFile, seq);
			}
		}
		return wasOk;
	}
	
	public boolean xmlReadROIsAndData(String csFileName, SequenceVirtual seq) {
		
		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				final List<ROI> rois = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
				xmlReadCapillaryParameters(doc);
				
				Collections.sort(rois, new Tools.ROINameComparator()); 

				try  {  
					for (ROI roi : rois)  {
						seq.addROI(roi);
					}
				}
				finally {
				}
				// add to undo manager
				seq.addUndoableEdit(new ROIAddsSequenceEdit(seq, rois) {
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
}
