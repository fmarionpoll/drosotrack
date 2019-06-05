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
		
		threshold =  XMLUtil.getElementIntValue(xmlVal, "threshold", -1);
		btrackWhite = XMLUtil.getElementBooleanValue(xmlVal, "btrackWhite", false);
		ichanselected = XMLUtil.getElementIntValue(xmlVal, "ichanselected", 0);
		blimitLow = XMLUtil.getElementBooleanValue(xmlVal, "blimitLow",false);
		blimitUp = XMLUtil.getElementBooleanValue(xmlVal, "blimitUp", false);
		limitLow =  XMLUtil.getElementIntValue(xmlVal, "limitLow", -1);
		limitUp =  XMLUtil.getElementIntValue(xmlVal, "limitUp", -1);
		jitter =  XMLUtil.getElementIntValue(xmlVal, "jitter", 10); 
		String op = XMLUtil.getElementValue(xmlVal, "transformOp", null);
		transformop = TransformOp.findByText(op);

		return true;
	}
	
	@Override
	public boolean saveToXML(Node node) {

		if (node == null)
			return false;

		Element xmlVal = XMLUtil.addElement(node, "DetectFliesParameters");
		
		XMLUtil.setElementIntValue(xmlVal, "threshold", threshold);
		XMLUtil.setElementBooleanValue(xmlVal, "btrackWhite", btrackWhite);
		XMLUtil.setElementIntValue(xmlVal, "ichanselected", ichanselected);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitLow", blimitLow);
		XMLUtil.setElementBooleanValue(xmlVal, "blimitUp", blimitUp);
		XMLUtil.setElementIntValue(xmlVal, "limitLow", limitLow);
		XMLUtil.setElementIntValue(xmlVal, "limitUp", limitUp);
		XMLUtil.setElementIntValue(xmlVal, "jitter", jitter); 
		if (transformop != null) {
			String transform = transformop.toString();
			XMLUtil.setElementValue(xmlVal, "transformOp", transform);
		}
		return true;
	}
	
}
