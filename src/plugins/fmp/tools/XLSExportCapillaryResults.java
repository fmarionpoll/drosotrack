package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
					ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(XLSExportItems.TOPLEVEL, options.t0);	
					rowmax = xlsExportCapillaryDataToWorkbook(workbook, "toplevel", XLSExportItems.TOPLEVEL, row0, charSeries, arrayList);
					if (options.onlyalive) {
						arrayList = trimDeadsFromArrayList(arrayList);
						xlsExportCapillaryDataToWorkbook(workbook, "toplevel_alive", XLSExportItems.TOPLEVEL, row0, charSeries, arrayList);
					}
				}
				if (options.bottomLevel) {
					ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(XLSExportItems.BOTTOMLEVEL, options.t0);	
					rowmax = xlsExportCapillaryDataToWorkbook(workbook, "bottomlevel", XLSExportItems.BOTTOMLEVEL, row0, charSeries, arrayList);
				}
				if (options.derivative) {
					ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(XLSExportItems.DERIVEDVALUES, options.t0);	
					rowmax = xlsExportCapillaryDataToWorkbook(workbook, "derivative", XLSExportItems.DERIVEDVALUES, row0, charSeries, arrayList);
				}
				if (options.consumption) {
					ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(XLSExportItems.SUMGULPS, options.t0);	
					rowmax = xlsExportCapillaryDataToWorkbook(workbook, "sumGulps", XLSExportItems.SUMGULPS, row0, charSeries, arrayList);
					if (options.onlyalive) {
						arrayList = trimDeadsFromArrayList(arrayList);
						xlsExportCapillaryDataToWorkbook(workbook, "sumGulps_alive", XLSExportItems.SUMGULPS, row0, charSeries, arrayList);
					}
				}
				if (options.sum) {
					ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(XLSExportItems.SUMLR, options.t0);	
					rowmax = xlsExportCapillaryDataToWorkbook(workbook, "sumL+R", XLSExportItems.SUMLR, row0, charSeries, arrayList);
					if (options.onlyalive) {
						arrayList = trimDeadsFromArrayList(arrayList);
						xlsExportCapillaryDataToWorkbook(workbook, "sumL+R_alive", XLSExportItems.SUMLR, row0, charSeries, arrayList);
					}
				}
				row0 = rowmax;
				iSeries++;
			}
			if (options.topLevel && options.transpose && options.pivot) 
				xlsCreatePivotTables(workbook, "toplevel", rowmax);
				
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
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
				resultsArrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			default:
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
	
	private static int xlsExportCapillaryDataToWorkbook(XSSFWorkbook workBook, String title, XLSExportItems xlsoption, int row0, String charSeries, ArrayList <ArrayList<Integer >> arrayList) {
		System.out.println("export worksheet "+title);	
		
		XSSFSheet sheet = workBook.getSheet(title );
		boolean flag = (sheet == null);
		if (flag)
			sheet = workBook.createSheet(title);
		
		Point pt = writeGlobalInfos(sheet, row0, options.transpose, charSeries);
		pt = writeColumnHeaders(sheet, xlsoption, pt, options.transpose, charSeries);
		pt = writeData(sheet, xlsoption, pt, options.transpose, charSeries, arrayList);
		return pt.y;
	}
	
	private static Point writeGlobalInfos(XSSFSheet sheet, int row0, boolean transpose, String charSeries) {
		Point pt = new Point(0, row0);

		XLSUtils.setValue(sheet, pt, transpose, "expt"+charSeries);
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
		
		pt.x = 0;
		XLSUtils.setValue(sheet, pt, transpose, "scale"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "capillary" );
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.capillaries.capillaryVolume);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.capillaries.capillaryPixels);
		pt.x = 0;
		pt.y++;
		
		return pt;
	}

	public static Point addLine(XSSFSheet sheet, Point pt, boolean transpose, XLSExperimentDescriptors desc) {
		XLSUtils.setValue(sheet, pt, transpose, desc.toString());
		pt.x++;
		pt.x++;
		switch (desc) {
		case CAGE: 	// assume 2 capillaries/slot
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, i/2);
			break;
		case NFLIES: // assume 2 capillaries/slot
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) {
				int j = 1;
				if (i < 2 || i > 17)
					j = 0;
				XLSUtils.setValue(sheet, pt, transpose, j);
			}
			break;
		case CAP: // assume 2 capillaries/slot
			for (int i= 0; i < kymographArrayList.size(); i++, pt.x++) {
				String name = kymographArrayList.get(i).getName();
				String letter = name.substring(name.length() - 1);
				XLSUtils.setValue(sheet, pt, transpose, letter);
			}
			break;
		default:
			break;
		}
		pt.x = 0;
		pt.y++;
		return pt;
	}
	
	private static Point writeColumnHeaders (XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {
		
		pt.x = 0;
		if (charSeries.equals("A")) {
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.DATE);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.STIM);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.CONC);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.CAM);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.CAP);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.CAGE);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.TIME);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.NFLIES);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.DUM1);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.DUM2);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.DUM3);
			pt = addLine(sheet, pt, transpose, XLSExperimentDescriptors.DUM4);
		}
		
		XLSUtils.setValue(sheet, pt, transpose, "rois"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "filename" );
		pt.x++;
		for (SequencePlus seq: kymographArrayList) {
			XLSUtils.setValue(sheet, pt, transpose, seq.getName() );
			pt.x++;
		}
		pt.x = 0;
		pt.y++;
		return pt;
	}

	private static Point writeData (XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
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

			XLSUtils.setValue(sheet, pt, transpose, charSeries+t);
			pt.x++;

			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(t);
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
	//						if (cell.getCellType() == CellType.NUMERIC)
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
	//						if (cell.getCellType() == CellType.NUMERIC)
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
