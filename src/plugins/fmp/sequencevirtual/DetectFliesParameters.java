package plugins.fmp.sequencevirtual;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;
import plugins.fmp.tools.ImageTransformTools.TransformOp;

public class DetectFliesParameters implements XMLPersistent {
	
	public int threshold = -1;
	public boolean 	btrackWhite = false;
	public int		ichanselected = 0;
	public boolean  blimitLow = false;
	public boolean  blimitUp = false;
	public int  	limitLow;
	public int  	limitUp;
	public int 		jitter = 10;
	public TransformOp transformop; 
	
	@Override
	public boolean loadFromXML(Node node) {

		if (node == null)
			return false;
		Element xmlVal = XMLUtil.getElement(node, "DetectFliesParameters");
		if (xmlVal == null) 
			return false;
		
		threshold =  XMLUtil.getAttributeIntValue(xmlVal, "threshold", -1);
		btrackWhite = XMLUtil.getAttributeBooleanValue(xmlVal, "btrackWhite", false);
		ichanselected = XMLUtil.getAttributeIntValue(xmlVal, "ichanselected", 0);
		blimitLow = XMLUtil.getAttributeBooleanValue(xmlVal, "blimitLow",false);
		blimitUp = XMLUtil.getAttributeBooleanValue(xmlVal, "blimitUp", false);
		limitLow =  XMLUtil.getAttributeIntValue(xmlVal, "limitLow", -1);
		limitUp =  XMLUtil.getAttributeIntValue(xmlVal, "limitUp", -1);
		jitter =  XMLUtil.getAttributeIntValue(xmlVal, "jitter", 10); 
		String op = XMLUtil.getAttributeValue(xmlVal, "transformOp", null);
		transformop = TransformOp.findByText(op);

		return true;
	}
	
	@Override
	public boolean saveToXML(Node node) {

		if (node == null)
			return false;

		Element xmlVal = XMLUtil.addElement(node, "DetectFliesParameters");
		
		XMLUtil.setAttributeIntValue(xmlVal, "threshold", threshold);
		XMLUtil.setAttributeBooleanValue(xmlVal, "btrackWhite", btrackWhite);
		XMLUtil.setAttributeIntValue(xmlVal, "ichanselected", ichanselected);
		XMLUtil.setAttributeBooleanValue(xmlVal, "blimitLow", blimitLow);
		XMLUtil.setAttributeBooleanValue(xmlVal, "blimitUp", blimitUp);
		XMLUtil.setAttributeIntValue(xmlVal, "limitLow", limitLow);
		XMLUtil.setAttributeIntValue(xmlVal, "limitUp", limitUp);
		XMLUtil.setAttributeIntValue(xmlVal, "jitter", jitter); 
		String transform = transformop.toString();
		XMLUtil.setAttributeValue(xmlVal, "transformOp", transform);

		return true;
	}
	
}
