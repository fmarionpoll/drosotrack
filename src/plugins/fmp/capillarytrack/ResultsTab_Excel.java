package plugins.fmp.capillarytrack;

import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import icy.gui.util.GuiUtil;
import icy.util.XLSUtil;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.fmp.sequencevirtual.SequencePlus;
import plugins.fmp.sequencevirtual.SequencePlus.ArrayListType;
import plugins.fmp.tools.Tools;

public class ResultsTab_Excel extends JPanel implements ActionListener  {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1290058998782225526L;
	enum ExportXLS {TOPLEVEL, BOTTOMLEVEL, DERIVEDVALUES, SUMGULPS, SUMLR};
	public JButton 		exportToXLSButton 	= new JButton("save XLS");
	public JCheckBox 	topLevelCheckBox 	= new JCheckBox("top", true);
	public JCheckBox 	bottomLevelCheckBox = new JCheckBox("bottom", false);
	public JCheckBox 	consumptionCheckBox = new JCheckBox("gulps", true);
	public JCheckBox 	sumCheckBox 		= new JCheckBox("L+R", true);
	public JCheckBox 	derivativeCheckBox  = new JCheckBox("derivative", false);
	public JCheckBox	t0CheckBox			= new JCheckBox("t-t0", true);
	public JCheckBox	transposeCheckBox 	= new JCheckBox("transpose", false);
	

	private Capillarytrack parent0 = null;
	
	
	public void init(GridLayout capLayout, Capillarytrack parent0) {	
		setLayout(capLayout);
		this.parent0 = parent0;
		add(GuiUtil.besidesPanel( topLevelCheckBox, bottomLevelCheckBox, consumptionCheckBox, sumCheckBox));
		add(GuiUtil.besidesPanel( t0CheckBox, transposeCheckBox, new JLabel(" "), exportToXLSButton)); 
		defineActionListeners();
	}
	
	private void defineActionListeners() {
		exportToXLSButton.addActionListener (this);
	}

	public void enableItems(boolean enabled) {
		exportToXLSButton.setEnabled(enabled);
		topLevelCheckBox.setEnabled(enabled);
		bottomLevelCheckBox.setEnabled(enabled);
		consumptionCheckBox.setEnabled(enabled);
		sumCheckBox.setEnabled(enabled);
		t0CheckBox.setEnabled(enabled);
		transposeCheckBox.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if ( o == exportToXLSButton)  {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xls";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xls");
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
			WritableWorkbook xlsWorkBook = XLSUtil.createWorkbook( filename); 
			if (topLevelCheckBox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "toplevel", ExportXLS.TOPLEVEL);
			if (bottomLevelCheckBox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "bottomlevel", ExportXLS.BOTTOMLEVEL);
			if (derivativeCheckBox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "derivative", ExportXLS.DERIVEDVALUES);
			if (consumptionCheckBox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "sumGulps", ExportXLS.SUMGULPS);
			if (sumCheckBox.isSelected()) 
				xlsExportToWorkbook(xlsWorkBook, "sumL+R", ExportXLS.SUMLR);
			XLSUtil.saveAndClose( xlsWorkBook );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (WriteException e) {
			e.printStackTrace();
		}
		System.out.println("XLS output finished");
	}

