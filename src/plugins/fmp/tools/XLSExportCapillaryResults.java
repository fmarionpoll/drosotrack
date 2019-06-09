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

import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequenceVirtual;

public class XLSExportCapillaryResults {

	static SequenceVirtual 					vSequence = null;
	static XLSExportCapillariesOptions options = null;
	static ArrayList<SequencePlus> 		kymographArrayList = null;
	
	public static void exportToFile(String filename, XLSExportCapillariesOptions opt, 
			SequenceVirtual vSeq,ArrayList<SequencePlus> kymographsArray) {
		
		System.out.println("XLS output");
		vSequence = vSeq;
		options = opt;
		kymographArrayList = kymographsArray;
		
		try { 
			Workbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

			if (options.topLevel) 
				xlsExportToWorkbook(workbook, "toplevel", XLSExportItems.TOPLEVEL);
			if (options.bottomLevel) 
				xlsExportToWorkbook(workbook, "bottomlevel", XLSExportItems.BOTTOMLEVEL);
			if (options.derivative) 
				xlsExportToWorkbook(workbook, "derivative", XLSExportItems.DERIVEDVALUES);
			if (options.consumption) 
				xlsExportToWorkbook(workbook, "sumGulps", XLSExportItems.SUMGULPS);
			if (options.sum) 
				xlsExportToWorkbook(workbook, "sumL+R", XLSExportItems.SUMLR);
			
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private static ArrayList <ArrayList<Integer >> getDataFromRois(XLSExportItems option, boolean relative) {
		ArrayList <ArrayList<Integer >> arrayList = new ArrayList <ArrayList <Integer>> ();
		for (SequencePlus seq: kymographArrayList) {
			switch (option) {
			case DERIVEDVALUES:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.derivedValues));
				break;
			case SUMGULPS: 
				arrayList.add(seq.getArrayListFromRois(ArrayListType.cumSum));
				break;
			case BOTTOMLEVEL:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.bottomLevel));
				break;
			case TOPLEVEL:
			case SUMLR:
			default:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.topLevel));
				break;
			}
		}
		
		if (relative) {
			for (ArrayList<Integer> array : arrayList) {
				int item0 = array.get(0);
				int i=0;
				for (int item: array) {
					array.set(i, item-item0);
					i++;
				}
			}
		}
		return arrayList;
	}
	
	private static void xlsExportToWorkbook(Workbook workBook, String title, XLSExportItems option) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(option, options.t0);		
		if (arrayList.size() == 0)
			return;

		Sheet sheet = workBook.createSheet(title );
		Point pt = writeGlobalInfos(sheet, options.transpose);
		pt = writeColumnHeaders(sheet, pt, option, options.transpose);
		pt = writeData(sheet, pt, option, arrayList, options.transpose);
	}
	
	private static Point writeGlobalInfos(Sheet sheet, boolean transpose) {
		Point pt = new Point(0, 0);

		XLSUtils.setValue(sheet,  pt.x, pt.y, "name:" );
		File file = new File(vSequence.getFileName(0));
		String path = file.getParent();
		pt = XLSUtils.nextCol(pt, transpose);
		XLSUtils.setValue(sheet,  pt.x, pt.y, path );
		pt= XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.toColZero(pt, transpose);
		Point pt1 = pt;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (µl):" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.capillaries.capillaryVolume);
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "capillary (pixels):" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, vSequence.capillaries.capillaryPixels);
		pt = XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private static Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose) {
		pt = XLSUtils.toColZero(pt, transpose);
		if (vSequence.isFileStack()) {
			XLSUtils.setValue(sheet,  pt.x, pt.y, "filename" );
			pt = XLSUtils.nextCol(pt, transpose);
		}
		XLSUtils.setValue(sheet,  pt.x, pt.y, "i" );
		pt = XLSUtils.nextCol(pt, transpose);
		
		switch (option) {
		case SUMLR:
			for (int i=0; i< kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+"+"+name1 );
				pt = XLSUtils.nextCol(pt, transpose);
				XLSUtils.setValue(sheet,  pt.x, pt.y, "." );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		default:
			for (int i=0; i< kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = kymographArrayList.get(i);
				String name = kymographSeq.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		}
		pt = XLSUtils.toColZero(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private static Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose) {
		int maxelements = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > maxelements)
				maxelements = datai.size();
		}
		int nelements = maxelements-1;
		if (nelements <= 0)
			return pt;
		
		double ratio = vSequence.capillaries.capillaryVolume / vSequence.capillaries.capillaryPixels;
		
		int startFrame = (int) vSequence.analysisStart;
		int t = startFrame;
		// TODO check if name of files is correct
		for (int j=0; j< nelements; j++) {
			Point pt2 = XLSUtils.toColZero(pt, transpose);
			if (vSequence.isFileStack()) {
				String cs = vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet,  pt2.x, pt2.y, fileName );
				pt2 = XLSUtils.nextCol(pt2, transpose);
			}

			XLSUtils.setValue(sheet,  pt2.x, pt2.y, t);
			t  += vSequence.analysisStep;
			pt2 = XLSUtils.nextCol(pt2, transpose);
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< kymographArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = arrayList.get(i);
					ArrayList<Integer> dataR = arrayList.get(i+1);
					if (j < dataL.size())
						XLSUtils.setValue(sheet,  pt2.x, pt2.y, (dataL.get(j)+dataR.get(j))*ratio );
					pt2 = XLSUtils.nextCol(pt2, transpose);
					pt2 = XLSUtils.nextCol(pt2, transpose);
				}
				break;

			default:
				for (int i=0; i< kymographArrayList.size(); i++) 
				{
					ArrayList<Integer> data = arrayList.get(i);
					if (j < data.size())
						XLSUtils.setValue(sheet, pt2.x, pt2.y, data.get(j)*ratio );
					pt2 = XLSUtils.nextCol (pt2, transpose);
				}
				break;
			}
			
			pt = XLSUtils.nextRow (pt, transpose);
		}
		return pt;
	}
}
