package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults {

	static SequenceVirtual 				vSequence = null;
	static XLSExportCapillariesOptions 	options = null;
	static ArrayList<SequencePlus> 		kymographArrayList = null;
	
	public static void exportToFile(String filename, XLSExportCapillariesOptions opt) {
		
		System.out.println("XLS output");
		options = opt;
		vSequence = vSeq;
		kymographArrayList = kymographsArray;
		
		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col0 = 0;
			for (int i=0; i < options.experimentList.size(); i++) {
				openDocuments(options.experimentList.get(i).filename);
				if (options.topLevel) {
					xlsExportToWorkbook(workbook, "toplevel", XLSExportItems.TOPLEVEL, col0);
					if (options.onlyalive) xlsExportToWorkbook(workbook, "toplevel_alive", XLSExportItems.TOPLEVEL, col0);
				}
				if (options.bottomLevel) 
					xlsExportToWorkbook(workbook, "bottomlevel", XLSExportItems.BOTTOMLEVEL, col0);
				if (options.derivative) 
					xlsExportToWorkbook(workbook, "derivative", XLSExportItems.DERIVEDVALUES, col0);
				if (options.consumption) {
					xlsExportToWorkbook(workbook, "sumGulps", XLSExportItems.SUMGULPS, col0);
					if (options.onlyalive) xlsExportToWorkbook(workbook, "sumGulps_alive", XLSExportItems.SUMGULPS, col0);
				}
				if (options.sum) { 
					xlsExportToWorkbook(workbook, "sumL+R", XLSExportItems.SUMLR, col0);
					if (options.onlyalive) xlsExportToWorkbook(workbook, "sumL+R_alive", XLSExportItems.SUMLR, col0);
				}
			}
			if (options.topLevel && options.transpose && options.pivot) xlsCreatePivotTable(workbook, "toplevel");
				
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}
	
	private static boolean openDocuments(String filename) {
		vSequence = new SequenceVirtual();
		if (null == vSequence.loadVirtualStackAt(filename))
			return false;
		String path = vSequence.getDirectory();
		String csCapillaries = path+"\\capillarytrack.xml";
		boolean flag = vSequence.capillaries.xmlReadROIsAndData(csCapillaries, vSequence);
		if (!flag) return flag;
		vSequence.capillaries.extractLinesFromSequence(vSequence);	// ???

		final String csKymographs = path+"\\results";
		
		
		return false;
	}

	private static ArrayList <ArrayList<Integer>> getDataFromRois(XLSExportItems xlsoption, boolean relative) {
		ArrayList <ArrayList<Integer >> resultsArrayList = new ArrayList <ArrayList<Integer >> ();
		for (SequencePlus seq: kymographArrayList) {
			switch (xlsoption) {
			case DERIVEDVALUES:
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.derivedValues));
				break;
			case SUMGULPS: 
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.cumSum));
				break;
			case BOTTOMLEVEL:
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.bottomLevel));
				break;
			case TOPLEVEL:
			case SUMLR:
			default:
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			}
		}
		
		if (relative && resultsArrayList.size() > 0) {
			for (ArrayList<Integer> array : resultsArrayList) {

				int item0 = array.get(0);
				int i=0;
				for (int item: array) {
					array.set(i, item-item0);
					i++;
				}
			}
		}
		return resultsArrayList;
	}
	
	private static ArrayList <ArrayList<Integer>> trimDeads(ArrayList <ArrayList<Integer >> resultsArrayList) {
		ArrayList <ArrayList<Integer >> trimmedArrayList = new ArrayList <ArrayList<Integer >> ();
		int ncages = vSequence.cages.flyPositionsList.size();
		int ncapillaries = resultsArrayList.size();
		int icapillary = 0;
		for (XYTaSeries flypos: vSequence.cages.flyPositionsList) {
			int ilastalive = getLastIntervalAlive(flypos);					
			trimmedArrayList.add(trimArray(resultsArrayList.get(icapillary), ilastalive));
			trimmedArrayList.add(trimArray(resultsArrayList.get(icapillary+1), ilastalive));
			icapillary += 2;
		}
		return trimmedArrayList;
		
	}
	
	private static int getLastIntervalAlive(XYTaSeries flypos) {
		int npos_intervals = flypos.pointsList.size();
		int ilastalive = -1;
		for (int i = npos_intervals-1; i >= 0; i--) {
			if (flypos.pointsList.get(i).alive) {
				ilastalive = i;
				break;
			}
		}
		return ilastalive;
	}
	
	private static ArrayList<Integer> trimArray (ArrayList<Integer> array, int ilastalive) {
		int nresults_intervals = array.size();
		for (int i = nresults_intervals-1; i > ilastalive; i--) {
			array.remove(i);
		}
		return array;
	}
	
	private static void xlsExportToWorkbook(XSSFWorkbook workBook, String title, XLSExportItems xlsoption, int col0) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(xlsoption, options.t0);		
		if (arrayList.size() == 0)
			return;

		if (title.contains("alive")
				&& options.onlyalive 
				&& vSequence.cages.flyPositionsList.size() > 0) {
			arrayList = trimDeads(arrayList);
		}
		
		Sheet sheet = workBook.getSheet(title );
		if (sheet == null)
				sheet = workBook.createSheet(title );
		Point pt = writeGlobalInfos(sheet, options.transpose, col0);
		pt = writeColumnHeaders(sheet, pt, xlsoption, options.transpose, col0);
		pt = writeData(sheet, pt, xlsoption, arrayList, options.transpose, col0);
	}
	
	private static Point writeGlobalInfos(Sheet sheet, boolean transpose, int col0) {
		Point pt = new Point(col0, 0);

		XLSUtils.setValue(sheet,  pt.x, pt.y, "name:" );
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		pt = XLSUtils.nextCol(pt, transpose);
		XLSUtils.setValue(sheet,  pt.x, pt.y, path );
		pt= XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.toColZero(pt, transpose, col0);
		Point pt1 = pt;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (µl):" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.capillaries.capillaryVolume);
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (pixels):" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.capillaries.capillaryPixels);
		pt = XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private static Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose, int col0) {
		pt = XLSUtils.toColZero(pt, transpose, col0);
		if (vSequence.isFileStack()) {
			XLSUtils.setValue(sheet,  pt.x, pt.y, "filename" );
			pt = XLSUtils.nextCol(pt, transpose);
		}
		XLSUtils.setValue(sheet,  pt.x, pt.y, "i" );
		pt = XLSUtils.nextCol(pt, transpose);
		
		switch (option) {
		case SUMLR:
			for (int i=0; i< kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+"+"+name1 );
				pt = XLSUtils.nextCol(pt, transpose);
				XLSUtils.setValue(sheet,  pt.x, pt.y, "." );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		default:
			for (int i=0; i< kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = kymographArrayList.get(i);
				String name = kymographSeq.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		}
		pt = XLSUtils.toColZero(pt, transpose, col0);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private static Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose, int col0) {
		int maxelements = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > maxelements)
				maxelements = datai.size();
		}
		int nelements = maxelements; //-1;
		if (nelements <= 0)
			return pt;
		
		double ratio = vSequence.capillaries.capillaryVolume / vSequence.capillaries.capillaryPixels;
		
		int startFrame = (int) vSequence.analysisStart;
		int t = startFrame;
		// TODO check if name of files is correct
		for (int j=0; j< nelements; j++) {
			Point pt2 = XLSUtils.toColZero(pt, transpose, col0);
			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet,  pt2.x, pt2.y, fileName );
				pt2 = XLSUtils.nextCol(pt2, transpose);
			}

			XLSUtils.setValue(sheet,  pt2.x, pt2.y, Integer.toString(t));
			t  += vSequence.analysisStep;
			pt2 = XLSUtils.nextCol(pt2, transpose);
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< kymographArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = arrayList.get(i);
					ArrayList<Integer> dataR = arrayList.get(i+1);
					if (j < dataL.size() && j < dataR.size())
						XLSUtils.setValue(sheet,  pt2.x, pt2.y, (dataL.get(j)+dataR.get(j))*ratio );
					pt2 = XLSUtils.nextCol(pt2, transpose);
					pt2 = XLSUtils.nextCol(pt2, transpose);
				}
				break;

			default:
				for (int i=0; i< kymographArrayList.size(); i++) 
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
	
	public static void xlsCreatePivotTable(XSSFWorkbook workBook, String sourcetitle) {
        XSSFSheet sheet = workBook.createSheet("pivot");
        XSSFSheet sourceSheet = workBook.getSheet("toplevel");
        
        int ndatacolumns = 0;
        for (XYTaSeries series: vSequence.cages.flyPositionsList) {
        	int len = series.pointsList.size();
        	if (len > ndatacolumns)
        		ndatacolumns = len;
        }
        int ncolumns_notdata = 4;
        CellAddress lastcell = new CellAddress (21, ndatacolumns + ncolumns_notdata -1);
        String address = "D2:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(1, 1);
        XSSFPivotTable pivotTable = sheet.createPivotTable(source, position, sourceSheet);
        
        pivotTable.addRowLabel(0);
        for (int i=0; i< ndatacolumns; i++) {
        	Cell cell = XLSUtils.getCell(sourceSheet, 1, i+ncolumns_notdata);
        	pivotTable.addColumnLabel(DataConsolidateFunction.AVERAGE, i+1, cell.getStringCellValue());
        }

	}
}
