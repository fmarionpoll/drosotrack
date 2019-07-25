package plugins.fmp.drosoTools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.frame.progress.ProgressFrame;
import plugins.fmp.drosoSequence.Experiment;
import plugins.fmp.drosoSequence.XYTaSeries;

public class XLSExportMoveResults extends XLSExport {

	public void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS move output");
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
			
				if (options.xyCenter)  	col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportItems.XYCENTER);
				if (options.distance) 	col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries, EnumXLSExportItems.DISTANCE);
				if (options.alive) 		col_end = xlsExportToWorkbook(exp, workbook, col_max, charSeries,  EnumXLSExportItems.ISALIVE);
				
				if (col_end > col_max)
					col_max = col_end;
				iSeries++;
				progress.incPosition();
			}
			
			if (options.transpose && options.pivot) { 
				progress.setMessage( "Build pivot tables... ");
				
				String sourceSheetName = null;
				if (options.alive) 
					sourceSheetName = EnumXLSExportItems.ISALIVE.toString();
				else if (options.xyCenter) 
					sourceSheetName = EnumXLSExportItems.XYCENTER.toString();
				else if (options.distance) 
					sourceSheetName = EnumXLSExportItems.DISTANCE.toString();
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
	
	private static ArrayList <ArrayList<Double>> getDataFromCages(Experiment exp, EnumXLSExportItems option) {

		ArrayList <ArrayList<Double >> arrayList = new ArrayList <ArrayList <Double>> ();
		
		for (XYTaSeries posxyt: exp.vSequence.cages.flyPositionsList) {
			switch (option) {
			case DISTANCE: 
				arrayList.add(posxyt.getDoubleArrayList(EnumArrayListType.distance));
				break;
			case ISALIVE:
				arrayList.add(posxyt.getDoubleArrayList(EnumArrayListType.isalive));
				// TODO add threshold to cleanup data?
				break;
			case XYCENTER:
			default:
				arrayList.add(posxyt.getDoubleArrayList(EnumArrayListType.xyPosition));
				break;
			}
		}
		return arrayList;
	}

	public int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, int col0, String charSeries, EnumXLSExportItems xlsExportOption) {

		ArrayList <ArrayList<Double >> arrayList = getDataFromCages(exp, xlsExportOption);

		XSSFSheet sheet = workBook.getSheet(xlsExportOption.toString());
		if (sheet == null) 
			sheet = workBook.createSheet(xlsExportOption.toString());
		
		Point pt = new Point(col0, 0);
		if (options.collateSeries) {
			pt = getStackColumnPosition(exp, pt);
		}
		pt = writeGlobalInfos(exp, sheet, pt, options.transpose, xlsExportOption);
		pt = writeHeader(exp, sheet, pt, xlsExportOption, options.transpose, charSeries);
		pt = writeData(exp, sheet, pt, xlsExportOption, arrayList, options.transpose, charSeries);
		return pt.x;
	}
	
	private Point writeGlobalInfos(Experiment exp, XSSFSheet sheet, Point pt, boolean transpose, EnumXLSExportItems option) {
		
		int col0 = pt.x;
		
		XLSUtils.setValue(sheet, pt, transpose, "expt");
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "name");
		File file = new File(exp.vSequence.getFileName(0));
		String path = file.getParent();
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, path);
		pt.y++;
		
		pt.x=col0;
		Point pt1 = pt;
		XLSUtils.setValue(sheet, pt, transpose, "n_cages");
		pt1.x++;
		XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.cages.flyPositionsList.size());
		switch (option) {
		case DISTANCE:
			break;
		case ISALIVE:
			pt1.x++;
			XLSUtils.setValue(sheet, pt, transpose, "threshold");
			pt1.x++;
			XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.cages.detect.threshold);
			break;
		case XYCENTER:
		default:
			break;
		}

		pt.x=col0;
		pt.y++;
		return pt;
	}

	private Point writeHeader (Experiment exp, XSSFSheet sheet, Point pt, EnumXLSExportItems xlsExportOption, boolean transpose, String charSeries) {
		
		int col0 = pt.x;
		pt = writeGenericHeader(exp, sheet, xlsExportOption, pt, transpose, charSeries);
		
		switch (xlsExportOption) {
		case DISTANCE:
			for (XYTaSeries posxyt: exp.vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
			}
			break;
			
		case ISALIVE:
			for (XYTaSeries posxyt: exp.vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
			}
			break;
		case XYCENTER:
		default:
			for (XYTaSeries posxyt: exp.vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0+".x");
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, name0+".y");
				pt.x++;
			}
			break;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}
	
	private Point writeData (Experiment exp, XSSFSheet sheet, Point pt, EnumXLSExportItems option, ArrayList <ArrayList<Double >> dataArrayList, boolean transpose, String charSeries) {
	
		int col0 = pt.x;
		int row0 = pt.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.vSequence.analysisStart;
		int endFrame 	= (int) exp.vSequence.analysisEnd;
		int step 		= expAll.step;
		
		FileTime imageTime = exp.vSequence.getImageModifiedTime(startFrame);
		long imageTimeMinutes = imageTime.toMillis()/ 60000;
		if (options.absoluteTime && (col0 ==0)) {
			imageTimeMinutes = expAll.fileTimeImageLastMinutes;
			long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step)/ step;
			imageTimeMinutes = expAll.fileTimeImageFirstMinutes;
			pt.x = col0;
			for (int i = 0; i<= diff; i++) {
				long diff2 = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step);
				pt.y = (int) (diff2/step + row0); 
				XLSUtils.setValue(sheet, pt, transpose, "t"+diff2);
				imageTimeMinutes += step;
			}
		}
		
