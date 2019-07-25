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
			
			progress.setMessage("Load measures...");
			progress.setLength(options.experimentList.experimentList.size());
			
			for (Experiment exp: options.experimentList.experimentList) 
			{
				String charSeries = CellReference.convertNumToColString(iSeries);
				
				if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVEL);
				if (options.topLevelDelta) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVELDELTA);
				if (options.bottomLevel) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.BOTTOMLEVEL);		
				if (options.derivative) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.DERIVEDVALUES);	
				if (options.consumption) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.SUMGULPS);
				if (options.sum) {		
					if (options.topLevel) 		col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVEL_LR);
					if (options.topLevelDelta) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.TOPLEVELDELTA_LR);
					if (options.consumption) 	col_end = getDataAndExport(exp, workbook, col_max, charSeries, EnumXLSExportItems.SUMGULPS_LR);
				}

				if (col_end > col_max)
					col_max = col_end;
				iSeries++;
				progress.incPosition();
			}
			
			if (options.transpose && options.pivot) {
				progress.setMessage( "Build pivot tables... ");
				
				String sourceSheetName = null;
				if (options.topLevel) sourceSheetName = EnumXLSExportItems.TOPLEVEL.toString();
				else if (options.topLevelDelta) sourceSheetName = EnumXLSExportItems.TOPLEVELDELTA.toString();
				else if (options.bottomLevel)  	sourceSheetName = EnumXLSExportItems.BOTTOMLEVEL.toString();
				else if (options.derivative) 	sourceSheetName = EnumXLSExportItems.DERIVEDVALUES.toString();	
				else if (options.consumption) 	sourceSheetName = EnumXLSExportItems.SUMGULPS.toString();
				else if (options.sum) 			sourceSheetName = EnumXLSExportItems.TOPLEVEL_LR.toString();
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
	
	private int getDataAndExport(Experiment exp, XSSFWorkbook workbook, int col0, String charSeries, EnumXLSExportItems datatype) 
	{	
		ArrayList <XLSCapillaryResults> arrayList = getDataFromRois(exp, datatype, options.t0);	
		int colmax = xlsExportToWorkbook(exp, workbook, datatype.toString(), datatype, col0, charSeries, arrayList);
		if (options.onlyalive) {
			trimDeadsFromArrayList(exp, arrayList);
			xlsExportToWorkbook(exp, workbook, datatype.toString()+"_alive", datatype, col0, charSeries, arrayList);
		}
		return colmax;
	}
	
	private ArrayList <XLSCapillaryResults> getDataFromRois(Experiment exp, EnumXLSExportItems xlsoption, boolean optiont0) {
		
		ArrayList <XLSCapillaryResults> resultsArrayList = new ArrayList <XLSCapillaryResults> ();	
		
		for (SequencePlus seq: exp.kymographArrayList) {
			XLSCapillaryResults results = new XLSCapillaryResults();
			results.name = seq.getName();
			switch (xlsoption) {
			case TOPLEVELDELTA:
			case TOPLEVELDELTA_LR:
				results.data = seq.subtractTi(seq.getArrayListFromRois(EnumArrayListType.topLevel));
				break;
			case DERIVEDVALUES:
				results.data = seq.getArrayListFromRois(EnumArrayListType.derivedValues);
				break;
			case SUMGULPS:
			case SUMGULPS_LR:
				results.data = seq.getArrayListFromRois(EnumArrayListType.cumSum);
				break;
			case BOTTOMLEVEL:
				results.data = seq.getArrayListFromRois(EnumArrayListType.bottomLevel);
				break;
			case TOPLEVEL:
			case TOPLEVEL_LR:
				if (optiont0)
					results.data = seq.subtractT0(seq.getArrayListFromRois(EnumArrayListType.topLevel));
				else
					results.data = seq.getArrayListFromRois(EnumArrayListType.topLevel);
				break;
			default:
				break;
			}
			resultsArrayList.add(results);
		}
			
		return resultsArrayList;
	}
	
	private void trimDeadsFromArrayList(Experiment exp, ArrayList <XLSCapillaryResults> resultsArrayList) {

		for (XYTaSeries flypos: exp.vSequence.cages.flyPositionsList) {
			
			String cagenumberString = flypos.roi.getName().substring(4);
			int cagenumber = Integer.parseInt(cagenumberString);
			if (cagenumber == 0 || cagenumber == 9)
				continue;
			
			for (XLSCapillaryResults capillaryResult : resultsArrayList) {
				if (getCageFromCapillaryName (capillaryResult.name) == cagenumber) {
					int ilastalive = flypos.getLastIntervalAlive();
					trimArrayLength(capillaryResult.data, ilastalive);
				}
			}
		}		
	}
	
	private void trimArrayLength (ArrayList<Integer> array, int ilastalive) {
		if (array == null)
			return;
		
		int arraysize = array.size();
		if (ilastalive < 0)
			ilastalive = 0;
		if (ilastalive > (arraysize-1))
			ilastalive = arraysize-1;
		
		array.subList(ilastalive, arraysize-1).clear();		
	}
	
	private int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, EnumXLSExportItems xlsExportOption, int col0, String charSeries, ArrayList <XLSCapillaryResults> arrayList) {
			
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

	private Point writeHeader (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, boolean transpose, String charSeries) {
		
		int col0 = pt.x;
		pt = writeGenericHeader(exp, sheet, option, pt, transpose, charSeries);
		int colseries = pt.x;
		
		for (SequencePlus seq: exp.kymographArrayList) {
			int col = getColFromKymoSequenceName(seq.getName());
			if (col >= 0)
				pt.x = colseries + col;
			XLSUtils.setValue(sheet, pt, transpose, seq.getName());
			pt.x++;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}
		
	private Point writeData (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, boolean transpose, String charSeries, ArrayList <XLSCapillaryResults> dataArrayList) {
		
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
		long referenceFileTimeImageFirstMinutes = 0;
		long referenceFileTimeImageLastMinutes = 0;
		
		if (options.absoluteTime) {
			referenceFileTimeImageFirstMinutes = expAll.fileTimeImageFirstMinutes;
			referenceFileTimeImageLastMinutes = expAll.fileTimeImageLastMinutes;
		}
		else {
			XLSNameAndPosition desc = getStackGlobalSeriesDescriptor(exp);
			if (desc != null) {
				referenceFileTimeImageFirstMinutes = desc.fileTimeImageFirstMinutes;
				referenceFileTimeImageLastMinutes = desc.fileTimeImageLastMinutes;
			}
			else
			{
				referenceFileTimeImageFirstMinutes = exp.fileTimeImageFirstMinutes;
				referenceFileTimeImageLastMinutes = exp.fileTimeImageLastMinutes;
			}
		}
			
		pt.x =0;
		imageTimeMinutes = referenceFileTimeImageLastMinutes;
		long diff = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step)/ step;
		imageTimeMinutes = referenceFileTimeImageFirstMinutes;
		for (int i = 0; i<= diff; i++) {
			long diff2 = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
			pt.y = (int) (diff2/step + row0); 
			XLSUtils.setValue(sheet, pt, transpose, "t"+diff2);
			imageTimeMinutes += step;
		}
		
		for (int currentFrame=startFrame; currentFrame < endFrame; currentFrame+=  step * options.pivotBinStep) {
			
			pt.x = col0;

			imageTime = exp.vSequence.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;

			long diff_current = getnearest(imageTimeMinutes-referenceFileTimeImageFirstMinutes, step);
			pt.y = (int) (diff_current/step + row0);

			pt.x++;
			XLSUtils.setValue(sheet, pt, transpose, imageTimeMinutes);
			pt.x++;
			if (exp.vSequence.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.vSequence, currentFrame) );
			}
			pt.x++;
			
			int colseries = pt.x;
			switch (option) {
			case TOPLEVEL_LR:
			case TOPLEVELDELTA_LR:
			case SUMGULPS_LR:
				for (int idataArray=0; idataArray< dataArrayList.size(); idataArray+=2) 
				{
					int colL = getColFromKymoSequenceName(dataArrayList.get(idataArray).name);
					if (colL >= 0)
						pt.x = colseries + colL;			

					ArrayList<Integer> dataL = dataArrayList.get(idataArray).data ;
					ArrayList<Integer> dataR = dataArrayList.get(idataArray+1).data;
					if (dataL != null && dataR != null) {
						int j = (currentFrame - startFrame)/step;
						if (j < dataL.size() && j < dataR.size()) {
							double value = (dataL.get(j)+dataR.get(j))*scalingFactorToPhysicalUnits;
							XLSUtils.setValue(sheet, pt, transpose, value);
							
							Point pt0 = new Point(pt);
							pt0.x ++;
							int colR = getColFromKymoSequenceName(dataArrayList.get(idataArray+1).name);
							if (colR >= 0)
								pt0.x = colseries + colR;
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
					int col = getColFromKymoSequenceName(dataArrayList.get(idataArray).name);
					if (col >= 0)
						pt.x = colseries + col;			

					ArrayList<Integer> data = dataArrayList.get(idataArray).data;
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
