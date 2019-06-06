package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.tools.Tools;
import plugins.fmp.tools.XLSExportItems;
import plugins.fmp.tools.XLSUtils;

public class ResultsTab_Excel extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox 	topLevelCheckBox 	= new JCheckBox("top", true);
	public JCheckBox 	bottomLevelCheckBox = new JCheckBox("bottom", false);
	public JCheckBox 	consumptionCheckBox = new JCheckBox("gulps", true);
	public JCheckBox 	sumCheckBox 		= new JCheckBox("L+R", true);
	public JCheckBox 	derivativeCheckBox  = new JCheckBox("derivative", false);
	public JCheckBox	t0CheckBox			= new JCheckBox("t-t0", true);
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", false);

	private Multicafe parent0 = null;
	
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( topLevelCheckBox, bottomLevelCheckBox, consumptionCheckBox, sumCheckBox));
		add(GuiUtil.besidesPanel( t0CheckBox, transposeCheckBox, new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	public void enableItems(boolean enabled) {
		exportToXLSButton.setEnabled(enabled);
		topLevelCheckBox.setEnabled(enabled);
		bottomLevelCheckBox.setEnabled(enabled);
		consumptionCheckBox.setEnabled(enabled);
		sumCheckBox.setEnabled(enabled);
		t0CheckBox.setEnabled(enabled);
		transposeCheckBox.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xlsx";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
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
		
		try { 
			Workbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

			if (topLevelCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "toplevel", XLSExportItems.TOPLEVEL);
			if (bottomLevelCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "bottomlevel", XLSExportItems.BOTTOMLEVEL);
			if (derivativeCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "derivative", XLSExportItems.DERIVEDVALUES);
			if (consumptionCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "sumGulps", XLSExportItems.SUMGULPS);
			if (sumCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "sumL+R", XLSExportItems.SUMLR);
			
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private ArrayList <ArrayList<Integer >> getDataFromRois(XLSExportItems option, boolean relative) {
		ArrayList <ArrayList<Integer >> arrayList = new ArrayList <ArrayList <Integer>> ();
		for (SequencePlus seq: parent0.kymographArrayList) {
			switch (option) {
			case DERIVEDVALUES:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.derivedValues));
				break;
			case SUMGULPS: 
				seq.getArrayListFromRois(ArrayListType.cumSum);
				arrayList.add(seq.getArrayListFromRois(ArrayListType.cumSum));
				break;
			case BOTTOMLEVEL:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.bottomLevel));
				break;
			case TOPLEVEL:
			case SUMLR:
			default:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			}
		}
		
		if (relative) {
			for (ArrayList<Integer> array : arrayList) {
				int item0 = array.get(0);
				int i=0;
				for (int item: array) {
					array.set(i, item-item0);
					i++;
				}
			}
		}
		return arrayList;
	}
	
	private void xlsExportToWorkbook(Workbook workBook, String title, XLSExportItems option) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(option, t0CheckBox.isSelected());		
		if (arrayList.size() == 0)
			return;

		Sheet sheet = workBook.createSheet(title );
		boolean transpose = transposeCheckBox.isSelected(); 
		Point pt = writeGlobalInfos(sheet, transpose);
		pt = writeColumnHeaders(sheet, pt, option, transpose);
		pt = writeData(sheet, pt, option, arrayList, transpose);
	}
	
	private Point writeGlobalInfos(Sheet sheet, boolean transpose) {
		Point pt = new Point(0, 0);

		XLSUtils.setValue(sheet,  pt.x, pt.y, "name:" );
		File file = new File(parent0.vSequence.getFileName(0));
		String path = file.getParent();
		pt = XLSUtils.nextCol(pt, transpose);
		XLSUtils.setValue(sheet,  pt.x, pt.y, path );
		pt= XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.toColZero(pt, transpose);
		Point pt1 = pt;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (µl):" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, 	parent0.vSequence.capillaries.capillaryVolume);
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (pixels):" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, 	parent0.vSequence.capillaries.capillaryPixels);
		pt = XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose) {
		pt = XLSUtils.toColZero(pt, transpose);
		if (parent0.vSequence.isFileStack()) {
			XLSUtils.setValue(sheet,  pt.x, pt.y, "filename" );
			pt = XLSUtils.nextCol(pt, transpose);
		}
		XLSUtils.setValue(sheet,  pt.x, pt.y, "i" );
		pt = XLSUtils.nextCol(pt, transpose);
		
		switch (option) {
		case SUMLR:
			for (int i=0; i< parent0.kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = parent0.kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = parent0.kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+"+"+name1 );
				pt = XLSUtils.nextCol(pt, transpose);
				XLSUtils.setValue(sheet,  pt.x, pt.y, "." );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		default:
			for (int i=0; i< parent0.kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = parent0.kymographArrayList.get(i);
				String name = kymographSeq.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		}
		pt = XLSUtils.toColZero(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose) {
		int maxelements = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > maxelements)
				maxelements = datai.size();
		}
		int nelements = maxelements-1;
		if (nelements <= 0)
			return pt;
		
		double ratio = parent0.vSequence.capillaries.capillaryVolume / parent0.vSequence.capillaries.capillaryPixels;
		
		int startFrame = (int) parent0.vSequence.analysisStart;
		int t = startFrame;
		for (int j=0; j< nelements; j++) {
			Point pt2 = XLSUtils.toColZero(pt, transpose);
			if (parent0.vSequence.isFileStack()) {
				String cs = parent0.vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet,  pt2.x, pt2.y, fileName );
				pt2 = XLSUtils.nextCol(pt2, transpose);
			}

			XLSUtils.setValue(sheet,  pt2.x, pt2.y, t);
			t  += parent0.vSequence.analysisStep;
			pt2 = XLSUtils.nextCol(pt2, transpose);
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< parent0.kymographArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = arrayList.get(i);
					ArrayList<Integer> dataR = arrayList.get(i+1);
					if (j < dataL.size())
						XLSUtils.setValue(sheet,  pt2.x, pt2.y, (dataL.get(j)+dataR.get(j))*ratio );
					pt2 = XLSUtils.nextCol(pt2, transpose);
					pt2 = XLSUtils.nextCol(pt2, transpose);
				}
				break;

			default:
				for (int i=0; i< parent0.kymographArrayList.size(); i++) 
				{
					ArrayList<Integer> data = arrayList.get(i);
					if (j < data.size())
						XLSUtils.setValue(sheet, pt2.x, pt2.y, data.get(j)*ratio );
					pt2 = XLSUtils.nextCol (pt2, transpose);
				}
				break;
			}
			
			pt = XLSUtils.nextRow (pt, transpose);
		}
		return pt;
	}
	
}
