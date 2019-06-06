package plugins.fmp.tools;

import java.awt.Point;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class XLSUtils {
	public static Point nextRow (Point pt, boolean transpose) {
		if (!transpose)
			pt.y ++;
		else
			pt.x++;
		return pt;
	}
	public static Point nextCol (Point pt, boolean transpose) {
		if (!transpose) 
			pt.x ++;
		else 
			pt.y++;
		return pt;
	}	
	
	public static Point toColZero (Point pt, boolean transpose) {
		if (!transpose) 
			pt.x = 0;
		else
			pt.y = 0;
		return pt;
	}

	public static void setValue (Sheet sheet, int column, int row, int ivalue) {
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(ivalue);
	}
	public static void setValue (Sheet sheet, int column, int row, String string) {
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(string);
	}
	
	public static void setValue (Sheet sheet, int column, int row, double value) {
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(value);
	}
	
	public static Cell getCell (Sheet sheet, int rownum, int colnum) {
		Row row = getRow(sheet, rownum);
		Cell cell = getCol (row, colnum);
		return cell;
	}
	
	public static Row getRow (Sheet sheet, int rownum) {
		Row row = sheet.getRow(rownum);
		if (row == null)
			row = sheet.createRow(rownum);
		return row;
	}
	
	public static Cell getCol (Row row, int cellnum) {
		Cell cell = row.getCell(cellnum);
		if (cell == null)
			cell = row.createCell(cellnum);
		return cell;
	}
}
