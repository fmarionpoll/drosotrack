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
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults {

	static XLSExportOptions 			options = null;
	static FileTime						image0Time;
	static long							image0TimeMinutes;
	
	static FileTime	 	firstImageTime 			= null;
	static FileTime	 	lastImageTime 			= null;
	static long			firstImageTimeMinutes 	= 0;
	static long			lastImageTimeMinutes 	= 0;
	static int			nintervals				= 0;
	
	public static void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS output");
		options = opt;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col0 = 0;
			int colmax = 0;
			int iSeries = 0;
			// get timemin et timemax - firstImageTime
			Experiment exp0 = options.experimentList.get(0);
			exp0.openSequenceAndMeasures();
			firstImageTime = exp0.vSequence.getImageModifiedTime(0);
			lastImageTime = exp0.vSequence.getImageModifiedTime(exp0.vSequence.getSizeT()-1);
			for (Experiment exp: options.experimentList) 
			{
				if (exp.openSequenceAndMeasures()) {
					if (firstImageTime.compareTo(exp.vSequence.getImageModifiedTime(0)) > 0) 
						firstImageTime = exp.vSequence.getImageModifiedTime(0);
					if (lastImageTime .compareTo(exp.vSequence.getImageModifiedTime(exp.vSequence.getSizeT()-1)) <0)
						lastImageTime = exp.vSequence.getImageModifiedTime(exp.vSequence.getSizeT()-1);
				}
			}
			firstImageTimeMinutes = firstImageTime.toMillis()/60000;
			lastImageTimeMinutes = lastImageTime.toMillis()/60000;
			System.out.println("First and intervals found ");
			
			for (Experiment exp: options.experimentList) 
			{
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 		colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.TOPLEVEL);
				if (options.topLevelDelta) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.TOPLEVELDELTA);
				if (options.bottomLevel) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.SUMGULPS);
				if (options.sum) 			colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.SUMLR);

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
	
	private static int getDataAndExport(Experiment exp, XSSFWorkbook workbook, int col0, String charSeries, XLSExportItems datatype) 
	{	
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(exp, datatype, options.t0);	
		int colmax = xlsExportCapillaryDataToWorkbook(exp, workbook, datatype.toString(), datatype, col0, charSeries, arrayList);
		if (options.onlyalive) {
			arrayList = trimDeadsFromArrayList(exp, arrayList);
			xlsExportCapillaryDataToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	private static ArrayList <ArrayList<Integer>> getDataFromRois(Experiment exp, XLSExportItems xlsoption, boolean optiont0) {
		
		ArrayList <ArrayList<Integer >> resultsArrayList = new ArrayList <ArrayList<Integer >> ();	
		for (SequencePlus seq: exp.kymographArrayList) {
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
	
	private static ArrayList <ArrayList<Integer>> trimDeadsFromArrayList(Experiment exp, ArrayList <ArrayList<Integer >> resultsArrayList) {
		
		ArrayList <ArrayList<Integer >> trimmedArrayList = new ArrayList <ArrayList<Integer >> ();
		int icapillary = 0;
		for (XYTaSeries flypos: exp.vSequence.cages.flyPositionsList) {
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
	
	private static int xlsExportCapillaryDataToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, XLSExportItems xlsoption, int col0, String charSeries, ArrayList <ArrayList<Integer >> arrayList) {
		System.out.println("export worksheet "+title);	
		
		XSSFSheet sheet = workBook.getSheet(title );
		boolean flag = (sheet == null);
		if (flag)
			sheet = workBook.createSheet(title);
		
		Point pt = writeGlobalInfos(exp, sheet, col0, options.transpose);
		pt = writeHeader(exp, sheet, xlsoption, pt, options.transpose, charSeries);
		pt = writeData(exp, sheet, xlsoption, pt, options.transpose, charSeries, arrayList);
		return pt.x;
	}
	
	private static Point writeGlobalInfos(Experiment exp, XSSFSheet sheet, int col0, boolean transpose) {
		Point pt = new Point(col0, 0);

		XLSUtils.setValue(sheet, pt, transpose, "expt");
		pt.x++;
		File file = new File(exp.vSequence.getFileName(0));
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
		XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.capillaryVolume);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.capillaryPixels);
		pt.x = col0;
		pt.y++;
		
		return pt;
	}

	public static Point addLineToHeader(Experiment exp, XSSFSheet sheet, Point pt, boolean transpose, XLSExperimentDescriptors desc) {
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, desc.toString());
		pt.x++;
		pt.x++;
		switch (desc) {
		case CAGE: 	// assume 2 capillaries/slot
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, i/2);
			break;
		case NFLIES: // assume first 2 and last 2 have no flies
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) {
				int j = 1;
				if (i < 2 || i > 17)
					j = 0;
				XLSUtils.setValue(sheet, pt, transpose, j);
			}
			break;
		case CAP:
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) {
				String name = exp.kymographArrayList.get(i).getName();
				String letter = name.substring(name.length() - 1);
				XLSUtils.setValue(sheet, pt, transpose, letter);
			}
			break;
		case DUM4: 
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) {
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
	
	private static Point writeHeader (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {
		
		int col0 = pt.x;
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.DATE);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.STIM);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.CONC);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.CAM);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.CAP);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.CAGE);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.TIME);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.NFLIES);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.DUM1);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.DUM2);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.DUM3);
		pt = addLineToHeader(exp, sheet, pt, transpose, XLSExperimentDescriptors.DUM4);
		image0Time = exp.vSequence.getImageModifiedTime(0);
		image0TimeMinutes = image0Time.toMillis()/60000;
	
		XLSUtils.setValue(sheet, pt, transpose, "rois"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "timeMin"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "filename" );
		pt.x++;
		for (SequencePlus seq: exp.kymographArrayList) {
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
	
	private static Point writeData (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
		int col0 = pt.x;

		double ratio = exp.vSequence.capillaries.capillaryVolume / exp.vSequence.capillaries.capillaryPixels;
		if (charSeries == null)
			charSeries = "t";
	
		
		int startFrame 	= (int) exp.vSequence.analysisStart;
		int step 		= exp.vSequence.analysisStep;
		int endFrame 	= (int) exp.vSequence.analysisEnd;
		int j = 0;
		
		FileTime imageTime = exp.vSequence.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		long diff = getnearest(imageTimeMinutes-firstImageTimeMinutes, step)/ step;
		imageTimeMinutes = firstImageTimeMinutes;
		pt.x = col0;
		for (int i = 0; i< diff; i++) {
			diff = getnearest(imageTimeMinutes-firstImageTimeMinutes, step);
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff);
			imageTimeMinutes += step;
			pt.y++;
		}
		
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+= step, j++) {
			pt.x = col0;

			imageTime = exp.vSequence.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;
			
			diff = getnearest(imageTimeMinutes-firstImageTimeMinutes, step);
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff);
			pt.x++;
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;

			if (exp.vSequence.isFileStack()) {
				String cs = exp.vSequence.getFileName(currentFrame);
				XLSUtils.setValue(sheet, pt, transpose, cs.substring(cs.lastIndexOf("\\") + 1) );
			}
			pt.x++;
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< dataArrayList.size(); i+=2) 
				{

					ArrayList<Integer> dataL = dataArrayList.get(i) ;
					ArrayList<Integer> dataR = dataArrayList.get(i+1);
					if (dataL != null && dataR != null) {
						if (j < dataL.size() && j < dataR.size()) {
							double value = (dataL.get(j)+dataR.get(j))*ratio;
							double valueold = 0.;
							XSSFCell cell = XLSUtils.getCell(sheet, pt, transpose);
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
					ArrayList<Integer> data = dataArrayList.get(i);
					if (data != null) {
						if (j < data.size()) {
							double value = data.get(j)*ratio;
							double valueold = 0.;
							XSSFCell cell = XLSUtils.getCell(sheet, pt, transpose);
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
