package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import icy.gui.util.GuiUtil;
import plugins.fmp.sequencevirtual.XYTaSeries;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.tools.XLSExportItems;
import plugins.fmp.tools.XLSUtils;
import plugins.fmp.tools.Tools;

public class MoveTab_Excel  extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JCheckBox 	xyCenterCheckBox 	= new JCheckBox("XY position", true);
	public JCheckBox 	distanceCheckBox = new JCheckBox("distance", false);
	public JCheckBox 	aliveCheckBox = new JCheckBox("alive or not", true);
	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", false);

	private Multicafe parent0 = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( xyCenterCheckBox, distanceCheckBox, aliveCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel( transposeCheckBox, new JLabel(" "), new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	public void enableItems(boolean enabled) {
		exportToXLSButton.setEnabled(enabled);
		transposeCheckBox.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_move.xlsx";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				exportToXLSButton.setEnabled( false);
				parent0.capillariesPane.propertiesTab.updateSequenceFromDialog();
				xlsExportResultsToFile(filename);

				firePropertyChange("EXPORT_TO_EXCEL", false, true);	
				exportToXLSButton.setEnabled( true );
			}
		}
	}

	private void xlsExportResultsToFile(String filename) {
		System.out.println("XLS output");
		
		try { 
			Workbook workbook = new XSSFWorkbook(); 
			workbook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
			
			if (xyCenterCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "xypos", XLSExportItems.XYCENTER);
			if (distanceCheckBox.isSelected()) 
				xlsExportToWorkbook(workbook, "distance", XLSExportItems.DISTANCE);
			if (aliveCheckBox.isSelected()) 
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

	private ArrayList <ArrayList<Double>> getDataFromCages(XLSExportItems option) {
		ArrayList <ArrayList<Double >> arrayList = new ArrayList <ArrayList <Double>> ();
		for (XYTaSeries posxyt: parent0.vSequence.cages.flyPositionsList) {
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

	//-----------------------------------------------------------------------------------
	private void xlsExportToWorkbook(Workbook workBook, String title, XLSExportItems option) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Double >> arrayList = getDataFromCages(option);		
		if (arrayList.size() == 0)
			return;

		Sheet sheet = workBook.createSheet(title );
		boolean transpose = transposeCheckBox.isSelected(); 
		Point pt = writeGlobalInfos(sheet, option, transpose);
		pt = writeColumnHeaders(sheet, pt, option, transpose);
		pt = writeData(sheet, pt, option, arrayList, transpose);
	}
	
	private Point writeGlobalInfos(Sheet sheet, XLSExportItems option, boolean transpose) {
		Point pt = new Point(0, 0);

		XLSUtils.setValue(sheet,  pt.x, pt.y, "name:" );
		File file = new File(parent0.vSequence.getFileName(0));
		String path = file.getParent();
		pt = XLSUtils.nextCol(pt, transpose);
		XLSUtils.setValue(sheet,  pt.x, pt.y, path );
		pt= XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.toColZero(pt, transpose);
		Point pt1 = pt;
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, "n cages" );
		pt1 = XLSUtils.nextCol(pt1, transpose);
		XLSUtils.setValue(sheet,  pt1.x, pt1.y, parent0.vSequence.cages.flyPositionsList.size());
		
		switch (option) {
		case DISTANCE:
			break;
		case ISALIVE:
			pt1 = XLSUtils.nextCol(pt1, transpose);
			XLSUtils.setValue(sheet,  pt1.x, pt1.y, "threshold" );
			pt1 = XLSUtils.nextCol(pt1, transpose);
			XLSUtils.setValue(sheet,  pt1.x, pt1.y, parent0.vSequence.cages.detect.threshold);
			break;
		case XYCENTER:
		default:
			break;
		}

		pt = XLSUtils.nextRow(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private Point writeColumnHeaders (Sheet sheet, Point pt, XLSExportItems option, boolean transpose) {
		pt = XLSUtils.toColZero(pt, transpose);
		if (parent0.vSequence.isFileStack()) {
			XLSUtils.setValue(sheet,  pt.x, pt.y, "filename" );
			pt = XLSUtils.nextCol(pt, transpose);
		}
		XLSUtils.setValue(sheet,  pt.x, pt.y, "i" );
		pt = XLSUtils.nextCol(pt, transpose);
		
		switch (option) {
		case DISTANCE:
		case ISALIVE:
			for (XYTaSeries posxyt: parent0.vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0 );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		case XYCENTER:
		default:
			for (XYTaSeries posxyt: parent0.vSequence.cages.flyPositionsList) {
				String name0 = posxyt.getName();
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+".x" );
				pt = XLSUtils.nextCol(pt, transpose);
				XLSUtils.setValue(sheet,  pt.x, pt.y, name0+".y" );
				pt = XLSUtils.nextCol(pt, transpose);
			}
			break;
		}
		pt = XLSUtils.toColZero(pt, transpose);
		pt = XLSUtils.nextRow(pt, transpose);
		return pt;
	}

	private Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Double >> arrayList, boolean transpose) {
	
		ArrayList<XYTaSeries> flyPositionsList = parent0.vSequence.cages.flyPositionsList; 
		int n_time_intervals = flyPositionsList.get(0).pointsList.size();
		int n_series = flyPositionsList.size();
		
		for (int time_interval=0; time_interval< n_time_intervals; time_interval++) {
			int time_absolute = flyPositionsList.get(0).pointsList.get(time_interval).time;
			Point pt2 = XLSUtils.toColZero(pt, transpose);
			if (parent0.vSequence.isFileStack()) {
				String cs = parent0.vSequence.getFileName(time_absolute);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtils.setValue(sheet,  pt2.x, pt2.y, fileName );
				pt2 = XLSUtils.nextCol(pt2, transpose);
			}
			XLSUtils.setValue(sheet,  pt2.x, pt2.y, time_absolute);
			time_absolute  += parent0.vSequence.analysisStep;
			pt2 = XLSUtils.nextCol(pt2, transpose);
			
			switch (option) {
			case DISTANCE:
			case ISALIVE:
				for (int i=0; i < n_series; i++ ) 
				{
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
