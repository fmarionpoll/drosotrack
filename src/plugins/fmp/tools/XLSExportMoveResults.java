package plugins.fmp.tools;

import java.awt.Point;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.SequenceVirtual;
import plugins.fmp.sequencevirtual.XYTaSeries;

public class XLSExportMoveResults {

	static XLSExportMoveOptions options = null;
	static SequenceVirtual vSequence = null;
	
	public static void exportToFile(String filename, XLSExportMoveOptions opt, SequenceVirtual seq) {
		System.out.println("XLS output");
		options = opt;
		vSequence = seq;
		
		try { 
			Workbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			
			if (options.xyCenter) 
				xlsExportToWorkbook(workbook, "xypos", XLSExportItems.XYCENTER);
			if (options.distance) 
				xlsExportToWorkbook(workbook, "distance", XLSExportItems.DISTANCE);
			if (options.alive) 
				xlsExportToWorkbook(workbook, "alive", XLSExportItems.ISALIVE);
			
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

	private static void xlsExportToWorkbook(Workbook workBook, String title, XLSExportItems option) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Double >> arrayList = getDataFromCages(option);		
		if (arrayList.size() == 0)
			return;

		Sheet sheet = workBook.createSheet(title );
		Point pt = writeGlobalInfos(sheet, option, options.transpose);
		pt = writeColumnHeaders(sheet, pt, option, options.transpose);
		pt = writeData(sheet, pt, option, arrayList, options.transpose);
	}
	
	private static Point writeGlobalInfos(Sheet sheet, XLSExportItems option, boolean transpose) {
		Point pt = new Point(0, 0);

		XLSUtils.setValue(sheet,  pt.x, pt.y, "name:" );
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		pt = XLSUtils.nextCol(pt, transpose);
		XLSUtils.setValue(sheet,  pt.x, pt.y, path );
		pt= XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.toColZero(pt, transpose, 0);
		Point pt1 = pt;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "n cages" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.cages.flyPositionsList.size());
		
		switch (option) {
		case DISTANCE:
			break;
		case ISALIVE:
			pt1 = XLSUtils.nextCol(pt1, transpose);
			XLSUtils.setValue(sheet,  pt1.x, pt1.y, "threshold" );
			pt1 = XLSUtils.nextCol(pt1, transpose);
			XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.cages.detect.threshold);
			break;
		case XYCENTER:
		default:
			break;
		}

		pt = XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private static Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose) {
		pt = XLSUtils.toColZero(pt, transpose, 0);
		if (vSequence.isFileStack()) {
			XLSUtils.setValue(sheet,  pt.x, pt.y, "filename" );
			pt = XLSUtils.nextCol(pt, transpose);
		}
		XLSUtils.setValue(sheet,  pt.x, pt.y, "i" );
		pt = XLSUtils.nextCol(pt, transpose);
		
		switch (option) {
		case DISTANCE:
			for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0 );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
			
		case ISALIVE:
			for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0 );
				pt = XLSUtils.nextCol(pt, transpose);
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0 );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		case XYCENTER:
		default:
			for (XYTaSeries posxyt: vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+".x" );
				pt = XLSUtils.nextCol(pt, transpose);
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+".y" );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		}
		pt = XLSUtils.toColZero(pt, transpose, 0);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private static Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Double >> arrayList, boolean transpose) {
	
		ArrayList<XYTaSeries> flyPositionsList = vSequence.cages.flyPositionsList; 
		int n_time_intervals = flyPositionsList.get(0).pointsList.size();
		int n_series = flyPositionsList.size();
		
		for (int time_interval=0; time_interval< n_time_intervals; time_interval++) {
			int time_absolute = flyPositionsList.get(0).pointsList.get(time_interval).time;
			Point pt2 = XLSUtils.toColZero(pt, transpose, 0);
			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(time_absolute);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet,  pt2.x, pt2.y, fileName );
				pt2 = XLSUtils.nextCol(pt2, transpose);
			}
			XLSUtils.setValue(sheet,  pt2.x, pt2.y, time_absolute);
			time_absolute  += vSequence.analysisStep;
			pt2 = XLSUtils.nextCol(pt2, transpose);
			
			switch (option) {
			case DISTANCE:
				for (int i=0; i < n_series; i++ ) 
				{
					XLSUtils.setValue(sheet,  pt2.x, pt2.y, arrayList.get(i).get(time_interval) );
					pt2 = XLSUtils.nextCol(pt2, transpose);
				}
				break;
			case ISALIVE:
				for (int i=0; i < n_series; i++ ) 
				{
					XLSUtils.setValue(sheet,  pt2.x, pt2.y, arrayList.get(i).get(time_interval) );
					pt2 = XLSUtils.nextCol(pt2, transpose);
					XLSUtils.setValue(sheet,  pt2.x, pt2.y, arrayList.get(i).get(time_interval) );
					pt2 = XLSUtils.nextCol(pt2, transpose);
				}
				break;

			case XYCENTER:
			default:
				for (int i=0; i < n_series; i++ ) 
				{
					int iarray = time_interval*2;
					XLSUtils.setValue(sheet,  pt2.x, pt2.y, arrayList.get(i).get(iarray) );
					pt2 = XLSUtils.nextCol(pt2, transpose);
					XLSUtils.setValue(sheet,  pt2.x, pt2.y, arrayList.get(i).get(iarray+1) );
					pt2 = XLSUtils.nextCol(pt2, transpose);
				}
				break;
			}
			pt = XLSUtils.nextRow (pt, transpose);
		}
		return pt;
	}
}
