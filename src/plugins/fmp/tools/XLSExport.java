package plugins.fmp.tools;

import java.awt.Point;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class XLSExport {

	protected XLSExportOptions 	options 	= null;
	protected Experiment 		expAll 		= null;
	int							nintervals	= 0;
	List <XLSNameAndPosition> 	listOfStacks; 

	public long getnearest(long value, int step) {
		long diff0 = (value /step)*step;
		long diff1 = diff0 + step;
		if ((value - diff0 ) < (diff1 - value))
			value = diff0;
		else
			value = diff1;
		return value;
	}
	
	private void addDescriptorTitlestoExperimentDescriptors(XSSFSheet sheet, Point pt, boolean transpose) {
		
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.DATE.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.BOXID.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.EXPMT.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.COMMENT.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.STIM.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.CONC.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.CAM.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.CAP.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.CAGE.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.NFLIES.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.DUM1.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.DUM2.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.DUM3.toString());
		pt.y++;
		XLSUtils.setValue(sheet, pt, transpose, EnumXLSExperimentDescriptors.DUM4.toString());
		pt.y++;
	}
	
	protected Point addExperimentDescriptorsToHeader(Experiment exp, XSSFSheet sheet, Point pt, boolean transpose) {
		int col0 = pt.x;
		int row = pt.y;
		if (col0 == 0)
			addDescriptorTitlestoExperimentDescriptors(sheet, pt, transpose);
		pt.x++;
		pt.x++;
		pt.x++;
		int colseries = pt.x;
		
		for (SequencePlus seq: exp.kymographArrayList) { 

			String name = seq.getName();
			int col = getColFromKymoSequenceName(name);
			if (col >= 0) 
				pt.x = colseries + col;
			pt.y = row;
			Path path = Paths.get(exp.vSequence.getFileName());
		
			// date
			SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
			XLSUtils.setValue(sheet, pt, transpose, df.format(exp.fileTimeImageFirst.toMillis()));
			pt.y++;
			
			XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.boxID);
			pt.y++;
			XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.experiment);
			pt.y++;
			XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.comment);
			pt.y++;

			// stimulus, conc
			String letter = name.substring(name.length() - 1);
			if (letter .equals("L")) 	XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.stimulusL);
			else						XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.stimulusR);
			pt.y++;
			if (letter .equals("L")) 	XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.concentrationL);
			else 						XLSUtils.setValue(sheet, pt, transpose, exp.vSequence.capillaries.concentrationR);
			pt.y++;
			
			// cam
			String cam = getSubName(path, 2).substring(0, 5); 
			XLSUtils.setValue(sheet, pt, transpose, cam);
			pt.y++;
			// cap
			XLSUtils.setValue(sheet, pt, transpose, letter);
			pt.y++;
			// cage
			int i = getCageFromCapillaryName(name);
			XLSUtils.setValue(sheet, pt, transpose, i);
			pt.y++;
			// nflies
			int j = 1;
			if (i < 1 || i > 8)
				j = 0;
			XLSUtils.setValue(sheet, pt, transpose, j);
			pt.y++;
			// dum1
			String name1 = getSubName(path, 2); 
			XLSUtils.setValue(sheet, pt, transpose, name1);
			pt.y++;
			// dum2
			String name11 = getSubName(path, 3); 
			XLSUtils.setValue(sheet, pt, transpose, name11);
			pt.y++;
			// dum3
			String name111 = getSubName(path, 4); 
			XLSUtils.setValue(sheet, pt, transpose, name111);
			pt.y++;
			// dum4
			XLSUtils.setValue(sheet, pt, transpose, sheet.getSheetName());
			pt.y++;
		}
		pt.x = col0;
		return pt;
	}
	
	protected int getCageFromCapillaryName(String name) {
		if (!name .contains("line"))
			return -1;
	
		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		return numFromName;
	}
	
	protected String getSubName(Path path, int subnameIndex) {
		String name = "-";
		if (path.getNameCount() >= subnameIndex)
			name = path.getName(path.getNameCount() -subnameIndex).toString();
		return name;
	}
	
	protected String getShortenedName(SequenceVirtual seq, int t) {
		String cs = seq.getFileName(t);
		return cs.substring(cs.lastIndexOf("\\") + 1) ;
	}

	protected void xlsCreatePivotTable(XSSFWorkbook workBook, String workBookName, String fromWorkbook, DataConsolidateFunction function) {

		XSSFSheet pivotSheet = workBook.createSheet(workBookName);
        XSSFSheet sourceSheet = workBook.getSheet(fromWorkbook);

        int lastRowNum = sourceSheet.getLastRowNum();
        int lastColumnNum = sourceSheet.getRow(0).getLastCellNum();
        CellAddress lastcell = new CellAddress (lastRowNum, lastColumnNum-1);
        String address = "A1:"+lastcell.toString();
        AreaReference source = new AreaReference(address, SpreadsheetVersion.EXCEL2007);
        CellReference position = new CellReference(0, 0);
        XSSFPivotTable pivotTable = pivotSheet.createPivotTable(source, position, sourceSheet);

        boolean flag = false;	// ugly trick: switch mode when flag = true, ie when column "roi" has been found
        for (int i = 0; i< lastColumnNum; i++) {
        	XSSFCell cell = XLSUtils.getCell(sourceSheet, 0, i);
        	String text = cell.getStringCellValue();
        	if( !flag) {
        		flag = text.contains("roi");  // ugly trick here
        		if (text.contains(EnumXLSExperimentDescriptors.CAP.toString()))
        			pivotTable.addColLabel(i);
        		if (text.contains(EnumXLSExperimentDescriptors.NFLIES.toString()))
        			pivotTable.addRowLabel(i);
        		continue;
        	}
        	pivotTable.addColumnLabel(function, i, text);
        }
	}

	protected Point writeGenericHeader (Experiment exp, XSSFSheet sheet, EnumXLSExportItems option, Point pt, boolean transpose, String charSeries) {

		pt = addExperimentDescriptorsToHeader(exp, sheet, pt, transpose);
	
		XLSUtils.setValue(sheet, pt, transpose, "rois"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "timeMin"+charSeries);
		pt.x++;
		XLSUtils.setValue(sheet, pt, transpose, "filename" );
		pt.x++;

		return pt;
	}

	protected void xlsCreatePivotTables(XSSFWorkbook workBook, String fromWorkbook) {
        
		xlsCreatePivotTable(workBook, "pivot_avg", fromWorkbook, DataConsolidateFunction.AVERAGE);
		xlsCreatePivotTable(workBook, "pivot_std", fromWorkbook, DataConsolidateFunction.STD_DEV);
		xlsCreatePivotTable(workBook, "pivot_n", fromWorkbook, DataConsolidateFunction.COUNT);
	}
	protected String getBoxIdentificator (Experiment exp) {
		Path path = Paths.get(exp.vSequence.getFileName());
		String name = getSubName(path, 2); 
		return name;
	}

	protected Point getStackColumnPosition (Experiment exp, Point pt) {
		Point localPt = pt;
		String name = getBoxIdentificator (exp);
		boolean found = false;
		for (XLSNameAndPosition desc: listOfStacks) {
			if (name .equals(desc.name)) {
				found = true;
				localPt = new Point(desc.column, desc.row);
				if (desc.fileTimeImageLastMinutes < exp.fileTimeImageLastMinutes) {
					desc.fileTimeImageLastMinutes = exp.fileTimeImageLastMinutes;
					desc.fileTimeImageLast = exp.fileTimeImageLast;
				}
				if (desc.fileTimeImageFirstMinutes 	> exp.fileTimeImageFirstMinutes) {
					desc.fileTimeImageFirstMinutes 	= exp.fileTimeImageFirstMinutes;
					desc.fileTimeImageFirst = exp.fileTimeImageFirst;
				}
				long filespan = desc.fileTimeImageLastMinutes - desc.fileTimeImageFirstMinutes;
				if (filespan > desc.fileTimeSpan)
					desc.fileTimeSpan = filespan;
				break;
			}
		}
		
		if (!found) {
			XLSNameAndPosition desc = new XLSNameAndPosition(name, pt);
			desc.fileTimeImageFirstMinutes 	= exp.fileTimeImageFirstMinutes;
			desc.fileTimeImageFirst = exp.fileTimeImageFirst;
			desc.fileTimeSpan  = desc.fileTimeImageLastMinutes - desc.fileTimeImageFirstMinutes;
			listOfStacks.add(desc);
		}
		return localPt;
	}
	
	protected XLSNameAndPosition getStackGlobalSeriesDescriptor (Experiment exp) {
		String name = getBoxIdentificator (exp);
		for (XLSNameAndPosition desc: listOfStacks) {
			if (name .equals(desc.name)) {
				return desc;				
			}
		}
		return null;
	}
	
	protected int getColFromKymoSequenceName(String name) {
		if (!name .contains("line"))
			return -1;

		String num = name.substring(4, 5);
		int numFromName = Integer.parseInt(num);
		String side = name.substring(5, 6);
		if (side != null) {
			if (side .equals("R")) {
				numFromName = numFromName* 2;
				numFromName += 1;
			}
			else if (side .equals("L"))
				numFromName = numFromName* 2;
		}
		return numFromName;
	}
	
}
