package plugins.fmp.multicafe;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.util.GuiUtil;
import icy.system.thread.ThreadUtil;
import plugins.fmp.sequencevirtual.Experiment;
import plugins.fmp.sequencevirtual.ExperimentList;
import plugins.fmp.tools.Tools;
import plugins.fmp.tools.XLSExportCapillaryResults;
import plugins.fmp.tools.XLSExportMoveResults;
import plugins.fmp.tools.XLSExportOptions;


public class MCExcelPane  extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4296207607692017074L;
	private JTabbedPane 		tabsPane 		= new JTabbedPane();
	private MCExcelTab_Options	optionsTab		= new MCExcelTab_Options();
	private MCExcelTab_Kymos		kymosTab		= new MCExcelTab_Kymos();
	private MCExcelTab_Move 		moveTab  		= new MCExcelTab_Move();
	
	private Multicafe parent0 = null;

	void init (JPanel mainPanel, String string, Multicafe parent0) {
		
		this.parent0 = parent0;
		final JPanel excelPanel = GuiUtil.generatePanel(string);
		mainPanel.add(GuiUtil.besidesPanel(excelPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		optionsTab.init(capLayout);
		tabsPane.addTab("Common options", null, optionsTab, "Define common options");
		optionsTab.addPropertyChangeListener(this);
		
		kymosTab.init(capLayout);
		tabsPane.addTab("Capillaries", null, kymosTab, "Export capillary levels to file");
		kymosTab.addPropertyChangeListener(this);
		
		moveTab.init(capLayout);
		tabsPane.addTab("Move", null, moveTab, "Export fly positions to file");
		moveTab.addPropertyChangeListener(this);
		
		excelPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("EXPORT_MOVEDATA")) {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_move.xlsx";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				parent0.capillariesPane.getCapillariesInfos(parent0.vSequence.capillaries);
				ThreadUtil.bgRun( new Runnable() { @Override public void run() {
					XLSExportMoveResults xlsExport = new XLSExportMoveResults();
					xlsExport.exportToFile(filename, getMoveOptions());
				}});
			}
		}
		else if (evt.getPropertyName().equals("EXPORT_KYMOSDATA")) {
			parent0.roisSaveEdits();
			Path directory = Paths.get(parent0.vSequence.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xlsx";
			String file = Tools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				parent0.capillariesPane.getCapillariesInfos(parent0.vSequence.capillaries);
				ThreadUtil.bgRun( new Runnable() { @Override public void run() {
					XLSExportCapillaryResults xlsExport = new XLSExportCapillaryResults();
					xlsExport.exportToFile(filename, getCapillariesOptions());
				}});
				
				firePropertyChange("EXPORT_TO_EXCEL", false, true);	
			}
		}
	}
	
	private XLSExportOptions getMoveOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.xyCenter = moveTab.xyCenterCheckBox.isSelected(); 
		options.distance = moveTab.distanceCheckBox.isSelected();
		options.alive = moveTab.aliveCheckBox.isSelected(); 
		getCommonOptions(options);
		return options;
	}
	
	private void getCommonOptions(XLSExportOptions options) {
		options.pivot 			= optionsTab.pivotCheckBox.isSelected();
		if (options.pivot) {
			options.transpose = true;
			try {
				optionsTab.pivotBinStep.commitEdit();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			options.pivotBinStep = (int) optionsTab.pivotBinStep.getValue();
		}
		else
			options.transpose 	= optionsTab.transposeCheckBox.isSelected();
		options.exportAllFiles 	= optionsTab.exportAllFilesCheckBox.isSelected();
		options.experimentList 	= new ExperimentList ();
		options.collateSeries 	= optionsTab.collateSeriesCheckBox.isSelected();
		options.absoluteTime	= optionsTab.absoluteTimeCheckBox.isSelected();
		
		if (optionsTab.exportAllFilesCheckBox.isSelected()) {
			int nfiles = parent0.sequencePane.infosTab.experimentComboBox.getItemCount();
			for (int i=0; i< nfiles; i++) {
				Experiment exp = new Experiment ();
				exp.filename = parent0.sequencePane.infosTab.experimentComboBox.getItemAt(i);
				options.experimentList.experimentList.add(exp);
			}
		}
		else {
			Experiment exp = new Experiment();
			exp.filename = parent0.vSequence.getFileName();
			options.experimentList.experimentList.add(exp);
		}
	}
	
	private XLSExportOptions getCapillariesOptions() {
		XLSExportOptions options = new XLSExportOptions();
		
		options.topLevel 		= kymosTab.topLevelCheckBox.isSelected(); 
		options.topLevelDelta 	= kymosTab.topLevelDCheckBox.isSelected(); 	
		options.bottomLevel 	= kymosTab.bottomLevelCheckBox.isSelected(); 
		options.derivative 		= kymosTab.derivativeCheckBox.isSelected(); 
		options.consumption 	= kymosTab.consumptionCheckBox.isSelected(); 
		options.sum 			= kymosTab.sumCheckBox.isSelected(); 
		options.t0 				= kymosTab.t0CheckBox.isSelected();
		options.onlyalive 		= kymosTab.onlyaliveCheckBox.isSelected();

		getCommonOptions(options);
		return options;
	}
}
