package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

import org.apache.poi.ss.SpreadsheetVersion;

import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlusUtils;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults {

	static SequenceVirtual 				vSequence = null;
	static XLSExportOptions 			options = null;
	static ArrayList<SequencePlus> 		kymographArrayList = null;
	static FileTime						image0Time;
	static long							image0TimeMinutes;
	static FileTime	 firstImageTime; ;
	static long	firstImageTimeMinutes;
	
	public static void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS output");
		options = opt;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col0 = 0;
			int colmax = 0;
			int iSeries = 0;
			for (Experiment exp: options.experimentList) 
			{
				openSequenceAndMeasures(exp.filename);

				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 		colmax = getDataAndExport(workbook, col0, charSeries, XLSExportItems.TOPLEVEL);
				if (options.topLevelDelta) 	colmax = getDataAndExport(workbook, col0, charSeries, XLSExportItems.TOPLEVELDELTA);
				if (options.bottomLevel) 	colmax = getDataAndExport(workbook, col0, charSeries, XLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	colmax = getDataAndExport(workbook, col0, charSeries, XLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	colmax = getDataAndExport(workbook, col0, charSeries, XLSExportItems.SUMGULPS);
				if (options.sum) 			colmax = getDataAndExport(workbook, col0, charSeries, XLSExportItems.SUMLR);

				col0 = colmax;
				iSeries++;
			}
			
			if (options.topLevel && options.transpose && options.pivot) 
				xlsCreatePivotTables(workbook, "toplevel", colmax);
				
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}
	
	private static int getDataAndExport(XSSFWorkbook workbook, int col0, String charSeries, XLSExportItems datatype) {
		
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(datatype, options.t0);	
		int colmax = xlsExportCapillaryDataToWorkbook(workbook, datatype.toString(), datatype, col0, charSeries, arrayList);
		if (options.onlyalive) {
			arrayList = trimDeadsFromArrayList(arrayList);
			xlsExportCapillaryDataToWorkbook(workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	public static boolean openSequenceAndMeasures(String filename) {
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
	
	private static ArrayList <ArrayList<Integer>> getDataFromRois(XLSExportItems xlsoption, boolean optiont0) {
		ArrayList <ArrayList<Integer >> resultsArrayList = new ArrayList <ArrayList<Integer >> ();
		
		for (SequencePlus seq: kymographArrayList) {
			switch (xlsoption) {
			case TOPLEVELDELTA:
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				subtractTi(resultsArrayList);
				break;
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
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			default:
				break;
			}
		}
		
		if (optiont0) {
			subtractT0(resultsArrayList);
		}		
		return resultsArrayList;
	}
	
	private static void subtractT0(ArrayList <ArrayList<Integer >> resultsArrayList) {
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
	
	private static void subtractTi(ArrayList <ArrayList<Integer >> resultsArrayList) {
		for (ArrayList<Integer> array : resultsArrayList) {
			if (array == null || array.size() < 1)
				continue;
			int item0 = array.get(0);
			int i=0;
			for (int item: array) {
				array.set(i, item0-item);
				item0 = item;
				i++;
			}
		}
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
	
	private static int xlsExportCapillaryDataToWorkbook(XSSFWorkbook workBook, String title, XLSExportItems xlsoption, int col0, String charSeries, ArrayList <ArrayList<Integer >> arrayList) {
		System.out.println("export worksheet "+title);	
		
		XSSFSheet sheet = workBook.getSheet(title );
		boolean flag = (sheet == null);
		if (flag)
			sheet = workBook.createSheet(title);
		
		Point pt = writeGlobalInfos(sheet, col0, options.transpose);
		pt = writeHeader(sheet, xlsoption, pt, options.transpose, charSeries);
		pt = writeData(sheet, xlsoption, pt, options.transpose, charSeries, arrayList);
		return pt.x;
	}
	
	private static Point writeGlobalInfos(XSSFSheet sheet, int col0, boolean transpose) {
		Point pt = new Point(col0, 0);

		XLSUtils.setValue(sheet, pt, transpose, "expt");
		pt.x++;
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		XLSUtils.setValue(sheet, pt, transpose, path);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "units");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "µl" );
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "pixels" );
		pt.y++;
		
		pt.x = col0;
		XLSUtils.setValue(sheet, pt, transpose, "scale");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "capillary" );
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.capillaries.capillaryVolume);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.capillaries.capillaryPixels);
		pt.x = col0;
		pt.y++;
		
		return pt;
	}

	public static Point addLineToHeader(XSSFSheet sheet, Point pt, boolean transpose, XLSExperimentDescriptors desc) {
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, desc.toString());
		pt.x++;
		pt.x++;
		switch (desc) {
		case CAGE: 	// assume 2 capillaries/slot
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, i/2);
			break;
		case NFLIES: // assume first 2 and last 2 have no flies
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) {
				int j = 1;
				if (i < 2 || i > 17)
					j = 0;
				XLSUtils.setValue(sheet, pt, transpose, j);
			}
			break;
		case CAP:
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) {
				String name = kymographArrayList.get(i).getName();
				String letter = name.substring(name.length() - 1);
				XLSUtils.setValue(sheet, pt, transpose, letter);
			}
			break;
		case DUM4: 
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) {
				XLSUtils.setValue(sheet, pt, transpose, sheet.getSheetName());
			}
			break;
		default:
			break;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}
	
	private static Point writeHeader (XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {
		
		int col0 = pt.x;
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.DATE);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.STIM);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.CONC);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.CAM);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.CAP);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.CAGE);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.TIME);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.NFLIES);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.DUM1);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.DUM2);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.DUM3);
		pt = addLineToHeader(sheet, pt, transpose, XLSExperimentDescriptors.DUM4);
		image0Time = vSequence.getImageModifiedTime(0);
		image0TimeMinutes = image0Time.toMillis()/60000;
		if (col0 == 0) {
			firstImageTime = image0Time;
			firstImageTimeMinutes = image0TimeMinutes;
		}
	
		XLSUtils.setValue(sheet, pt, transpose, "rois"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "timeMin"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "filename" );
		pt.x++;
		for (SequencePlus seq: kymographArrayList) {
			XLSUtils.setValue(sheet, pt, transpose, seq.getName() );
			pt.x++;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}

	private static long getnearest(long diff, int step) {
		long diff0 = (diff /step)*step;
		long diff1 = diff0 + step;
		if ((diff - diff0 ) < (diff1 - diff))
			diff = diff0;
		else
			diff = diff1;
		return diff;
	}
	
	private static Point checkIfStartBefore(XSSFSheet sheet, Point pt, boolean transpose) {
		
		int startFrame 	= (int) vSequence.analysisStart;
		int step 		= vSequence.analysisStep;
		
		Point ptFirstScaleItem = new Point(pt);
		ptFirstScaleItem.x = 2;
		FileTime imageTime = vSequence.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		int row0 = pt.y;
		
//		long firstScaleItem = (long) XLSUtils.getCell(sheet, ptFirstScaleItem, transpose).getNumericCellValue();
//		long diff = getnearest(imageTimeMinutes - firstScaleItem, step);		
		long diff0 = getnearest(imageTimeMinutes - firstImageTimeMinutes, step);
		if (diff0 < 0) {
			int nshifts = (int) -diff0/step;
			if (!transpose) {
				int endRow = sheet.getLastRowNum();
				sheet.shiftRows(row0, endRow, nshifts);
			}
			else
			{
				XSSFRow row = sheet.getRow(0);				 
				int colCount = row.getLastCellNum();
				sheet.shiftColumns(row0, colCount, nshifts);
			}
			firstImageTimeMinutes = imageTimeMinutes;
			long diff = 0;
			for (int i = 0; i < nshifts; i++) {
				pt.x = 0;
				XLSUtils.setValue(sheet, pt, transpose, "t"+diff);
				pt.x = 1;
				XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
				pt.y ++;
				diff += step;
				imageTimeMinutes += step;
			}
		}
		return pt;
	}
	
	private static Point writeData (XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
		int col0 = pt.x;
		int row0 = pt.y;
		double ratio = vSequence.capillaries.capillaryVolume / vSequence.capillaries.capillaryPixels;
		if (charSeries == null)
			charSeries = "t";
		checkIfStartBefore(sheet, pt, transpose);
		
		int startFrame 	= (int) vSequence.analysisStart;
		int step 		= vSequence.analysisStep;
		int endFrame 	= (int) vSequence.analysisEnd;
		Point pt0 = new Point (pt);
		int j = 0;
		
		FileTime imageTime = vSequence.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		long diff = getnearest(imageTimeMinutes-firstImageTimeMinutes, step)/ step;
		pt.y = (int) (row0 + diff);
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+= step, j++) {
			pt.x = col0;

			imageTime = vSequence.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;
			diff = getnearest(imageTimeMinutes-image0TimeMinutes, step);
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff);
			pt.x++;
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;

			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(currentFrame);
				XLSUtils.setValue(sheet, pt, transpose, cs.substring(cs.lastIndexOf("\\") + 1) );
			}
			pt.x++;
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< dataArrayList.size(); i+=2) 
				{
					pt0.x = pt.x;
					ArrayList<Integer> dataL = dataArrayList.get(i) ;
					ArrayList<Integer> dataR = dataArrayList.get(i+1);
					if (dataL != null && dataR != null) {
						if (j < dataL.size() && j < dataR.size()) {
							double value = (dataL.get(j)+dataR.get(j))*ratio;
							double valueold = 0.;
							XSSFCell cell = XLSUtils.getCell(sheet, pt0, transpose);
							if (cell.getCellType() == CellType.NUMERIC)
								valueold = cell.getNumericCellValue();
							value += valueold;
							XLSUtils.setValue(sheet, pt, transpose, value );
						}
					}
					pt.x++;
					pt.x++;
				}
				break;

			default:
				for (int i=0; i< dataArrayList.size(); i++) 
				{
					pt0.x = pt.x;
					ArrayList<Integer> data = dataArrayList.get(i);
					if (data != null) {
						if (j < data.size()) {
							double value = data.get(j)*ratio;
							double valueold = 0.;
							XSSFCell cell = XLSUtils.getCell(sheet, pt0, transpose);
							if (cell.getCellType() == CellType.NUMERIC)
								valueold = cell.getNumericCellValue();
							value += valueold;
							XLSUtils.setValue(sheet, pt, transpose, value);
						}
					}
					pt.x++;
				}
				break;
			}
			pt.y++;
		}
		return pt;
	}
	
	public static void xlsCreatePivotTables(XSSFWorkbook workBook, String fromWorkbook, int rowmax) {
        
		xlsCreatePivotTable(workBook, "pivot_avg", fromWorkbook, rowmax, DataConsolidateFunction.AVERAGE);
		xlsCreatePivotTable(workBook, "pivot_std", fromWorkbook, rowmax, DataConsolidateFunction.STD_DEV);
		xlsCreatePivotTable(workBook, "pivot_n", fromWorkbook, rowmax, DataConsolidateFunction.COUNT);
	}
	
	public static void xlsCreatePivotTable(XSSFWorkbook workBook, String workBookName, String fromWorkbook,int rowmax, DataConsolidateFunction function) {
        XSSFSheet pivotSheet = workBook.createSheet(workBookName);
        XSSFSheet sourceSheet = workBook.getSheet(fromWorkbook);

        int ncolumns = rowmax;
        CellAddress lastcell = new CellAddress (21, ncolumns-1);
        String address = "A1:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(0, 0);
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(source, position, sourceSheet);

        boolean flag = false;
        for (int i = 0; i< ncolumns; i++) {
        	XSSFCell cell = XLSUtils.getCell(sourceSheet, 0, i);
        	String text = cell.getStringCellValue();
        	if( !flag) {
        		flag = text.contains("roi");
        		if (text.contains(XLSExperimentDescriptors.CAP.toString()))
        			pivotTable.addRowLabel(i);
        		if (text.contains(XLSExperimentDescriptors.NFLIES.toString()))
        			pivotTable.addRowLabel(i);
        		continue;
        		}
        	if (text.contains("expt")) {
        		i+= 2;
        		continue;
        	}
        	pivotTable.addColumnLabel(function, i, text);
        }
	}
}
