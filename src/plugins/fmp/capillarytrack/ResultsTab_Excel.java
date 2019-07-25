package plugins.fmp.capillarytrack;

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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.multicafeSequence.SequencePlus;
import plugins.fmp.multicafeTools.EnumArrayListType;
import plugins.fmp.multicafeTools.EnumXLSExportItems;
import plugins.fmp.multicafeTools.MulticafeTools;

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

	private Capillarytrack parent0 = null;
	
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {	
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
			String file = MulticafeTools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
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
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

			if (topLevelCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "toplevel", EnumXLSExportItems.TOPLEVEL);
			if (bottomLevelCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "bottomlevel", EnumXLSExportItems.BOTTOMLEVEL);
			if (derivativeCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "derivative", EnumXLSExportItems.DERIVEDVALUES);
			if (consumptionCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "sumGulps", EnumXLSExportItems.SUMGULPS);
			if (sumCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "sumL+R", EnumXLSExportItems.TOPLEVEL_LR);
			
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private ArrayList <ArrayList<Integer >> getDataFromRois(EnumXLSExportItems option, boolean relative) {
		ArrayList <ArrayList<Integer >> arrayList = new ArrayList <ArrayList <Integer>> ();
		for (SequencePlus seq: parent0.kymographArrayList) {
			switch (option) {
			case DERIVEDVALUES:
				arrayList.add(seq.getArrayListFromRois(EnumArrayListType.derivedValues));
				break;
			case SUMGULPS: 
				seq.getArrayListFromRois(EnumArrayListType.cumSum);
				arrayList.add(seq.getArrayListFromRois(EnumArrayListType.cumSum));
				break;
			case BOTTOMLEVEL:
				arrayList.add(seq.getArrayListFromRois(EnumArrayListType.bottomLevel));
				break;
			case TOPLEVEL:
			case TOPLEVEL_LR:
			default:
				arrayList.add(seq.getArrayListFromRois(EnumArrayListType.topLevel));
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
	
	private void xlsExportToWorkbook(XSSFWorkbook workBook, String title, EnumXLSExportItems option) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(option, t0CheckBox.isSelected());		
		if (arrayList.size() == 0)
			return;

		XSSFSheet sheet = workBook.createSheet(title );
		boolean transpose = transposeCheckBox.isSelected(); 
		Point pt = writeGlobalInfos(sheet, transpose);
		pt = writeColumnHeaders(sheet, pt, option, transpose);
		pt = writeData(sheet, pt, option, arrayList, transpose);

	}
	
	private Point writeGlobalInfos(XSSFSheet sheet, boolean transpose) {
		Point pt = new Point(0, 0);

		setValue(sheet,  pt.x, pt.y, "name:" );
		File file = new File(parent0.vSequence.getFileName(0));
		String path = file.getParent();
		pt = nextCol(pt, transpose);
		setValue(sheet,  pt.x, pt.y, path );
		pt= nextRow(pt, transpose);
		pt = toColZero(pt, transpose);
		Point pt1 = pt;
		setValue(sheet,  pt1.x, pt1.y, "capillary (µl):" );
		pt1 = nextCol(pt1, transpose);
		setValue(sheet,  pt1.x, pt1.y, 	parent0.vSequence.capillaries.volume);
		pt1 = nextCol(pt1, transpose);
		setValue(sheet,  pt1.x, pt1.y, "capillary (pixels):" );
		pt1 = nextCol(pt1, transpose);
		setValue(sheet,  pt1.x, pt1.y, 	parent0.vSequence.capillaries.pixels);
		pt = nextRow(pt, transpose);
		pt = nextRow(pt, transpose);
		return pt;
	}

	private Point writeColumnHeaders (XSSFSheet sheet, Point pt, EnumXLSExportItems option, boolean transpose) {
		pt = toColZero(pt, transpose);
		if (parent0.vSequence.isFileStack()) {
			setValue(sheet,  pt.x, pt.y, "filename" );
			pt = nextCol(pt, transpose);
		}
		setValue(sheet,  pt.x, pt.y, "i" );
		pt = nextCol(pt, transpose);
		
		switch (option) {
		case TOPLEVEL_LR:
			for (int i=0; i< parent0.kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = parent0.kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = parent0.kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				setValue(sheet,  pt.x, pt.y, name0+"+"+name1 );
				pt = nextCol(pt, transpose);
				setValue(sheet,  pt.x, pt.y, "." );
				pt = nextCol(pt, transpose);
			}
			break;
		default:
			for (int i=0; i< parent0.kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = parent0.kymographArrayList.get(i);
				String name = kymographSeq.getName();
				setValue(sheet,  pt.x, pt.y, name );
				pt = nextCol(pt, transpose);
			}
			break;
		}
		pt = toColZero(pt, transpose);
		pt = nextRow(pt, transpose);
		return pt;
	}

	private Point writeData (XSSFSheet sheet, Point pt, EnumXLSExportItems option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose) {
		int maxelements = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > maxelements)
				maxelements = datai.size();
		}
		int nelements = maxelements-1;
		if (nelements <= 0)
			return pt;
		
		double ratio = parent0.vSequence.capillaries.volume / parent0.vSequence.capillaries.pixels;
		
		int startFrame = (int) parent0.vSequence.analysisStart;
		int t = startFrame;
		for (int j=0; j< nelements; j++) {
			Point pt2 = toColZero(pt, transpose);
			if (parent0.vSequence.isFileStack()) {
				String cs = parent0.vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				setValue(sheet,  pt2.x, pt2.y, fileName );
				pt2 = nextCol(pt2, transpose);
			}

			setValue(sheet,  pt2.x, pt2.y, t);
			t  += parent0.vSequence.analysisStep;
			pt2 = nextCol(pt2, transpose);
			
			switch (option) {
			case TOPLEVEL_LR:
				for (int i=0; i< parent0.kymographArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = arrayList.get(i);
					ArrayList<Integer> dataR = arrayList.get(i+1);
					if (j < dataL.size())
						setValue(sheet,  pt2.x, pt2.y, (dataL.get(j)+dataR.get(j))*ratio );
					pt2 = nextCol(pt2, transpose);
					pt2 = nextCol(pt2, transpose);
				}
				break;

			default:
				for (int i=0; i< parent0.kymographArrayList.size(); i++) 
				{
					ArrayList<Integer> data = arrayList.get(i);
					if (j < data.size())
						setValue(sheet, pt2.x, pt2.y, data.get(j)*ratio );
					pt2 = nextCol (pt2, transpose);
				}
				break;
			}
			
			pt = nextRow (pt, transpose);
		}
		return pt;
	}
	
	private Point nextRow (Point pt, boolean transpose) {
		if (!transpose)
			pt.y ++;
		else
			pt.x++;
		return pt;
	}
	private Point nextCol (Point pt, boolean transpose) {
		if (!transpose) 
			pt.x ++;
		else 
			pt.y++;
		return pt;
	}	
	private Point toColZero (Point pt, boolean transpose) {
		if (!transpose) 
			pt.x = 0;
		else
			pt.y = 0;
		return pt;
	}

	private void setValue (XSSFSheet sheet, int column, int row, int ivalue) {
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(ivalue);
	}
	private void setValue (XSSFSheet sheet, int column, int row, String string) {
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(string);
	}
	
	private void setValue (XSSFSheet sheet, int column, int row, double value) {
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(value);
	}
	
	private Cell getCell (XSSFSheet sheet, int rownum, int colnum) {
		Row row = getRow(sheet, rownum);
		Cell cell = getCol (row, colnum);
		return cell;
	}
	private Row getRow (XSSFSheet sheet, int rownum) {
		Row row = sheet.getRow(rownum);
		if (row == null)
			row = sheet.createRow(rownum);
		return row;
	}
	private Cell getCol (Row row, int cellnum) {
		Cell cell = row.getCell(cellnum);
		if (cell == null)
			cell = row.createCell(cellnum);
		return cell;
	}
	
}
