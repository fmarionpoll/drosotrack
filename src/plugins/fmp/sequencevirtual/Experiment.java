package plugins.fmp.sequencevirtual;


import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

public class Experiment {
	public String						filename = null;
	public SequenceVirtual 				vSequence = null;
	public ArrayList <SequencePlus> 	kymographArrayList	= new ArrayList <SequencePlus> ();
	public FileTime						fileTimeImageFirst;
	public FileTime						fileTimeImageLast;
	
	public boolean openSequenceAndMeasures() {

		vSequence = new SequenceVirtual();
		if (null == vSequence.loadVirtualStackAt(filename))
			return false;
		fileTimeImageFirst = vSequence.getImageModifiedTime(0);
		fileTimeImageLast = vSequence.getImageModifiedTime(vSequence.getSizeT()-1);
		
		if (!vSequence.xmlReadCapillaryTrackDefault()) 
			return false;
		String directory = vSequence.getDirectory() +"\\results";
		kymographArrayList = SequencePlusUtils.openFiles(directory);
		vSequence.xmlReadDrosoTrackDefault();
		return true;
	}
}
