package plugins.fmp.toolsSequence;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

public class Experiment {
	
	public String						filename 					= null;
	public SequenceVirtual 				vSequence 					= null;
	public ArrayList <SequencePlus> 	kymographArrayList			= null;
	public FileTime						fileTimeImageFirst;
	public FileTime						fileTimeImageLast;
	public long							fileTimeImageFirstMinutes 	= 0;
	public long							fileTimeImageLastMinutes 	= 0;
	public int							number_of_frames 			= 0;
	
	public int 							startFrame 					= 0;
	public int 							step 						= 1;
	public int 							endFrame 					= 0;
	
	public	String						boxIDFromFilePath;		// TODO suppress here and move to multicafe
	
	
	public boolean openSequenceAndMeasures() {

		vSequence = new SequenceVirtual();
		if (null == vSequence.loadVirtualStackAt(filename))
			return false;
		fileTimeImageFirst = vSequence.getImageModifiedTime(0);
		fileTimeImageLast = vSequence.getImageModifiedTime(vSequence.getSizeT()-1);
		//System.out.println("read expt: "+ filename+" .....size "+ vSequence.getSizeT());
		
		fileTimeImageFirstMinutes = fileTimeImageFirst.toMillis()/60000;
		fileTimeImageLastMinutes = fileTimeImageLast.toMillis()/60000;
		
		if (!vSequence.xmlReadCapillaryTrackDefault()) 
			return false;
		boxIDFromFilePath = getBoxIdentificatorFromFilePath();
		
		String directory = vSequence.getDirectory() +"\\results";
		kymographArrayList = SequencePlusUtils.openFiles(directory, vSequence.capillaries);
		vSequence.xmlReadDrosoTrackDefault();
		return true;
	}
	
	protected String getBoxIdentificatorFromFilePath () {
		Path path = Paths.get(vSequence.getFileName());
		String name = getSubName(path, 2); 
		return name;
	}
	
	protected String getSubName(Path path, int subnameIndex) {
		String name = "-";
		if (path.getNameCount() >= subnameIndex)
			name = path.getName(path.getNameCount() -subnameIndex).toString();
		return name;
	}
}
