package plugins.fmp.tools;

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
			System.out.println("collect global infos on each experiment to preload data and find first and last time of the sequences");
			options.experimentList.readInfosFromAllExperiments();
			expAll = options.experimentList.getStartAndEndFromAllExperiments();
			expAll.step = options.experimentList.experimentList.get(0).vSequence.analysisStep;
			
			int i= 0;
			for (Experiment exp: options.experimentList.experimentList) 
			{
				System.out.println("output experiment "+i);
				String charSeries = CellReference.convertNumToColString(iSeries);
			
				if (options.xyCenter) 
					colmax = xlsExportToWorkbook(exp, workbook, col0, charSeries, XLSExportItems.XYCENTER);
				if (options.distance) 
					colmax = xlsExportToWorkbook(exp, workbook, col0, charSeries, XLSExportItems.DISTANCE);
				if (options.alive) 
					colmax = xlsExportToWorkbook(exp, workbook, col0, charSeries,  XLSExportItems.ISALIVE);
				
				col0 = colmax;
				iSeries++;
				i++;
			}
			
			if (options.transpose && options.pivot) { 
				System.out.println("Build pivot tables... ");
				String sourceSheetName = null;
				if (options.alive) 
					sourceSheetName = XLSExportItems.ISALIVE.toString();
				else if (options.xyCenter) 
					sourceSheetName = XLSExportItems.XYCENTER.toString();
				else if (options.distance) 
					sourceSheetName = XLSExportItems.DISTANCE.toString();
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

	public static int xlsExportToWorkbook(Experiment exp, XSSFWorkbook workBook, int col0, String charSeries, XLSExportItems option) {

		ArrayList <ArrayList<Double >> arrayList = getDataFromCages(exp, option);

		XSSFSheet sheet = workBook.getSheet(option.toString());
		boolean flag = (sheet == null);
		if (flag)
			sheet = workBook.createSheet(option.toString());
		
		Point pt = writeGlobalInfos(exp, sheet, col0, options.transpose, option);
		pt = writeHeader(exp, sheet, pt, option, options.transpose, charSeries);
		pt = writeData(exp, sheet, pt, option, arrayList, options.transpose, charSeries);
		return pt.x;
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

		pt = writeGenericHeader(exp, sheet, option, pt, transpose, charSeries);
		
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
		pt.x = col0;
		pt.y++;
		return pt;
	}
	
	private static Point writeData (Experiment exp, XSSFSheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Double >> dataArrayList, boolean transpose, String charSeries) {
	
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
		
		if (dataArrayList.size() == 0) {
			pt.x = columnOfNextSeries(exp, option, col0);
			return pt;
		}
		
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
			if (exp.vSequence.isFileStack()) {
				XLSUtils.setValue(sheet, pt, transpose, getShortenedName(exp.vSequence, currentFrame) );
			}
			pt.x++;
			
			int t = (currentFrame - startFrame)/step;
			switch (option) {
			case DISTANCE:
				for (int idataArray=0; idataArray < dataArrayList.size(); idataArray++ ) 
				{
//					if (dataArrayList.get(idataArray).size() > t)
						XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
				}
				break;
			case ISALIVE:
				for (int idataArray=0; idataArray < dataArrayList.size(); idataArray++ ) 
				{
//					if (dataArrayList.get(idataArray).size() > t)
						XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
//					if (dataArrayList.get(idataArray).size() > t)
						XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(idataArray).get(t));
					pt.x++;
				}
				break;

			case XYCENTER:
			default:
				for (int iDataArray=0; iDataArray < dataArrayList.size(); iDataArray++ ) 
				{
					int iarray = t*2;
//					if (dataArrayList.get(iDataArray).size() > iarray)
						XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(iDataArray).get(iarray));
					pt.x++;
//					if (dataArrayList.get(iDataArray).size() > iarray+1)
						XLSUtils.setValue(sheet, pt, transpose, dataArrayList.get(iDataArray).get(iarray+1));
					pt.x++;
				}
				break;
			}
		} 
		pt.x = columnOfNextSeries(exp, option, col0);
		return pt;
	}

	private static int columnOfNextSeries(Experiment exp, XLSExportItems option, int currentcolumn) {
		int n = 2;
		if(option == XLSExportItems.DISTANCE) 
			n= 1;
		int value = currentcolumn + exp.vSequence.cages.flyPositionsList.size() * n +4;
		return value;
	}
	
	
}
