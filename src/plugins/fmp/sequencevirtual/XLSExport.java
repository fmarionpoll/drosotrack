package plugins.fmp.sequencevirtual;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.tools.XLSExperimentDescriptors;
import plugins.fmp.tools.XLSExportItems;
import plugins.fmp.tools.XLSExportOptions;
import plugins.fmp.tools.XLSUtils;

public class XLSExport {

	protected static XLSExportOptions options = null;
	protected static Experiment 		expAll = null;
	static int							nintervals	= 0;

	public static long getnearest(long value, int step) {
		long diff0 = (value /step)*step;
		long diff1 = diff0 + step;
		if ((value - diff0 ) < (diff1 - value))
			value = diff0;
		else
			value = diff1;
		return value;
	}
	
	public static Point addLineToHeader(Experiment exp, XSSFSheet sheet, Point pt, boolean transpose, XLSExperimentDescriptors desc) {
		int col0 = pt.x;
		XLSUtils.setValue(sheet, pt, transpose, desc.toString());
		pt.x++;
		pt.x++;
		pt.x++;
		switch (desc) {
		case CAGE: 	// assume 2 capillaries/slot
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, i/2);
			break;
		case NFLIES: // assume first 2 and last 2 have no flies
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) {
				int j = 1;
				if (i < 2 || i > 17)
					j = 0;
				XLSUtils.setValue(sheet, pt, transpose, j);
			}
			break;
		case CAP:
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) {
				String name = exp.kymographArrayList.get(i).getName();
				String letter = name.substring(name.length() - 1);
				XLSUtils.setValue(sheet, pt, transpose, letter);
			}
			break;
		case DUM1: {
			Path path = Paths.get(exp.vSequence.getFileName());
			String name = path.getName(path.getNameCount() -2).toString();
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++)
				XLSUtils.setValue(sheet, pt, transpose, name);
			}
			break;
		case DUM2:	{
			Path path = Paths.get(exp.vSequence.getFileName());
			String name = path.getName(path.getNameCount() -3).toString();
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++)
				XLSUtils.setValue(sheet, pt, transpose, name);
			}
			break;
		case DUM3:	{
			Path path = Paths.get(exp.vSequence.getFileName());
			String name = path.getName(path.getNameCount() -4).toString();
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++)
				XLSUtils.setValue(sheet, pt, transpose, name);
			}
			break;
		case DUM4: 
			for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, sheet.getSheetName());
			break;
		case DATE:
			SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
	       for (int i= 0; i < exp.kymographArrayList.size(); i++, pt.x++) 
				XLSUtils.setValue(sheet, pt, transpose, df.format(exp.fileTimeImageFirst.toMillis()));
			break;
		default:
			break;
		}
		pt.x = col0;
		pt.y++;
		return pt;
	}
	
	public static String getShortenedName(SequenceVirtual seq, int t) {
		String cs = seq.getFileName(t);
		return cs.substring(cs.lastIndexOf("\\") + 1) ;
	}

	public static void xlsCreatePivotTable(XSSFWorkbook workBook, String workBookName, String fromWorkbook, DataConsolidateFunction function) {

		XSSFSheet pivotSheet = workBook.createSheet(workBookName);
        XSSFSheet sourceSheet = workBook.getSheet(fromWorkbook);

        int lastRowNum = sourceSheet.getLastRowNum();
        int lastColumnNum = sourceSheet.getRow(0).getLastCellNum();
        CellAddress lastcell = new CellAddress (lastRowNum, lastColumnNum-1);
        String address = "A1:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(0, 0);
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(source, position, sourceSheet);

        boolean flag = false;
        for (int i = 0; i< lastColumnNum; i++) {
        	XSSFCell cell = XLSUtils.getCell(sourceSheet, 0, i);
        	String text = cell.getStringCellValue();
        	if( !flag) {
        		flag = text.contains("roi");
        		if (text.contains(XLSExperimentDescriptors.CAP.toString()))
        			pivotTable.addRowLabel(i);
        		if (text.contains(XLSExperimentDescriptors.NFLIES.toString()))
        			pivotTable.addRowLabel(i);
        		continue;
        	}
        	pivotTable.addColumnLabel(function, i, text);
        }
	}

	public static Point writeGenericHeader (Experiment exp, XSSFSheet sheet, XLSExportItems option, Point pt, boolean transpose, String charSeries) {

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
		XLSUtils.setValue(sheet, pt, transpose, "timeMin"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "filename" );
		pt.x++;

		return pt;
	}

	public static void xlsCreatePivotTables(XSSFWorkbook workBook, String fromWorkbook) {
        
		xlsCreatePivotTable(workBook, "pivot_avg", fromWorkbook, DataConsolidateFunction.AVERAGE);
		xlsCreatePivotTable(workBook, "pivot_std", fromWorkbook, DataConsolidateFunction.STD_DEV);
		xlsCreatePivotTable(workBook, "pivot_n", fromWorkbook, DataConsolidateFunction.COUNT);
	}
}
