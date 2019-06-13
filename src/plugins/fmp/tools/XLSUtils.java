package plugins.fmp.tools;

import java.awt.Point;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class XLSUtils {

	public static void setValue (Sheet sheet, Point pt, boolean transpose, int ivalue) {
		getCell(sheet, pt, transpose).setCellValue(ivalue);
	}
	
	public static void setValue (Sheet sheet, Point pt, boolean transpose, String string) {
		getCell(sheet, pt, transpose).setCellValue(string);
	}
	
	public static void setValue (Sheet sheet, Point pt, boolean transpose, double value) {
		getCell(sheet, pt, transpose).setCellValue(value);
	}
	
	public static Cell getCell (Sheet sheet, int rownum, int colnum) {
		Row row = getSheetRow(sheet, rownum);
		Cell cell = getRowCell (row, colnum);
		return cell;
	}
	
	public static Row getSheetRow (Sheet sheet, int rownum) {
		Row row = sheet.getRow(rownum);
		if (row == null)
			row = sheet.createRow(rownum);
		return row;
	}
	
	public static Cell getRowCell (Row row, int cellnum) {
		Cell cell = row.getCell(cellnum);
		if (cell == null)
			cell = row.createCell(cellnum);
		return cell;
	}
	
	public static Cell getCell (Sheet sheet, Point point, boolean transpose) {
		Point pt = new Point(point);
		if (transpose) {
			int dummy = pt.x;
			pt.x = pt.y;
			pt.y = dummy;
		}
		return getCell (sheet, pt.y, pt.x);
	}

}
