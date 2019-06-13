package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.multicafe.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlusUtils;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults {

	static SequenceVirtual 				vSequence = null;
	static XLSExportCapillariesOptions 	options = null;
	static ArrayList<SequencePlus> 		kymographArrayList = null;
	
	public static void exportToFile(String filename, XLSExportCapillariesOptions opt) {
		
		System.out.println("XLS output");
		options = opt;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int row0 = 0;
			int rowmax = 0;
			int iSeries = 0;
			for (Experiment exp: options.experimentList) 
			{
				openSequenceAndMeasures(exp.filename);
				
				String charSeries = CellReference.convertNumToColString(iSeries);
				if (options.topLevel) {
					rowmax = xlsExportToWorkbook(workbook, "toplevel", XLSExportItems.TOPLEVEL, row0, charSeries);
					if (options.onlyalive) 
						xlsExportToWorkbook(workbook, "toplevel_alive", XLSExportItems.TOPLEVEL, row0, charSeries);
				}
				if (options.bottomLevel) 
					xlsExportToWorkbook(workbook, "bottomlevel", XLSExportItems.BOTTOMLEVEL, row0, charSeries);
				if (options.derivative) 
					xlsExportToWorkbook(workbook, "derivative", XLSExportItems.DERIVEDVALUES, row0, charSeries);
				if (options.consumption) {
					xlsExportToWorkbook(workbook, "sumGulps", XLSExportItems.SUMGULPS, row0, charSeries);
					if (options.onlyalive) 
						xlsExportToWorkbook(workbook, "sumGulps_alive", XLSExportItems.SUMGULPS, row0, charSeries);
				}
				if (options.sum) { 
					xlsExportToWorkbook(workbook, "sumL+R", XLSExportItems.SUMLR, row0, charSeries);
					if (options.onlyalive) 
						xlsExportToWorkbook(workbook, "sumL+R_alive", XLSExportItems.SUMLR, row0, charSeries);
				}
				row0 = rowmax;
				iSeries++;
			}
			if (options.topLevel && options.transpose && options.pivot) 
				xlsCreatePivotTable(workbook, "toplevel", rowmax, iSeries);
				
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}
	
	private static boolean openSequenceAndMeasures(String filename) {
		vSequence = new SequenceVirtual();
		if (null == vSequence.loadVirtualStackAt(filename))
			return false;
		if (!vSequence.xmlReadCapillaryTrackDefault()) 
			return false;
		String directory = vSequence.getDirectory() +"\\results";
		kymographArrayList = SequencePlusUtils.openFiles(directory);
		vSequence.xmlReadDrosoTrackDefault();
		return true;
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
				if (array == null || array.size() < 1)
					continue;
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
	
	private static ArrayList <ArrayList<Integer>> trimDeadsFromArrayList(ArrayList <ArrayList<Integer >> resultsArrayList) {
		ArrayList <ArrayList<Integer >> trimmedArrayList = new ArrayList <ArrayList<Integer >> ();
		int icapillary = 0;
		for (XYTaSeries flypos: vSequence.cages.flyPositionsList) {
			int ilastalive = getLastIntervalAlive(flypos);					
			trimmedArrayList.add(trimArrayLength(resultsArrayList.get(icapillary), ilastalive));
			trimmedArrayList.add(trimArrayLength(resultsArrayList.get(icapillary+1), ilastalive));
			icapillary += 2;
		}
		return trimmedArrayList;
		
	}
	
	private static int getLastIntervalAlive(XYTaSeries flypos) {
		if (flypos.lastIntervalAlive >= 0)
			return flypos.lastIntervalAlive;
		
		return flypos.computeLastIntervalAlive();
	}
	
	private static ArrayList<Integer> trimArrayLength (ArrayList<Integer> array, int ilastalive) {
		if (array == null)
			return null;
		int nresults_intervals = array.size();
		for (int i = nresults_intervals-1; i > ilastalive; i--) {
			array.remove(i);
		}
		return array;
	}
	
	private static int xlsExportToWorkbook(XSSFWorkbook workBook, String title, XLSExportItems xlsoption, int row0, String charSeries) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(xlsoption, options.t0);		
		if (arrayList.size() == 0)
			return 0;

		if (title.contains("alive")
				&& options.onlyalive 
				&& vSequence.cages.flyPositionsList.size() > 0) {
			arrayList = trimDeadsFromArrayList(arrayList);
		}
		
		Sheet sheet = workBook.getSheet(title );
		if (sheet == null)
			sheet = workBook.createSheet(title);
		Point pt = writeGlobalInfos(sheet, row0, options.transpose, charSeries);
		pt = writeColumnHeaders(sheet, xlsoption, pt, options.transpose, charSeries);
		pt = writeData(sheet, xlsoption, pt, options.transpose, charSeries, arrayList);
		return pt.y;
	}
	
	private static Point writeGlobalInfos(Sheet sheet, int row0, boolean transpose, String charSeries) {
		Point pt = new Point(0, row0);

		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		XLSUtils.setValue(sheet, pt, transpose, path);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, charSeries+"units");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "µl" );
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "pixels" );
		pt.y++;
		
		pt.x = 1;
		XLSUtils.setValue(sheet, pt, transpose, charSeries+"capillary" );
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.capillaries.capillaryVolume);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.capillaries.capillaryPixels);
		pt.x = 0;
		pt.y++;
		return pt;
	}

	private static Point writeColumnHeaders (Sheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {
		pt.x = 0;
		if (vSequence.isFileStack()) {
			XLSUtils.setValue(sheet, pt, transpose, "filename" );
			pt.x++;
		}
		XLSUtils.setValue(sheet, pt, transpose, charSeries+"i" );
		pt.x++;
		
		switch (option) {
		case SUMLR:
			for (int i=0; i< kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0+"+"+name1 );
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, "." );
				pt.x++;
			}
			break;
		default:
			for (int i=0; i< kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = kymographArrayList.get(i);
				String name = kymographSeq.getName();
				XLSUtils.setValue(sheet, pt, transpose, name );
				pt.x++;
			}
			break;
		}
		pt.x = 0;
		pt.y++;
		return pt;
	}

	private static Point writeData (Sheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
		double ratio = vSequence.capillaries.capillaryVolume / vSequence.capillaries.capillaryPixels;
		if (charSeries == null)
			charSeries = "t";
		
		int startFrame = (int) vSequence.analysisStart;
		int step = vSequence.analysisStep;
		int endFrame = (int) vSequence.analysisEnd;
		Point pt0 = new Point (pt);
		if (pt.y -4 > 0)
			pt0.y -= 4;
		int j = 0;		

		for (int t=startFrame; t < endFrame; t+= step, j++) {
			pt.x = 0;
			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(t);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet, pt, transpose, fileName );
				pt.x++;
			}

			XLSUtils.setValue(sheet, pt, transpose, charSeries+t);
			pt.x++;
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< kymographArrayList.size(); i+=2) 
				{
					pt0.x = pt.x;
					ArrayList<Integer> dataL = dataArrayList.get(i) ;
					ArrayList<Integer> dataR = dataArrayList.get(i+1);
					if (j < dataL.size() && j < dataR.size()) {
						double value = (dataL.get(j)+dataR.get(j))*ratio;
						double valueold = 0.;
						Cell cell = XLSUtils.getCell(sheet, pt0, transpose);
						if (cell.getCellType() == CellType.NUMERIC)
							valueold = cell.getNumericCellValue();
						value += valueold;
						XLSUtils.setValue(sheet, pt, transpose, value );
					}
					pt.x++;
					pt.x++;
				}
				break;

			default:
				for (int i=0; i< kymographArrayList.size(); i++) 
				{
					pt0.x = pt.x;
					ArrayList<Integer> data = dataArrayList.get(i);
					if (j < data.size()) {
						double value = data.get(j)*ratio;
						double valueold = 0.;
						Cell cell = XLSUtils.getCell(sheet, pt0, transpose);
						if (cell.getCellType() == CellType.NUMERIC)
							valueold = cell.getNumericCellValue();
						value += valueold;
						XLSUtils.setValue(sheet, pt, transpose, value);
					}
					pt.x++;
				}
				break;
			}
			pt.y++;
		}
		return pt;
	}
	
	public static void xlsCreatePivotTable(XSSFWorkbook workBook, String sourcetitle, int rowmax, int nseries) {
        XSSFSheet pivotSheet = workBook.createSheet("pivot");
        XSSFSheet sourceSheet = workBook.getSheet("toplevel");

        int ncolumns = rowmax;
        int ncolumns_notdata = 3;

        CellAddress lastcell = new CellAddress (21, rowmax-1);
        String address = "A2:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(1, 1);
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(source, position, sourceSheet);
        
        pivotTable.addRowLabel(2);
        for (int i=ncolumns_notdata; i< ncolumns; i++) {
        	Cell cell = XLSUtils.getCell(sourceSheet, 1, i);
        	String text = cell.getStringCellValue();
        	if (text.contains("units")) {
        		i+= 2;
        		continue;
        	}
        	pivotTable.addColumnLabel(DataConsolidateFunction.AVERAGE, i, text);
        }

	}
}
