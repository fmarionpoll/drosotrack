package plugins.fmp.sequencevirtual;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;


public class SequencePlusUtils {
	public static boolean isInterrupted = false;
	public static boolean isRunning = false;
	
	public static ArrayList<SequencePlus> openFiles (String directory) {
		
		isRunning = true;
		ArrayList<SequencePlus> arrayKymos = new ArrayList<SequencePlus> ();	
		String[] list = (new File(directory)).list();
		if (list == null)
			return arrayKymos;
		
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(list.length);
		
		for (String filename: list) {
			if (!filename.contains(".tiff"))
				continue;
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			SequencePlus kymographSeq = new SequencePlus();
			final String name =  directory + "\\" + filename;
			progress.setMessage( "Load "+filename);
			

			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(name);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.addImage(ibufImage);
			
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf("\\")+1;
			String title = filename.substring(index0, index1);
			kymographSeq.setName(title);
			kymographSeq.loadXMLKymographAnalysis(directory);
			arrayKymos.add(kymographSeq);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return arrayKymos;
	}
}
