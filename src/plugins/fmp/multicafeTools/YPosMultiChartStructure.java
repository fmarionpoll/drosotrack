package plugins.fmp.multicafeTools;

import org.jfree.data.xy.XYSeriesCollection;

public class YPosMultiChartStructure {
	public MinMaxDouble minmax = null;
	public XYSeriesCollection xyDataset = null;

	public YPosMultiChartStructure () {
		
	}
	public YPosMultiChartStructure (MinMaxDouble minmax, XYSeriesCollection xyDataset) {
		this.minmax = minmax;
		this.xyDataset = xyDataset;
	}
}
