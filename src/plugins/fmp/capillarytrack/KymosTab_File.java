package plugins.fmp.capillarytrack;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import loci.formats.FormatException;
import plugins.fmp.drosoSequence.SequencePlus;
import plugins.fmp.drosoTools.EnumStatusAnalysis;

public class KymosTab_File  extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3973928400949966679L;
	private JButton		openButtonKymos	= new JButton("Load...");
	private JButton		saveButtonKymos	= new JButton("Save...");
	private Capillarytrack parent0 = null;
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		JLabel loadsaveText1 = new JLabel ("-> File (tiff) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));	
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonKymos, saveButtonKymos));
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openButtonKymos.addActionListener(this);
		saveButtonKymos.addActionListener(this);
	}
	
	public void enableItems(boolean enabled) {
		openButtonKymos.setEnabled(enabled);
		saveButtonKymos.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openButtonKymos)  {
			enableItems(false);
			String path = parent0.vSequence.getDirectory()+ "\\results";
			boolean flag = openFiles(path); 
			enableItems(true);
			if (flag) {
				parent0.buttonsVisibilityUpdate(EnumStatusAnalysis.KYMOS_OK);
				firePropertyChange("KYMOS_OPEN", false, true);	
			}
		}
		else if ( o == saveButtonKymos) {
			enableItems(false);
			String path = parent0.vSequence.getDirectory() + "\\results";
			saveFiles(path);
			enableItems(true);
			firePropertyChange("KYMOS_SAVE", false, true);	
		}		
	}
	
	public boolean openFiles(String directory) {
		
		if (directory == null) {
			directory = parent0.vSequence.getDirectory();
		
			JFileChooser f = new JFileChooser(directory);
			f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
			int v = f.showOpenDialog(null);
			if (v == JFileChooser.APPROVE_OPTION  )
				directory =  f.getSelectedFile().getAbsolutePath();
			else
				return false;
		}
		
		String[] list = (new File(directory)).list();
		if (list == null)
			return false;
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);

		// send some info
		ProgressFrame progress = new ProgressFrame("Open kymographs ...");
		int itotal = parent0.kymographArrayList.size();
		progress.setLength(itotal);

		// loop over the list to open tiff files as kymographs
		parent0.kymographArrayList.clear();

		for (String filename: list) {
			if (!filename.contains(".tiff"))
				continue;

			SequencePlus kymographSeq = new SequencePlus();
			filename = directory + File.separator + filename;
			progress.incPosition(  );
			progress.setMessage( "Open file : " + filename);

			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(filename);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.addImage(0, ibufImage);
			
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf(File.separator)+1;
			String title = filename.substring(index0, index1);
			kymographSeq.setName(title);
			parent0.kymographArrayList.add(kymographSeq);
		}

		progress.close();
		return true;
	}

	public void saveFiles(String directory) {

		// send some info
		ProgressFrame progress = new ProgressFrame("Save kymographs");
		
		if (directory == null) {
			directory = parent0.vSequence.getDirectory()+ File.separator + "results";
			}
		
		try {
			Files.createDirectories(Paths.get(directory));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		String outputpath =  directory;
		JFileChooser f = new JFileChooser(outputpath);
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
		int returnedval = f.showSaveDialog(null);
		if (returnedval == JFileChooser.APPROVE_OPTION) { 
			outputpath = f.getSelectedFile().getAbsolutePath();		
			for (SequencePlus seq: parent0.kymographArrayList) {
	
				progress.setMessage( "Save kymograph file : " + seq.getName());
				String filename = outputpath + File.separator + seq.getName() + ".tiff";
				File file = new File (filename);
				IcyBufferedImage image = seq.getFirstImage();
				try {
					Saver.saveImage(image, file, true);
				} catch (FormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				System.out.println("File "+ seq.getName() + " saved " );
			}
			System.out.println("End of Kymograph saving process");
		}
		progress.close();
	}


}
