package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;
import plugins.fmp.tools.Tools;

public class ResultsTab_Excel extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;
	public JButton 		exportToXLSButton 	= new JButton("Export to XLS file...");
	public JCheckBox 	topLevelCheckbox 	= new JCheckBox("top level", true);
	public JCheckBox 	bottomLevelCheckbox = new JCheckBox("bottom level", true);
	public JCheckBox 	derivativeCheckbox 	= new JCheckBox("derivative", true);
	public JCheckBox 	consumptionCheckbox = new JCheckBox("consumption", true);
	public JCheckBox 	sumCheckbox = new JCheckBox("sum L+R", true);

	private Capillarytrack parent0 = null;
	
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( topLevelCheckbox, bottomLevelCheckbox, derivativeCheckbox, consumptionCheckbox));
		add(GuiUtil.besidesPanel( sumCheckbox, new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	public void enableItems(boolean enabled) {
		exportToXLSButton.setEnabled(enabled);
		topLevelCheckbox.setEnabled(enabled);
		bottomLevelCheckbox.setEnabled(enabled);
		derivativeCheckbox.setEnabled(enabled);
		consumptionCheckbox.setEnabled(enabled);
		sumCheckbox.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xls";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xls");
			if (file != null) {
				final String filename = file;
				exportToXLSButton.setEnabled( false);
				parent0.capillariesPane.propertiesTab.updateSequenceFromDialog();
				xlsExportResultsToFile(filename);
				firePropertyChange("EXPORT_TO_EXCEL", false, true);	
				exportToXLSButton.setEnabled( true );
			}
		}
	}

	private void xlsExportResultsToFile(String filename) {
		System.out.println("XLS output");
		
		double ratio = parent0.vSequence.capillaryVolume / parent0.vSequence.capillaryPixels;

		try {
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename); 
			if (topLevelCheckbox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "toplevel", 0, ratio);
			if (bottomLevelCheckbox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "bottomlevel", 3, ratio);
			if (derivativeCheckbox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "derivative", 1, ratio);
			if (consumptionCheckbox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "consumption", 2, ratio);
			if (sumCheckbox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "sumL+R", 4, ratio);
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private void xlsExportToWorkbook(WritableWorkbook xlsWorkBook, String title, int ioption, double ratio ) {
		System.out.println("export worksheet "+title);
		int ncols = parent0.kymographArrayList.size();
		ArrayList <ArrayList<Integer >> arrayList = new ArrayList <ArrayList <Integer>> ();
		for (SequencePlus seq: parent0.kymographArrayList) {
			switch (ioption) {
			case 1:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.derivedValues));
				break;
			case 2: 
				seq.getArrayListFromRois(ArrayListType.cumSum);
				arrayList.add(seq.getArrayListFromRois(ArrayListType.cumSum));
				break;
			case 3:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.bottomLevel));
				break;
			case 4: // TODO
			case 0:
			default:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			}
		}
		
		if (arrayList.size() == 0)
			return;

		int nrowmax = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > nrowmax)
				nrowmax = datai.size();
		}
		int nrows = nrowmax-1;
		// exit if no data in the first sequence
		if (nrows <= 0)
			return;

		WritableSheet excelSheet = XLSUtil.createNewPage( xlsWorkBook , title );

		// output last interval at which movement was detected over the whole period analyzed
		int irow = 0;
		XLSUtil.setCellString( excelSheet , 0, irow, "name:" );
		
		File file = new File(parent0.vSequence.getFileName(0));
		String path = file.getParent();
		XLSUtil.setCellString( excelSheet , 1, irow, path );
		irow++;
		int icol00 = 0;
		XLSUtil.setCellString( excelSheet, icol00++, irow, "capillary" );
		XLSUtil.setCellString( excelSheet, icol00++, irow, "volume (µl):" );
		XLSUtil.setCellNumber( excelSheet, icol00++, irow, 	parent0.vSequence.capillaryVolume);
		XLSUtil.setCellString( excelSheet, icol00++, irow, "pixels:" );
		XLSUtil.setCellNumber( excelSheet, icol00++, irow, 	parent0.vSequence.capillaryPixels);
		irow++;
		irow++;

		// output column headers
		int icol0 = 0;

		if (parent0.vSequence.isFileStack()) {
			XLSUtil.setCellString( excelSheet , icol0, irow, "filename" );
			icol0++;
		}

		XLSUtil.setCellString( excelSheet , icol0, irow, "i" );
		icol0++;

		// export data
		for (int i=0; i< ncols; i++) {
			SequencePlus kymographSeq = parent0.kymographArrayList.get(i);
			String name = kymographSeq.getName();
			XLSUtil.setCellString( excelSheet , icol0 + i, irow, name );
		}
		irow++;

		// output data
		int startFrame = (int) parent0.vSequence.analysisStart;
		int t = startFrame;
		for (int j=0; j<nrows; j++) {
			icol0 = 0;
			if (parent0.vSequence.isFileStack()) {
				String cs = parent0.vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtil.setCellString( excelSheet , icol0, irow, fileName );
				icol0++;
			}

			XLSUtil.setCellNumber( excelSheet , icol0, irow, t);
			t  += parent0.vSequence.analyzeStep;
			
			icol0++;
			for (int i=0; i< ncols; i++, icol0++) {
				ArrayList<Integer> data = arrayList.get(i);
				if (j < data.size())
					XLSUtil.setCellNumber( excelSheet , icol0, irow, data.get(j)*ratio );
			}
			irow++;
		}
	}


}
