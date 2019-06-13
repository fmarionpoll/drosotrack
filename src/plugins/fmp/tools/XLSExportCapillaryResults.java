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
			for (Experiment exp: options.experimentList) {
				openDocuments(exp.filename);
				if (options.topLevel) {
					rowmax = xlsExportToWorkbook(workbook, "toplevel", XLSExportItems.TOPLEVEL, row0);
					if (options.onlyalive) 
						xlsExportToWorkbook(workbook, "toplevel_alive", XLSExportItems.TOPLEVEL, row0);
				}
				if (options.bottomLevel) 
					xlsExportToWorkbook(workbook, "bottomlevel", XLSExportItems.BOTTOMLEVEL, row0);
				if (options.derivative) 
					xlsExportToWorkbook(workbook, "derivative", XLSExportItems.DERIVEDVALUES, row0);
				if (options.consumption) {
					xlsExportToWorkbook(workbook, "sumGulps", XLSExportItems.SUMGULPS, row0);
					if (options.onlyalive) xlsExportToWorkbook(workbook, "sumGulps_alive", XLSExportItems.SUMGULPS, row0);
				}
				if (options.sum) { 
					xlsExportToWorkbook(workbook, "sumL+R", XLSExportItems.SUMLR, row0);
					if (options.onlyalive) xlsExportToWorkbook(workbook, "sumL+R_alive", XLSExportItems.SUMLR, row0);
				}
				row0 = rowmax+1;
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
		boolean flag = vSequence.capillaries.xmlReadROIsAndData(path+"\\capillarytrack.xml", vSequence);
		if (!flag) return flag;
		vSequence.capillaries.extractLinesFromSequence(vSequence);	// ???
		String directory = path+"\\results";
		kymographArrayList = SequencePlusUtils.openFiles(directory);
		vSequence.cages.xmlReadCagesFromFileNoQuestion(path + "\\drosotrack.xml", vSequence);
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
//		int ncages = vSequence.cages.flyPositionsList.size();
//		int ncapillaries = resultsArrayList.size();
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
	
	private static ArrayList<Integer> trimArrayLength (ArrayList<Integer> array, int ilastalive) {
		if (array == null)
			return null;
		int nresults_intervals = array.size();
		for (int i = nresults_intervals-1; i > ilastalive; i--) {
			array.remove(i);
		}
		return array;
	}
	
	private static int xlsExportToWorkbook(XSSFWorkbook workBook, String title, XLSExportItems xlsoption, int row0) {
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
		Point pt = writeGlobalInfos(sheet, options.transpose, row0);
		pt = writeColumnHeaders(sheet, pt, xlsoption, options.transpose);
		pt = writeData(sheet, pt, xlsoption, arrayList, options.transpose);
		return pt.y;
	}
	
	private static Point writeGlobalInfos(Sheet sheet, boolean transpose, int row0) {
		Point pt = new Point(0, row0);

		XLSUtils.setValue(sheet,  pt.x, pt.y, "name:" , transpose);
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		pt.x++;
		XLSUtils.setValue(sheet,  pt.x, pt.y, path, transpose);
		pt.y++;
		pt.x = 0;
		Point pt1 = pt;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (µl):" , transpose);
		pt1.x++;;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.capillaries.capillaryVolume, transpose);
		pt1.x++;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (pixels):" , transpose);
		pt1.x++;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.capillaries.capillaryPixels, transpose);
		pt.y++;
		pt.y++;
		return pt;
	}

	private static Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose) {
		pt.x = 0;
		if (vSequence.isFileStack()) {
			XLSUtils.setValue(sheet,  pt.x, pt.y, "filename" , transpose);
			pt.x++;
		}
		XLSUtils.setValue(sheet,  pt.x, pt.y, "i" , transpose);
		pt.x++;
		
		switch (option) {
		case SUMLR:
			for (int i=0; i< kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+"+"+name1 , transpose);
				pt.x++;
				XLSUtils.setValue(sheet,  pt.x, pt.y, "." , transpose);
				pt.x++;
			}
			break;
		default:
			for (int i=0; i< kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = kymographArrayList.get(i);
				String name = kymographSeq.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name , transpose);
				pt.x++;
			}
			break;
		}
		pt.x = 0;
		pt.y++;
		return pt;
	}

	private static Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose) {
		int maxelements = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai == null)
				continue;
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
			Point pt2 = new Point(0, pt.y); 
			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet,  pt2.x, pt2.y, fileName , transpose);
				pt2.x++;
			}

			XLSUtils.setValue(sheet,  pt2.x, pt2.y, Integer.toString(t), transpose);
			t  += vSequence.analysisStep;
			pt2.x++;
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< kymographArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = arrayList.get(i);
					ArrayList<Integer> dataR = arrayList.get(i+1);
					if (j < dataL.size() && j < dataR.size())
						XLSUtils.setValue(sheet,  pt2.x, pt2.y, (dataL.get(j)+dataR.get(j))*ratio , transpose);
					pt2.x++;
					pt2.x++;
				}
				break;

			default:
				for (int i=0; i< kymographArrayList.size(); i++) 
				{
					ArrayList<Integer> data = arrayList.get(i);
					if (j < data.size())
						XLSUtils.setValue(sheet, pt2.x, pt2.y, data.get(j)*ratio , transpose);
					pt2.x++;
				}
				break;
			}
			pt.y++;
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
