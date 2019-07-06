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

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportCapillaryResults extends XLSExport {
	
	public void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS capillary measures output");
		options = opt;
		ProgressFrame progress = new ProgressFrame("Export data to Excel");
		
		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col_max = 0;
			int col_end = 0;
			int iSeries = 0;
			
			options.experimentList.readInfosFromAllExperiments();
			expAll = options.experimentList.getStartAndEndFromAllExperiments();
			expAll.step = options.experimentList.experimentList.get(0).vSequence.analysisStep;
			listOfStacks = new ArrayList <XLSNameAndPosition> ();
			
			progress.setMessage( "Load measures...");
			progress.setLength(options.experimentList.experimentList.size());
			
			for (Experiment exp: options.experimentList.experimentList) 
			{
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.TOPLEVEL);
				if (options.topLevelDelta) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.TOPLEVELDELTA);
				if (options.bottomLevel) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.SUMGULPS);
				if (options.sum) {		
					if (options.topLevel) col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.SUMLR);
					if (options.topLevelDelta) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, XLSExportItems.TOPLEVELDELTALR);
				}

				if (col_end > col_max)
					col_max = col_end;
				iSeries++;
				progress.incPosition();
			}
			
			if (options.transpose && options.pivot) {
				progress.setMessage( "Build pivot tables... ");
				
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
			
			progress.setMessage( "Save Excel file to disk... ");
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		progress.close();
		System.out.println("XLS output finished");
	}
	
	private int getDataAndExport(Experiment exp, XSSFWorkbook workbook, int col0, String charSeries, XLSExportItems datatype) 
	{	
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(exp, datatype, options.t0);	
		int colmax = xlsExportToWorkbook(exp, workbook, datatype.toString(), datatype, col0, charSeries, arrayList);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp, arrayList);
			xlsExportToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	private ArrayList <ArrayList<Integer>> getDataFromRois(Experiment exp, XLSExportItems xlsoption, boolean optiont0) {
		
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
			case TOPLEVELDELTALR:
				resultsArrayList.add(subtractTi(seq.getArrayListFromRois(ArrayListType.topLevel)));
				break;
			default:
				break;
			}
		}
			
		return resultsArrayList;
	}
	
	private ArrayList<Integer> subtractT0 (ArrayList<Integer> array) {

		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	private ArrayList<Integer> subtractTi(ArrayList<Integer > array) {
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
	
	private void trimDeadsFromArrayList(Experiment exp, ArrayList <ArrayList<Integer >> resultsArrayList) {
		
		ArrayList <ArrayList<Integer >> trimmedArrayList = new ArrayList <ArrayList<Integer >> ();
		int icapillary = 0;
		for (XYTaSeries flypos: exp.vSequence.cages.flyPositionsList) {
			int ilastalive = flypos.getLastIntervalAlive();
			trimArrayLength(resultsArrayList.get(icapillary), ilastalive);
			trimmedArrayList.add(resultsArrayList.get(icapillary));
			trimArrayLength(resultsArrayList.get(icapillary+1), ilastalive);
			trimmedArrayList.add(resultsArrayList.get(icapillary+1));
			icapillary += 2;
		}		
	}
	
	private void trimArrayLength (ArrayList<Integer> array, int ilastalive) {
		if (array == null)
			return;
		int arraysize = array.size();
		for (int i = arraysize-1; i > ilastalive; i--) {
			array.remove(i);
		}
	}
	
	private int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, XLSExportItems xlsExportOption, int col0, String charSeries, ArrayList <ArrayList<Integer >> arrayList) {
			
		XSSFSheet sheet = workBook.getSheet(title );
		if (sheet == null)
			sheet = workBook.createSheet(title);
		
		Point pt = new Point(col0, 0);
		if (options.collateSeries) {
			pt = getStackColumnPosition(exp, pt);
		}
		
		pt = writeGlobalInfos(exp, sheet, pt, options.transpose);
		pt = writeHeader(exp, sheet, xlsExportOption, pt, options.transpose, charSeries);
		pt = writeData(exp, sheet, xlsExportOption, pt, options.transpose, charSeries, arrayList);
		return pt.x;
	}
	
	private Point writeGlobalInfos(Experiment exp, XSSFSheet sheet, Point pt, boolean transpose) {

		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, "expt");
		pt.x++;
		File file = new File(exp.vSequence.getFileName(0));
		String path = file.getParent();
		XLSUtils.setValue(sheet, pt, transpose, path);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "µl" );
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "pixels" );
		pt.x++;
		pt.y++;
		
		pt.x = col0;
		XLSUtils.setValue(sheet, pt, transpose, "scale");
		pt.x++;
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.volume);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.pixels);
		pt.x = col0;
		pt.y++;
		
		return pt;
	}

	private Point writeHeader (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {
		
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

	private Point writeData (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <ArrayList<Integer >> dataArrayList) {
		
		double scalingFactorToPhysicalUnits = exp.vSequence.capillaries.volume / exp.vSequence.capillaries.pixels;
		
		int col0 = pt.x;
		int row0 = pt.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.vSequence.analysisStart;
		int endFrame 	= (int) exp.vSequence.analysisEnd;
		int step 		= expAll.step;

		FileTime imageTime = exp.vSequence.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		if (col0 ==0) {
			pt.x = col0;
			
			if (options.absoluteTime) {
				imageTimeMinutes = expAll.fileTimeImageLastMinutes;
				long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step)/ step;
				imageTimeMinutes = expAll.fileTimeImageFirstMinutes;
				for (int i = 0; i<= diff; i++) {
					long diff2 = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step);
					pt.y = (int) (diff2/step + row0); 
					XLSUtils.setValue(sheet, pt, transpose, "t"+diff2);
					imageTimeMinutes += step;
				}
			}
			else {
				for (int i = 0; i<= expAll.number_of_frames; i+= step) {
					pt.y = i/step + row0; 
					XLSUtils.setValue(sheet, pt, transpose, "t"+i);
				}
			}
		}
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+=  step * options.pivotBinStep) {
			
			pt.x = col0;

			long diff0 = (currentFrame - startFrame)/step;
			imageTime = exp.vSequence.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;

			if (options.absoluteTime) {
				long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step);
				pt.y = (int) (diff/step + row0);
				diff0 = diff;
			} else {
				pt.y = (int) diff0 + row0;
			}
			//XLSUtils.setValue(sheet, pt, transpose, "t"+diff0);
			pt.x++;
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;
			if (exp.vSequence.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.vSequence, currentFrame) );
			}
			pt.x++;
			
			switch (option) {
			case SUMLR:
			case TOPLEVELDELTALR:
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
		//pt.x = columnOfNextSeries(exp, option, col0);
		return pt;
	}
		
}
