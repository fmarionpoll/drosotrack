package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


import org.apache.poi.ss.SpreadsheetVersion;

import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults {

	static XLSExportOptions options = null;
	static FileTime			image0Time;
	static long				image0TimeMinutes;
	
	static Experiment expAll = null;
	static int			nintervals					= 0;
	
	public static void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS capillary measures output");
		options = opt;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col0 = 0;
			int colmax = 0;
			int iSeries = 0;
			
			options.experimentList.readInfosFromAllExperiments();
			expAll = options.experimentList.getStartAndEndFromAllExperiments();
			
			for (Experiment exp: options.experimentList.experimentList) 
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
			
			if (options.transpose && options.pivot) {
				String sourceSheetName = null;
				if (options.topLevel) sourceSheetName = XLSExportItems.TOPLEVEL.toString();
				else if (options.topLevelDelta) sourceSheetName = XLSExportItems.TOPLEVELDELTA.toString();
				else if (options.bottomLevel)  	sourceSheetName = XLSExportItems.BOTTOMLEVEL.toString();
				else if (options.derivative) 	sourceSheetName = XLSExportItems.DERIVEDVALUES.toString();	
				else if (options.consumption) 	sourceSheetName = XLSExportItems.SUMGULPS.toString();
				else if (options.sum) 			sourceSheetName = XLSExportItems.SUMLR.toString();
				if (sourceSheetName != null)
					xlsCreatePivotTables(workbook, sourceSheetName);
			}

				
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
			trimDeadsFromArrayList(exp, arrayList);
			xlsExportCapillaryDataToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	private static ArrayList <ArrayList<Integer>> getDataFromRois(Experiment exp, XLSExportItems xlsoption, boolean optiont0) {
		
		ArrayList <ArrayList<Integer >> resultsArrayList = new ArrayList <ArrayList<Integer >> ();	
		
		for (SequencePlus seq: exp.kymographArrayList) {
			switch (xlsoption) {
			case TOPLEVELDELTA:
				resultsArrayList.add(subtractTi(seq.getArrayListFromRois(ArrayListType.topLevel)));
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
				if (optiont0)
					resultsArrayList.add(subtractT0(seq.getArrayListFromRois(ArrayListType.topLevel)));
				else
					resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			default:
				break;
			}
		}
			
		return resultsArrayList;
	}
	
	private static ArrayList<Integer> subtractT0 (ArrayList<Integer> array) {

		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	private static ArrayList<Integer> subtractTi(ArrayList<Integer > array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
			item0 = value;
		}
		return array;
	}
	
	private static void trimDeadsFromArrayList(Experiment exp, ArrayList <ArrayList<Integer >> resultsArrayList) {
		
		ArrayList <ArrayList<Integer >> trimmedArrayList = new ArrayList <ArrayList<Integer >> ();
		int icapillary = 0;
		for (XYTaSeries flypos: exp.vSequence.cages.flyPositionsList) {
			int ilastalive = getLastIntervalAlive(flypos);
			trimArrayLength(resultsArrayList.get(icapillary), ilastalive);
			trimmedArrayList.add(resultsArrayList.get(icapillary));
			trimArrayLength(resultsArrayList.get(icapillary+1), ilastalive);
			trimmedArrayList.add(resultsArrayList.get(icapillary+1));
			icapillary += 2;
		}		
	}
	
	private static int getLastIntervalAlive(XYTaSeries flypos) {
		if (flypos.lastIntervalAlive >= 0)
			return flypos.lastIntervalAlive;
		return flypos.computeLastIntervalAlive();
	}
	
	private static void trimArrayLength (ArrayList<Integer> array, int ilastalive) {
		if (array == null)
			return;
		int arraysize = array.size();
		for (int i = arraysize-1; i > ilastalive; i--) {
			array.remove(i);
		}
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
		case DUM1: {
			Path path = Paths.get(exp.vSequence.getFileName());
			String name = path.getName(path.getNameCount() -2).toString();
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++)
				XLSUtils.setValue(sheet, pt, transpose, name);
			}
			break;
		case DUM2:	{
			Path path = Paths.get(exp.vSequence.getFileName());
			String name = path.getName(path.getNameCount() -3).toString();
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++)
				XLSUtils.setValue(sheet, pt, transpose, name);
			}
			break;
		case DUM3:	{
			Path path = Paths.get(exp.vSequence.getFileName());
			String name = path.getName(path.getNameCount() -4).toString();
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++)
				XLSUtils.setValue(sheet, pt, transpose, name);
			}
			break;
		case DUM4: 
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, sheet.getSheetName());
			break;
		case DATE:
			SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	       for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, df.format(exp.fileTimeImageFirst.toMillis()));
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

	private static long getnearest(long value, int step) {
		long diff0 = (value /step)*step;
		long diff1 = diff0 + step;
		if ((value - diff0 ) < (diff1 - value))
			value = diff0;
		else
			value = diff1;
		return value;
	}
	
	private static Point writeData (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
		int col0 = pt.x;
		int row0 = pt.y;

		double scalingFactorToPhysicalUnits = exp.vSequence.capillaries.capillaryVolume / exp.vSequence.capillaries.capillaryPixels;
		if (charSeries == null)
			charSeries = "t";
	
		int startFrame 	= (int) exp.vSequence.analysisStart;
		int step 		= exp.vSequence.analysisStep;
		int endFrame 	= (int) exp.vSequence.analysisEnd;
		
		FileTime imageTime = exp.vSequence.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		if (col0 == 0) {
			imageTimeMinutes = expAll.fileTimeImageLastMinutes;
		}
		long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step)/ step;
		imageTimeMinutes = expAll.fileTimeImageFirstMinutes;
		pt.x = col0;
		for (int i = 0; i<= diff; i++) {
			long diff2 = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step);
			pt.y = (int) (diff2/step + row0); 
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff2);
			imageTimeMinutes += step;
		}
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+= step) {
			pt.x = col0;

			imageTime = exp.vSequence.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;
			diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step);
			pt.y = (int) (diff/step + row0);
			long diff0 = getnearest(imageTimeMinutes-exp.fileTimeImageFirst.toMillis()/60000, step);
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff0);
			pt.x++;

			
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;

			if (exp.vSequence.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.vSequence, currentFrame) );
			}
			pt.x++;
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< dataArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = dataArrayList.get(i) ;
					ArrayList<Integer> dataR = dataArrayList.get(i+1);
					if (dataL != null && dataR != null) {
						int j = (currentFrame - startFrame)/step;
						if (j < dataL.size() && j < dataR.size()) {
							double value = (dataL.get(j)+dataR.get(j))*scalingFactorToPhysicalUnits;
							XLSUtils.setValue(sheet, pt, transpose, value);
							Point pt0 = new Point(pt);
							pt0.x ++;
							value = (dataL.get(j)-dataR.get(j))*scalingFactorToPhysicalUnits/value;
							XLSUtils.setValue(sheet, pt0, transpose, value);
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
						int j = (currentFrame - startFrame)/step;
						if (j < data.size()) {
							double value = data.get(j)*scalingFactorToPhysicalUnits;
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
	
	public static String getShortenedName(SequenceVirtual seq, int t) {
		String cs = seq.getFileName(t);
		return cs.substring(cs.lastIndexOf("\\") + 1) ;
	}
	
	public static void xlsCreatePivotTables(XSSFWorkbook workBook, String fromWorkbook) {
        
		xlsCreatePivotTable(workBook, "pivot_avg", fromWorkbook, DataConsolidateFunction.AVERAGE);
		xlsCreatePivotTable(workBook, "pivot_std", fromWorkbook, DataConsolidateFunction.STD_DEV);
		xlsCreatePivotTable(workBook, "pivot_n", fromWorkbook, DataConsolidateFunction.COUNT);
	}
	
	public static void xlsCreatePivotTable(XSSFWorkbook workBook, String workBookName, String fromWorkbook, DataConsolidateFunction function) {
        XSSFSheet pivotSheet = workBook.createSheet(workBookName);
        XSSFSheet sourceSheet = workBook.getSheet(fromWorkbook);

        int lastRowNum = sourceSheet.getLastRowNum();
        int lastColumnNum = sourceSheet.getRow(0).getLastCellNum();
        CellAddress lastcell = new CellAddress (lastRowNum, lastColumnNum-1);
        String address = "A1:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(0, 0);
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(source, position, sourceSheet);

        boolean flag = false;
        for (int i = 0; i< lastColumnNum; i++) {
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
        	pivotTable.addColumnLabel(function, i, text);
        }
	}
	
	
}
