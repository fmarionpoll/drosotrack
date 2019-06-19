package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.usermodel.Row;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.XLSExport;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportMoveResults extends XLSExport {

	public static void exportToFile(String filename, XLSExportOptions opt) {
		
		System.out.println("XLS move output");
		options = opt;
		
		try { 
			XSSFWorkbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			int col0 = 0;
			int colmax = 0;
			int iSeries = 0;
			options.experimentList.readInfosFromAllExperiments();
			expAll = options.experimentList.getStartAndEndFromAllExperiments();
			expAll.step = options.experimentList.experimentList.get(0).vSequence.analysisStep;
			
			for (Experiment exp: options.experimentList.experimentList) 
			{
				String charSeries = CellReference.convertNumToColString(iSeries);
			
				if (options.xyCenter) 
					colmax = xlsExportToWorkbook(exp, workbook, "xypos", XLSExportItems.XYCENTER, col0, charSeries);
				if (options.distance) 
					colmax = xlsExportToWorkbook(exp, workbook, "distance", XLSExportItems.DISTANCE, col0, charSeries);
				if (options.alive) 
					colmax = xlsExportToWorkbook(exp, workbook, "alive", XLSExportItems.ISALIVE, col0, charSeries);
				
				col0 = colmax;
				iSeries++;
			}
			
			if (options.transpose && options.pivot) { 
				String sourceSheetName = null;
				if (options.alive) 
					sourceSheetName = XLSExportItems.ISALIVE.toString();
				else if (options.xyCenter) 
					sourceSheetName = XLSExportItems.XYCENTER.toString();
				else if (options.distance) 
					sourceSheetName = XLSExportItems.DISTANCE.toString();
				xlsCreatePivotTables(workbook, sourceSheetName, colmax);
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
	
	
	private static ArrayList <ArrayList<Double>> getDataFromCages(Experiment exp, XLSExportItems option) {
		ArrayList <ArrayList<Double >> arrayList = new ArrayList <ArrayList <Double>> ();
		
		for (XYTaSeries posxyt: exp.vSequence.cages.flyPositionsList) {
			switch (option) {
			case DISTANCE: 
				arrayList.add(posxyt.getDoubleArrayList(ArrayListType.distance));
				break;
			case ISALIVE:
				arrayList.add(posxyt.getDoubleArrayList(ArrayListType.isalive));
				// TODO add threshold to cleanup data?
				break;
			case XYCENTER:
			default:
				arrayList.add(posxyt.getDoubleArrayList(ArrayListType.xyPosition));
				break;
			}
		}
		return arrayList;
	}

	public static int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, String title, XLSExportItems option, int col0, String charSeries) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Double >> arrayList = getDataFromCages(exp, option);

		XSSFSheet sheet = workBook.getSheet(title );
		boolean flag = (sheet == null);
		if (flag)
			sheet = workBook.createSheet(title);
		
		Point pt = writeGlobalInfos(exp, sheet, col0, options.transpose, option);
		pt = writeHeader(exp, sheet, pt, option, options.transpose, charSeries);
		pt = writeData(exp, sheet, pt, option, arrayList, options.transpose, charSeries);
		return pt.y;
	}
	
	private static Point writeGlobalInfos(Experiment exp, XSSFSheet sheet, int col0, boolean transpose, XLSExportItems option) {
		
		Point pt = new Point(col0, 0);

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

	
	private static Point writeHeader (Experiment exp, XSSFSheet sheet, Point pt, XLSExportItems option, boolean transpose, String charSeries) {
		
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
		
		XLSUtils.setValue(sheet, pt, transpose, "rois"+charSeries);
		pt.x++;
		if (exp.vSequence.isFileStack()) {
			XLSUtils.setValue(sheet, pt, transpose, "filename");
			pt.x++;
		}
		
		switch (option) {
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
		pt.x= col0;
		pt.y++;
		return pt;
	}

	
	private static Point writeData (Experiment exp, XSSFSheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Double >> arrayList, boolean transpose, String charSeries) {
	
		int col0 = pt.x;
		int row0 = pt.y;
		if (charSeries == null)
			charSeries = "t";
		int startFrame 	= (int) exp.vSequence.analysisStart;
		int step 		= expAll.step;
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
		
		int n_series = arrayList.size();
		
		for (int currentFrame=startFrame; currentFrame< endFrame; currentFrame+= step) {
			
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
			
			int t = currentFrame - startFrame;
			if (exp.vSequence.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.vSequence, startFrame + t) );
			}
			pt.x++;
						
			switch (option) {
			case DISTANCE:
				for (int i=0; i < n_series; i++ ) 
				{
					XLSUtils.setValue(sheet, pt, transpose, arrayList.get(i).get(t));
					pt.x++;
				}
				break;
			case ISALIVE:
				for (int i=0; i < n_series; i++ ) 
				{
					XLSUtils.setValue(sheet, pt, transpose, arrayList.get(i).get(t));
					pt.x++;
					XLSUtils.setValue(sheet, pt, transpose, arrayList.get(i).get(t));
					pt.x++;
				}
				break;

			case XYCENTER:
			default:
				for (int i=0; i < n_series; i++ ) 
				{
					int iarray = t*2;
					XLSUtils.setValue(sheet, pt, transpose, arrayList.get(i).get(iarray));
					pt.x++;
					XLSUtils.setValue(sheet, pt, transpose, arrayList.get(i).get(iarray+1));
					pt.x++;
				}
				break;
			}
			pt.y++;
		}
		return pt;
	}

	public static void xlsCreatePivotTables(XSSFWorkbook workBook, String fromWorkbook, int rowmax) {
        
		xlsCreatePivotTable(workBook, "pivot_sum", fromWorkbook, DataConsolidateFunction.SUM);
	}
	
}
