package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
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
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.fmp.sequencevirtual.PositionsXYT;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.tools.ArrayListType;
import plugins.fmp.tools.XLSExportItems;
import plugins.fmp.tools.XLSUtils;
import plugins.fmp.tools.Tools;

public class ResultsTab_Excel2  extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;

	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", false);

	private Multicafe parent0 = null;
	
	public void init(GridLayout capLayout, Multicafe parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( transposeCheckBox, new JLabel(" ")));
		add(GuiUtil.besidesPanel( new JLabel(" "), exportToXLSButton)); 
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
			String tentativeName = subpath.toString()+"_feeding.xlsx";
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
			
			xlsExportWorkSheetDistance(workbook);
			xlsExportWorkSheetXY(workbook);
			xlsExportWorkSheetAliveOrNot(workbook);
			
			FileOutputStream fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
	        fileOut.close();
	        
	        workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private void xlsExportWorkSheetAliveOrNot(Workbook workBook) {
		
		String title = "alive";
		System.out.println("export worksheet "+title);
		ArrayList<PositionsXYT> flyPositionsList = parent0.vSequence.cages.flyPositionsList;		
		if (flyPositionsList.size() == 0)
			return;

		Sheet sheet = workBook.createSheet(title );
		boolean transpose = transposeCheckBox.isSelected(); 
		Point pt = writeGlobalInfos(sheet, transpose);
		pt = writeColumnHeaders(sheet, pt, option, transpose);
		pt = writeData(sheet, pt, option, arrayList, transpose);
	}
	
	private Point writeData (Sheet sheet, Point pt, XLSExportItems option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose) {	
		ArrayList<PositionsXYT> flyPositionsList = parent0.vSequence.cages.flyPositionsList;
		
		// local variables used for exporting the work sheet
		int irow = 0;
		int nrois = flyPositionsList.size();
		int icol0 = 0;

		// xls output - distances
		// --------------
		WritableSheet alivePage = XLSUtil.createNewPage( workBook , "alive" );
		XLSUtil.setCellString( alivePage , 0, irow, "name:" );
		XLSUtil.setCellString( alivePage , 1, irow, vSequence.getName() );
		irow++;;
		
		XLSUtil.setCellString( alivePage , 0, irow, "Last movement (index):" );
		int icol = 1;
		if (blistofFiles)
			icol ++;
		for (int iroi=0; iroi < nrois; iroi++, icol++) {
			XLSUtil.setCellNumber( alivePage , icol, irow,  lastTime_it_MovedList.get(iroi) );
		}
		irow=2;
		// table header
		icol0 = 0;
		if (blistofFiles) {
			XLSUtil.setCellString( alivePage , icol0,   irow, "filename" );
			icol0++;
		}
		XLSUtil.setCellString( alivePage , icol0, irow, "index" );
		icol0++;
		for (int iroi=0; iroi < nrois; iroi++, icol0++) {
			XLSUtil.setCellString( alivePage , icol0, irow, roiList.get(iroi).getName() );
		}
		irow++;

		// data
		for ( int t = startFrame+1 ; t < endFrame;  t  += analyzeStep )
		{
			
				icol0 = 0;
				if (blistofFiles) {
					XLSUtil.setCellString( alivePage , icol0,   irow, listofFiles[t] );
					icol0++;
				}
				XLSUtil.setCellNumber( alivePage, icol0 , irow , t ); // frame number
				icol0++;
				
				for (int iroi=0; iroi < nrois; iroi++) {
					int alive = 1;
					if (t > lastTime_it_MovedList.get(iroi))
						alive = 0;
					XLSUtil.setCellNumber( alivePage, icol0 , irow , alive ); 
					icol0++;
				}
				
				irow++;
			}
		}

	
	
	private void xlsExportWorkSheetXY(Workbook workBook) {
		
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (selectInputStack2Button.isSelected() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		// local variables used for exporting the 2 worksheets
		int it = 0;
		int irow = 0;
		int nrois = cageLimitROIList.size();
		int icol0 = 0;

		// --------------
		WritableSheet xyMousePositionPage = XLSUtil.createNewPage( workBook , "xy" );
		// output last interval at which movement was detected over the whole period analyzed
		irow = 0;
		XLSUtil.setCellString( xyMousePositionPage , 0, irow, "name:" );
		XLSUtil.setCellString( xyMousePositionPage , 1, irow, vSequence.getName() );
		irow++;
		nrois = cageLimitROIList.size();
		
		// output points detected
		irow= 2;
		icol0 = 0;
		if (selectInputStack2Button.isSelected() )
		{
			XLSUtil.setCellString( xyMousePositionPage , 0,   irow, "filename");
			icol0++;
		}
		XLSUtil.setCellString( xyMousePositionPage , icol0,   irow, "interval");
		icol0++;

		for (int iroi=0; iroi < nrois; iroi++) {
			XLSUtil.setCellString( xyMousePositionPage , icol0,   irow, "x"+ iroi );
			icol0++;
			XLSUtil.setCellString( xyMousePositionPage , icol0, irow, "y"+ iroi );
			icol0++;
		}

		// reset the previous point array
		ArrayList<Point2D> XYPoints_of_Row_t = new ArrayList<Point2D>();
		for (int iroi = 0; iroi < nrois; iroi++)
			XYPoints_of_Row_t.add(points2D_rois_then_t_ListArray.get(iroi).get(0));

		it = 0;
		for ( int t = startFrame ; t < endFrame;  t  += analyzeStep, it++ )
		{
			try
			{
				irow++;
				icol0 = 0;
				if (blistofFiles) {
					XLSUtil.setCellString( xyMousePositionPage , icol0,   irow, listofFiles[t] );
					icol0++;
				}
				XLSUtil.setCellNumber( xyMousePositionPage, icol0 , irow , t ); // frame number
				icol0++;

				for (int iroi=0; iroi < nrois; iroi++) {

					Point2D mousePosition = points2D_rois_then_t_ListArray.get(iroi).get(it);
					XLSUtil.setCellNumber( xyMousePositionPage, icol0 , 	irow , mousePosition.getX() ); // x location
					icol0++;
					XLSUtil.setCellNumber( xyMousePositionPage, icol0 ,irow , mousePosition.getY() ); // y location
					icol0++;
					XYPoints_of_Row_t.set(iroi, mousePosition);
				}
			}catch( IndexOutOfBoundsException e)
			{
				// no mouse Position
			}
		}
	}
	
	private void xlsExportWorkSheetDistance(Workbook workBook) {
		
		String[] listofFiles = null;
		boolean blistofFiles = false;
		if (selectInputStack2Button.isSelected() )
		{
			listofFiles = vSequence.getListofFiles();
			blistofFiles = true;
		}
		// local variables used for exporting the 2 worksheets
		int it = 0;
		int irow = 0;
		int nrois = cageLimitROIList.size();
		int icol0 = 0;

		// xls output - distances
		// --------------
		WritableSheet distancePage = XLSUtil.createNewPage( workBook , "distance" );
		XLSUtil.setCellString( distancePage , 0, irow, "name:" );
		XLSUtil.setCellString( distancePage , 1, irow, vSequence.getName() );
		irow++;;
		
		XLSUtil.setCellString( distancePage , 0, irow, "Last movement (index):" );
		int icol = 1;
		if (blistofFiles)
			icol ++;
		for (int iroi=0; iroi < nrois; iroi++, icol++) {
			XLSUtil.setCellNumber( distancePage , icol, irow,  lastTime_it_MovedList.get(iroi) );
		}
		irow=2;
		nrois = cageLimitROIList.size();
		irow++;
		
		// table header
		icol0 = 0;
		if (blistofFiles) {
			XLSUtil.setCellString( distancePage , icol0,   irow, "filename" );
			icol0++;
		}
		XLSUtil.setCellString( distancePage , icol0, irow, "index" );
		icol0++;
		for (int iroi=0; iroi < nrois; iroi++, icol0++) {
			XLSUtil.setCellString( distancePage , icol0, irow, roiList.get(iroi).getName() );
		}
		irow++;

		// data
		it = 0;
		for ( int t = startFrame+1 ; t < endFrame;  t  += analyzeStep, it++ )
		{

			icol0 = 0;
			if (blistofFiles) {
				XLSUtil.setCellString( distancePage , icol0,   irow, listofFiles[t] );
				icol0++;
			}
			XLSUtil.setCellNumber( distancePage, icol0 , irow , t ); // frame number
			icol0++;
			
			for (int iroi=0; iroi < nrois; iroi++) {

				Point2D mousePosition = points2D_rois_then_t_ListArray.get(iroi).get(it);
				double distance = mousePosition.distance(points2D_rois_then_t_ListArray.get(iroi).get(it-1)); 
				XLSUtil.setCellNumber( distancePage, icol0 , irow , distance ); 
				icol0++;

			}
			irow++;
		}
	}
		


}
