package plugins.fmp.multicafe;

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
import plugins.fmp.sequencevirtual.SequencePlus;


public class CapillariesTab_File extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4019075448319252245L;
	
	private JButton		openButtonCapillaries	= new JButton("Load...");
	private JButton		saveButtonCapillaries	= new JButton("Save...");
	private JButton		openButtonKymos1	= new JButton("Load...");
	private JButton		saveButtonKymos1	= new JButton("Save...");
	private Multicafe parent0 = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		
		JLabel loadsaveText1 = new JLabel ("-> Capillaries (xml) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonCapillaries, saveButtonCapillaries));
		
		JLabel loadsaveText1b = new JLabel ("-> Kymographs (tiff) ", SwingConstants.RIGHT);
		loadsaveText1b.setFont(FontUtil.setStyle(loadsaveText1b.getFont(), Font.ITALIC));	
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1b, openButtonKymos1, saveButtonKymos1));
		
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openButtonCapillaries.addActionListener(this); 
		saveButtonCapillaries.addActionListener(this);	
		openButtonKymos1.addActionListener(this);
		saveButtonKymos1.addActionListener(this);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openButtonCapillaries)  {
			firePropertyChange("CAPILLARIES_NEW", false, true);
		}
		else if ( o == saveButtonCapillaries) {
			firePropertyChange("CAP_ROIS_SAVE", false, true);	
		}
		else if ( o == openButtonKymos1)  {
			String path = parent0.vSequence.getDirectory()+ "\\results";
			boolean flag = openFiles(path); 
			if (flag) {
				firePropertyChange("KYMOS_OPEN", false, true);	
			}
		}
		else if ( o == saveButtonKymos1) {
			String path = parent0.vSequence.getDirectory() + "\\results";
			saveFiles(path);
			firePropertyChange("KYMOS_SAVE", false, true);	
		}
	}

	public boolean capillaryRoisOpen(String csFileName) {
		
		boolean flag = false;
		if (csFileName == null)
			flag = parent0.vSequence.capillaries.xmlReadROIsAndData(parent0.vSequence);
		else
			flag = parent0.vSequence.capillaries.xmlReadROIsAndData(csFileName, parent0.vSequence);
		return flag;
	}
	
	public boolean capillaryRoisSave() {
		return parent0.vSequence.capillaries.xmlWriteROIsAndData("capillarytrack.xml", parent0.vSequence);
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
			filename = directory + "\\" + filename;
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
			kymographSeq.addImage(ibufImage);
			
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf("\\")+1;
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
			directory = parent0.vSequence.getDirectory()+ "\\results";
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
				String filename = outputpath + "\\" + seq.getName() + ".tiff";
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
	
	public boolean loadDefaultKymos() {
		String path = parent0.vSequence.getDirectory();
		final String cs = path+"\\results";
		boolean flag = openFiles(cs);
		if (flag) {
			parent0.capillariesPane.optionsTab.transferFileNamesToComboBox();
			parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		return flag;
	}
}
