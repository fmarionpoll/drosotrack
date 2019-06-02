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
import plugins.fmp.multicafe.Multicafe.StatusAnalysis;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;

public class KymosTab_File  extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3973928400949966679L;
	private JButton		openButtonKymos1	= new JButton("Load...");
	private JButton		saveButtonKymos1	= new JButton("Save...");
	private JButton		openMeasuresButton		= new JButton("Load");
	private JButton		saveMeasuresButton		= new JButton("Save");
	private Multicafe parent0 = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		this.parent0 = parent0;
		JLabel loadsaveText1 = new JLabel ("-> File (tiff) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));	
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonKymos1, saveButtonKymos1));
		
		JLabel loadsaveText3 = new JLabel ("-> File (xml) ", SwingConstants.RIGHT); 
		loadsaveText3.setFont(FontUtil.setStyle(loadsaveText3.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel(new JLabel (" "), loadsaveText3,  openMeasuresButton, saveMeasuresButton));

		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openButtonKymos1.addActionListener(this);
		saveButtonKymos1.addActionListener(this);
		openMeasuresButton.addActionListener(this); 
		saveMeasuresButton.addActionListener(this);	
	}
	
	public void enableItems(boolean enabled) {
		openButtonKymos1.setEnabled(enabled);
		saveButtonKymos1.setEnabled(enabled);
		openMeasuresButton.setEnabled(enabled);
		saveMeasuresButton.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == openButtonKymos1)  {
			enableItems(false);
			String path = parent0.vSequence.getDirectory()+ "\\results";
			boolean flag = openFiles(path); 
			enableItems(true);
			if (flag) {
				parent0.buttonsVisibilityUpdate(StatusAnalysis.KYMOS_OK);
				firePropertyChange("KYMOS_OPEN", false, true);	
			}
		}
		else if ( o == saveButtonKymos1) {
			enableItems(false);
			String path = parent0.vSequence.getDirectory() + "\\results";
			saveFiles(path);
			enableItems(true);
			firePropertyChange("KYMOS_SAVE", false, true);	
		}
		else if ( o == openMeasuresButton)  {
			if (measuresFileOpen()) {
				firePropertyChange("MEASURES_OPEN", false, true);
			}
		}
		else if ( o == saveMeasuresButton) {
			measuresFileSave();
			firePropertyChange("MEASURES_SAVE", false, true);	
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

	// ASSUME: same parameters for each kymograph
	public boolean measuresFileOpen() {
		String directory = parent0.vSequence.getDirectory();
		boolean flag = true;
		for (int kymo=0; kymo < parent0.kymographArrayList.size(); kymo++) {	
			SequencePlus seq = parent0.kymographArrayList.get(kymo);
			seq.beginUpdate();
			if (flag = seq.loadXMLCapillaryTrackResults(directory)) {
				seq.validateRois();
				seq.getArrayListFromRois(ArrayListType.cumSum);
			}
			else 
				System.out.println("load measures -> failed or not found in directory: " + directory);
			seq.endUpdate();
		}
		
		SequencePlus seq = parent0.kymographArrayList.get(0);
		parent0.vSequence.analysisStart = seq.analysisStart; 
		parent0.vSequence.analysisEnd = seq.analysisEnd;
		parent0.vSequence.analysisStep = seq.analysisStep;
		return flag;
	}
	
	public void measuresFileSave() {
		
		String directory = parent0.vSequence.getDirectory();
		for (int kymo=0; kymo < parent0.kymographArrayList.size(); kymo++) {
			SequencePlus seq = parent0.kymographArrayList.get(kymo);
			seq.analysisStart = parent0.vSequence.analysisStart; 
			seq.analysisEnd = parent0.vSequence.analysisEnd;
			seq.analysisStep = parent0.vSequence.analysisStep;
			
			System.out.println("saving "+seq.getName());
			if (!seq.saveXMLCapillaryTrackResults(directory))
				System.out.println(" -> failed - in directory: " + directory);
		}
	}
}
