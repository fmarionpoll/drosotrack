package plugins.fmp.fmpTools;

import java.awt.Point;


import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

public class XLSUtils {

	public static void setValue (XSSFSheet sheet, Point pt, boolean transpose, int ivalue) {
		getCell(sheet, pt, transpose).setCellValue(ivalue);
	}
	
	public static void setValue (XSSFSheet sheet, Point pt, boolean transpose, String string) {
		getCell(sheet, pt, transpose).setCellValue(string);
	}
	
	public static void setValue (XSSFSheet sheet, Point pt, boolean transpose, double value) {
		getCell(sheet, pt, transpose).setCellValue(value);
	}
		
	public static double getValueDouble (XSSFSheet sheet, Point pt, boolean transpose) {
		return getCell(sheet, pt, transpose).getNumericCellValue();
	}
	
	public static XSSFCell getCell (XSSFSheet sheet, int rownum, int colnum) {
		XSSFRow row = getSheetRow(sheet, rownum);
		XSSFCell cell = getRowCell (row, colnum);
		return cell;
	}
	
	public static XSSFRow getSheetRow (XSSFSheet sheet, int rownum) {
		XSSFRow row = sheet.getRow(rownum);
		if (row == null)
			row = sheet.createRow(rownum);
		return row;
	}
	
	public static XSSFCell getRowCell (XSSFRow row, int cellnum) {
		XSSFCell cell = row.getCell(cellnum);
		if (cell == null)
			cell = row.createCell(cellnum);
		return cell;
	}
	
	public static XSSFCell getCell (XSSFSheet sheet, Point point, boolean transpose) {
		Point pt = new Point(point);
		if (transpose) {
			int dummy = pt.x;
			pt.x = pt.y;
			pt.y = dummy;
		}
		return getCell (sheet, pt.y, pt.x);
	}

}
