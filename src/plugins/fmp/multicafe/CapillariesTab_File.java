package plugins.fmp.multicafe;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import icy.file.Saver;
import icy.gui.frame.progress.ProgressFrame;
import icy.gui.util.FontUtil;
import icy.gui.util.GuiUtil;
import icy.image.IcyBufferedImage;
import loci.formats.FormatException;

import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlusUtils;


public class CapillariesTab_File extends JPanel implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4019075448319252245L;
	
	private JButton		openButtonCapillaries	= new JButton("Load...");
	private JButton		saveButtonCapillaries	= new JButton("Save...");
	private JButton		openButtonKymos			= new JButton("Load...");
	private JButton		saveButtonKymos			= new JButton("Save...");
	private Multicafe 	parent0 				= null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {
		setLayout(capLayout);
		
		JLabel loadsaveText1 = new JLabel ("-> Capillaries (xml) ", SwingConstants.RIGHT);
		loadsaveText1.setFont(FontUtil.setStyle(loadsaveText1.getFont(), Font.ITALIC));
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1, openButtonCapillaries, saveButtonCapillaries));
		
		JLabel loadsaveText1b = new JLabel ("-> Kymographs (tiff) ", SwingConstants.RIGHT);
		loadsaveText1b.setFont(FontUtil.setStyle(loadsaveText1b.getFont(), Font.ITALIC));	
		add(GuiUtil.besidesPanel( new JLabel (" "), loadsaveText1b, openButtonKymos, saveButtonKymos));
		
		this.parent0 = parent0;
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		openButtonCapillaries.addActionListener(this); 
		saveButtonCapillaries.addActionListener(this);	
		openButtonKymos.addActionListener(this);
		saveButtonKymos.addActionListener(this);
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
		else if ( o == openButtonKymos)  {
			String path = parent0.vSequence.getDirectory()+ "\\results";
			parent0.kymographArrayList = SequencePlusUtils.openFiles(path); 
			firePropertyChange("KYMOS_OPEN", false, true);	
		}
		else if ( o == saveButtonKymos) {
			String path = parent0.vSequence.getDirectory() + "\\results";
			saveFiles(path);
			firePropertyChange("KYMOS_SAVE", false, true);	
		}
	}

	public boolean capillaryRoisOpen(String csFileName) {
		
		boolean flag = false;
		if (csFileName == null)
			flag = parent0.vSequence.xmlReadCapillaryTrackDefault();
		else
			flag = parent0.vSequence.xmlReadCapillaryTrack(csFileName);
		return flag;
	}
	
	public boolean capillaryRoisSave() {
		parent0.sequencePane.optionsTab.UpdateItemsToSequence (parent0);
		return parent0.vSequence.xmlWriteCapillaryTrackDefault();
	}

	public ArrayList<SequencePlus> openFiles() {
		String directory = parent0.vSequence.getDirectory();
		
		JFileChooser f = new JFileChooser(directory);
		f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 
		int v = f.showOpenDialog(null);
		if (v == JFileChooser.APPROVE_OPTION  )
			directory =  f.getSelectedFile().getAbsolutePath();
		else
			return null;
		return SequencePlusUtils.openFiles(directory); 
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
		boolean flag = false;
		parent0.kymographArrayList = SequencePlusUtils.openFiles(cs);
		if (parent0.kymographArrayList != null) {
			flag = true;
			parent0.capillariesPane.optionsTab.transferFileNamesToComboBox();
			parent0.capillariesPane.optionsTab.viewKymosCheckBox.setSelected(true);
		}
		return flag;
	}
}
