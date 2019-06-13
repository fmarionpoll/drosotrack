package plugins.fmp.sequencevirtual;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.image.IcyBufferedImage;

public class SequencePlusUtils {
	public static ArrayList<SequencePlus> openFiles (String directory) {
		
		ArrayList<SequencePlus> arrayKymos = new ArrayList<SequencePlus> ();	
		String[] list = (new File(directory)).list();
		if (list == null)
			return arrayKymos;
		
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		for (String filename: list) {
			if (!filename.contains(".tiff"))
				continue;

			SequencePlus kymographSeq = new SequencePlus();
			filename = directory + "\\" + filename;

			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(filename);

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
			kymographSeq.loadXMLCapillaryTrackResults(directory);
			arrayKymos.add(kymographSeq);
		}
		return arrayKymos;
	}
}
