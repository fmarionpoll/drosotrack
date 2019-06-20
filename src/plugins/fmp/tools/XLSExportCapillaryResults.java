package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.XLSExport;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults extends XLSExport {
	
	public static void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS capillary measures output");
		options = opt;

		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col0 = 0;
			int colmax = 0;
			int iSeries = 0;
			System.out.println("collect global infos on each experiment to preload data and find first and last time of the sequences");
			options.experimentList.readInfosFromAllExperiments();
			expAll = options.experimentList.getStartAndEndFromAllExperiments();
			expAll.step = options.experimentList.experimentList.get(0).vSequence.analysisStep;
			
			int i= 0;
			for (Experiment exp: options.experimentList.experimentList) 
			{
				System.out.println("output experiment "+i);
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 		colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.TOPLEVEL);
				if (options.topLevelDelta) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.TOPLEVELDELTA);
				if (options.bottomLevel) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.SUMGULPS);
				if (options.sum) 			colmax = getDataAndExport(exp, workbook, col0, charSeries, XLSExportItems.SUMLR);

				col0 = colmax;
				iSeries++;
				i++;
			}
			
			if (options.transpose && options.pivot) {
				System.out.println("Build pivot tables... ");
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

	private static Point writeHeader (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {
		
		int col0 = pt.x;
		
		pt = writeGenericHeader(exp, sheet, option, pt, transpose, charSeries);
		
		for (SequencePlus seq: exp.kymographArrayList) {
			XLSUtils.setValue(sheet, pt, transpose, seq.getName());
			pt.x++;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}

	private static Point writeData (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
		double scalingFactorToPhysicalUnits = exp.vSequence.capillaries.capillaryVolume / exp.vSequence.capillaries.capillaryPixels;
		
		int col0 = pt.x;
		int row0 = pt.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.vSequence.analysisStart;
		int endFrame 	= (int) exp.vSequence.analysisEnd;
		int step 		= expAll.step;
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
				for (int idataArray=0; idataArray< dataArrayList.size(); idataArray+=2) 
				{
					ArrayList<Integer> dataL = dataArrayList.get(idataArray) ;
					ArrayList<Integer> dataR = dataArrayList.get(idataArray+1);
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
				for (int idataArray=0; idataArray< dataArrayList.size(); idataArray++) 
				{
					ArrayList<Integer> data = dataArrayList.get(idataArray);
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
		}
		return pt;
	}
		


}
