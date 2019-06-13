package plugins.fmp.tools;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class XLSUtils {

	public static void setValue (Sheet sheet, int column, int row, int ivalue, boolean transpose) {
		if (transpose) {
			int dummy = row;
			row = column;
			column = dummy;
		}
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(ivalue);
	}
	
	public static void setValue (Sheet sheet, int column, int row, String string, boolean transpose) {
		if (transpose) {
			int dummy = row;
			row = column;
			column = dummy;
		}
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(string);
	}
	
	public static void setValue (Sheet sheet, int column, int row, double value, boolean transpose) {
		if (transpose) {
			int dummy = row;
			row = column;
			column = dummy;
		}
		Cell cell = getCell (sheet, row, column);
		cell.setCellValue(value);
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
	

}