	private ArrayList <ArrayList<Integer >> getDataFromRois(ExportXLS option, boolean relative) {
		ArrayList <ArrayList<Integer >> arrayList = new ArrayList <ArrayList <Integer>> ();
		for (SequencePlus seq: parent0.kymographArrayList) {
			switch (option) {
			case DERIVEDVALUES:
				arrayList.add(seq.getArrayListFromRois(ArrayListType.derivedValues));
				break;
			case SUMGULPS: 
				seq.getArrayListFromRois(ArrayListType.cumSum);
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
	
	private void xlsExportToWorkbook(WritableWorkbook xlsWorkBook, String title, ExportXLS option) {
		System.out.println("export worksheet "+title);
		ArrayList <ArrayList<Integer >> arrayList = getDataFromRois(option, t0CheckBox.isSelected());
		
		if (arrayList.size() == 0)
			return;

		WritableSheet excelSheet = XLSUtil.createNewPage( xlsWorkBook , title );
		
		boolean transpose =transposeCheckBox.isSelected(); 

		Point pt = writeGlobalInfos(excelSheet, transpose);
		pt = writeColumnHeaders(excelSheet, pt, option, transpose);
		pt = writeData(excelSheet, pt, option, arrayList, transpose);

	}
	
	private Point writeGlobalInfos(WritableSheet excelSheet, boolean transpose) {
		Point pt = new Point(0, 0);

		XLSUtil.setCellString( excelSheet, pt.x, pt.y, "name:" );
		File file = new File(parent0.vSequence.getFileName(0));
		String path = file.getParent();
		pt = nextCol(pt, transpose);
		XLSUtil.setCellString( excelSheet , pt.x, pt.y, path );
		pt= nextRow(pt, transpose);
		pt = toColZero(pt, transpose);
		Point pt1 = pt;
		XLSUtil.setCellString( excelSheet, pt1.x, pt1.y, "capillary (µl):" );
		pt1 = nextCol(pt1, transpose);
		XLSUtil.setCellNumber( excelSheet, pt1.x, pt1.y, 	parent0.vSequence.capillaryVolume);
		pt1 = nextCol(pt1, transpose);
		XLSUtil.setCellString( excelSheet, pt1.x, pt1.y, "capillary (pixels):" );
		pt1 = nextCol(pt1, transpose);
		XLSUtil.setCellNumber( excelSheet, pt1.x, pt1.y, 	parent0.vSequence.capillaryPixels);
		pt = nextRow(pt, transpose);
		pt = nextRow(pt, transpose);
		return pt;
	}

	private Point nextRow (Point pt, boolean transpose) {
		if (!transpose)
			pt.y ++;
		else
			pt.x++;
		return pt;
	}
	private Point nextCol (Point pt, boolean transpose) {
		if (!transpose) 
			pt.x ++;
		else 
			pt.y++;
		return pt;
	}	
	private Point toColZero (Point pt, boolean transpose) {
		if (!transpose) 
			pt.x = 0;
		else
			pt.y = 0;
		return pt;
	}

	
	private Point writeColumnHeaders (WritableSheet excelSheet, Point pt, ExportXLS option, boolean transpose) {
		pt = toColZero(pt, transpose);
		if (parent0.vSequence.isFileStack()) {
			XLSUtil.setCellString( excelSheet, pt.x, pt.y, "filename" );
			pt = nextCol(pt, transpose);
		}
		XLSUtil.setCellString( excelSheet, pt.x, pt.y, "i" );
		pt = nextCol(pt, transpose);
		
		switch (option) {
		case SUMLR:
			for (int i=0; i< parent0.kymographArrayList.size(); i+= 2) {
				SequencePlus kymographSeq0 = parent0.kymographArrayList.get(i);
				String name0 = kymographSeq0.getName();
				SequencePlus kymographSeq1 = parent0.kymographArrayList.get(i+1);
				String name1 = kymographSeq1.getName();
				XLSUtil.setCellString( excelSheet, pt.x, pt.y, name0+"+"+name1 );
				pt = nextCol(pt, transpose);
				XLSUtil.setCellString( excelSheet, pt.x, pt.y, "." );
				pt = nextCol(pt, transpose);
			}
			break;
		default:
			for (int i=0; i< parent0.kymographArrayList.size(); i++) {
				SequencePlus kymographSeq = parent0.kymographArrayList.get(i);
				String name = kymographSeq.getName();
				XLSUtil.setCellString( excelSheet, pt.x, pt.y, name );
				pt = nextCol(pt, transpose);
			}
			break;
		}
		pt = toColZero(pt, transpose);
		pt = nextRow(pt, transpose);
		return pt;
	}

	private Point writeData (WritableSheet excelSheet, Point pt, ExportXLS option, ArrayList <ArrayList<Integer >> arrayList, boolean transpose) {
		int maxelements = 0;
		for (int i=0; i< arrayList.size(); i++) {
			ArrayList<Integer> datai = arrayList.get(i);
			if (datai.size() > maxelements)
				maxelements = datai.size();
		}
		int nelements = maxelements-1;
		if (nelements <= 0)
			return pt;
		
		double ratio = parent0.vSequence.capillaryVolume / parent0.vSequence.capillaryPixels;
		
		int startFrame = (int) parent0.vSequence.analysisStart;
		int t = startFrame;
		for (int j=0; j< nelements; j++) {
			Point pt2 = toColZero(pt, transpose);
			if (parent0.vSequence.isFileStack()) {
				String cs = parent0.vSequence.getFileName(j+startFrame);
				int index = cs.lastIndexOf("\\");
				String fileName = cs.substring(index + 1);
				XLSUtil.setCellString( excelSheet, pt2.x, pt2.y, fileName );
				pt2 = nextCol(pt2, transpose);
			}

			XLSUtil.setCellNumber( excelSheet, pt2.x, pt2.y, t);
			t  += parent0.vSequence.analysisStep;
			pt2 = nextCol(pt2, transpose);
			
			switch (option) {
			case SUMLR:
				for (int i=0; i< parent0.kymographArrayList.size(); i+=2) 
				{
					ArrayList<Integer> dataL = arrayList.get(i);
					ArrayList<Integer> dataR = arrayList.get(i+1);
					if (j < dataL.size())
						XLSUtil.setCellNumber( excelSheet, pt2.x, pt2.y, (dataL.get(j)+dataR.get(j))*ratio );
					pt2 = nextCol(pt2, transpose);
					pt2 = nextCol(pt2, transpose);
				}
				break;

			default:
				for (int i=0; i< parent0.kymographArrayList.size(); i++) 
				{
					ArrayList<Integer> data = arrayList.get(i);
					if (j < data.size())
						XLSUtil.setCellNumber( excelSheet, pt2.x, pt2.y, data.get(j)*ratio );
					pt2 = nextCol (pt2, transpose);
				}
				break;
			}
			
			pt = nextRow (pt, transpose);
		}
		return pt;
	}

}