//		if (dataArrayList.size() == 0) {
//			pt.x = columnOfNextSeries(exp, option, col0);
//			return pt;
//		}
		
		for (int currentFrame=startFrame; currentFrame< endFrame; currentFrame+= step  * options.pivotBinStep) {
			
			pt.x = col0;
 
			long diff0 = (currentFrame - startFrame)/step;
			imageTime = exp.vSequence.getImageModifiedTime(currentFrame);
			imageTimeMinutes = imageTime.toMillis()/ 60000;

			if (options.absoluteTime) {
				long diff = getnearest(imageTimeMinutes-expAll.fileTimeImageFirstMinutes, step);
				pt.y = (int) (diff/step + row0);
				diff0 = diff; //getnearest(imageTimeMinutes-exp.fileTimeImageFirst.toMillis()/60000, step);
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
			
			int t = (currentFrame - startFrame)/step;
			switch (option) {
			case DISTANCE:
				for (int idataArray=0; idataArray < dataArrayList.size(); idataArray++ ) 
				{
					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
				}
				break;
			case ISALIVE:
				for (int idataArray=0; idataArray < dataArrayList.size(); idataArray++ ) 
				{
					Double value = dataArrayList.get(idataArray).get(t);
					if (value > 0) {
						XLSUtils.setValue(sheet, pt, transpose, value );
						pt.x++;
						XLSUtils.setValue(sheet, pt, transpose, value);
						pt.x++;
					}
					else
						pt.x += 2;
				}
				break;

			case XYCENTER:
			default:
				for (int iDataArray=0; iDataArray < dataArrayList.size(); iDataArray++ ) 
				{
					int iarray = t*2;
					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(iDataArray).get(iarray));
					pt.x++;

					XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(iDataArray).get(iarray+1));
					pt.x++;
				}
				break;
			}
		} 
		pt.x = columnOfNextSeries(exp, option, col0);
		return pt;
	}

	private int columnOfNextSeries(Experiment exp, EnumXLSExportItems option, int currentcolumn) {
		int n = 2;
		int value = currentcolumn + exp.vSequence.cages.cageLimitROIList.size() * n + 3;
		return value;
	}
	
	
}
