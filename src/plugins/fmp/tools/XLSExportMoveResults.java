package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportMoveResults {

	static XLSExportMoveOptions options = null;
	static SequenceVirtual vSequence = null;
	
	public static void exportToFile(String filename, XLSExportMoveOptions opt) {
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
				vSequence = new SequenceVirtual();
				if (null == vSequence.loadVirtualStackAt(exp.filename))
					continue;
				vSequence.xmlReadDrosoTrackDefault();
				String charSeries = CellReference.convertNumToColString(iSeries);
			
				if (options.xyCenter) 
					rowmax = xlsExportToWorkbook(workbook, "xypos", XLSExportItems.XYCENTER, row0, charSeries);
				if (options.distance) 
					rowmax = xlsExportToWorkbook(workbook, "distance", XLSExportItems.DISTANCE, row0, charSeries);
				if (options.alive) 
					rowmax = xlsExportToWorkbook(workbook, "alive", XLSExportItems.ISALIVE, row0, charSeries);
				
				row0 = rowmax;
				iSeries++;
			}
			
			if (options.transpose && options.pivot) 
				xlsCreatePivotTables(workbook, "alive", rowmax);
			
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private static ArrayList <ArrayList<Double>> getDataFromCages(XLSExportItems option) {
		ArrayList <ArrayList<Double >> arrayList = new ArrayList <ArrayList <Double>> ();
		
		for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
			switch (option) {
			case DISTANCE: 
				arrayList.add(posxyt.getDoubleArrayList(ArrayListType.distance));
				break;
			case ISALIVE:
				arrayList.add(posxyt.getDoubleArrayList(ArrayListType.isalive));
				// TODO add threshold to cleanup data
				break;
			case XYCENTER:
			default:
				arrayList.add(posxyt.getDoubleArrayList(ArrayListType.xyPosition));
				break;
			}
		}
		return arrayList;
	}

	public static int xlsExportToWorkbook(XSSFWorkbook workBook, String title, XLSExportItems option, int row0, String charSeries) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Double >> arrayList = getDataFromCages(option);

		Sheet sheet = workBook.getSheet(title );
		boolean flag = (sheet == null);
		if (flag)
			sheet = workBook.createSheet(title);
		
		Point pt = writeGlobalInfos(sheet, row0, option, options.transpose, charSeries);
		pt = writeColumnHeaders(sheet, pt, option, options.transpose, charSeries);
		pt = writeData(sheet, pt, option, arrayList, options.transpose, charSeries);
		return pt.y;
	}
	
	private static Point writeGlobalInfos(Sheet sheet, int row0, XLSExportItems option, boolean transpose, String charSeries) {
		Point pt = new Point(0, row0);

		XLSUtils.setValue(sheet, pt, transpose, "expt"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "name");
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, path);
		pt.y++;
		
		pt.x=0;
		Point pt1 = pt;
		XLSUtils.setValue(sheet, pt, transpose, "n_cages"+charSeries);
		pt1.x++;
		XLSUtils.setValue(sheet, pt, transpose, vSequence.cages.flyPositionsList.size());
		switch (option) {
		case DISTANCE:
			break;
		case ISALIVE:
			pt1.x++;
			XLSUtils.setValue(sheet, pt, transpose, "threshold");
			pt1.x++;
			XLSUtils.setValue(sheet, pt, transpose, vSequence.cages.detect.threshold);
			break;
		case XYCENTER:
		default:
			break;
		}

		pt.y++;
		pt.x=0;
		return pt;
	}

	public static Point addLine(Sheet sheet, Point pt, boolean transpose, XLSExperimentDescriptors desc) {
		XLSUtils.setValue(sheet, pt, transpose, desc.toString());
		pt.x++;
		pt.x++;
		switch (desc) {
		case CAGE: 	// assume 2 capillaries/slot
			for (int i= 0; i < vSequence.cages.flyPositionsList.size()*2; i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, i/2);
			break;
		case NFLIES: // assume 2 capillaries/slot
			for (int i= 0; i < vSequence.cages.flyPositionsList.size()*2; i++, pt.x++) {
				int j = 1;
				if (i < 2 || i > 17)
					j = 0;
				XLSUtils.setValue(sheet, pt, transpose, j);
			}
			break;
		case CAP:
			for (int i= 0; i < vSequence.cages.flyPositionsList.size()*2; i++, pt.x++) {
				boolean isEven = (i % 2) == 0;
				if (isEven)
					XLSUtils.setValue(sheet, pt, transpose, "L");
				else
					XLSUtils.setValue(sheet, pt, transpose, "R");
			}
			break;
		default:
			break;
		}
		pt.x = 0;
		pt.y++;
		return pt;
	}
	
	private static Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose, String charSeries) {
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
		if (vSequence.isFileStack()) {
			XLSUtils.setValue(sheet, pt, transpose, "filename");
			pt.x++;
		}
		
		switch (option) {
		case DISTANCE:
			for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
			}
			break;
			
		case ISALIVE:
			for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, name0);
				pt.x++;
			}
			break;
		case XYCENTER:
		default:
			for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet, pt, transpose, name0+".x");
				pt.x++;
				XLSUtils.setValue(sheet, pt, transpose, name0+".y");
				pt.x++;
			}
			break;
		}
		pt.x=0;
		pt.y++;
		return pt;
	}

	private static Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Double >> arrayList, boolean transpose, String charSeries) {
	
		if (charSeries == null)
			charSeries = "t";
		
		ArrayList<XYTaSeries> flyPositionsList = vSequence.cages.flyPositionsList; 
		int n_time_intervals = flyPositionsList.get(0).pointsList.size();
		int n_series = flyPositionsList.size();
		
		for (int t=0; t< n_time_intervals; t++) {
			Point pt2 = new Point(0, pt.y); 
			int time_absolute = flyPositionsList.get(0).pointsList.get(t).time;
			XLSUtils.setValue(sheet, pt2, transpose, charSeries+time_absolute);

			pt2.x++;if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(time_absolute);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet, pt2, transpose, fileName);
				pt2.x++;
			}
						
			switch (option) {
			case DISTANCE:
				for (int i=0; i < n_series; i++ ) 
				{
					XLSUtils.setValue(sheet, pt2, transpose, arrayList.get(i).get(t));
					pt2.x++;
				}
				break;
			case ISALIVE:
				for (int i=0; i < n_series; i++ ) 
				{
					XLSUtils.setValue(sheet, pt2, transpose, arrayList.get(i).get(t));
					pt2.x++;
					XLSUtils.setValue(sheet, pt2, transpose, arrayList.get(i).get(t));
					pt2.x++;
				}
				break;

			case XYCENTER:
			default:
				for (int i=0; i < n_series; i++ ) 
				{
					int iarray = t*2;
					XLSUtils.setValue(sheet, pt2, transpose, arrayList.get(i).get(iarray));
					pt2.x++;
					XLSUtils.setValue(sheet, pt2, transpose, arrayList.get(i).get(iarray+1));
					pt2.x++;
				}
				break;
			}
			pt.y++;
		}
		return pt;
	}

	public static void xlsCreatePivotTables(XSSFWorkbook workBook, String fromWorkbook, int rowmax) {
        
		xlsCreatePivotTable(workBook, "pivot_sum", fromWorkbook, rowmax, DataConsolidateFunction.SUM);
	}
	
	public static void xlsCreatePivotTable(XSSFWorkbook workBook, String workBookName, String fromWorkbook, int rowmax, DataConsolidateFunction function) {
        XSSFSheet pivotSheet = workBook.createSheet(workBookName);
        XSSFSheet sourceSheet = workBook.getSheet(fromWorkbook);

        int ncolumns = rowmax;
        CellAddress lastcell = new CellAddress (21, ncolumns-1);
        String address = "A1:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(1, 1);
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(source, position, sourceSheet);

        boolean flag = false;
        for (int i = 0; i< ncolumns; i++) {
        	Cell cell = XLSUtils.getCell(sourceSheet, 0, i);
        	String text = cell.getStringCellValue();
        	if(!flag) {
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
