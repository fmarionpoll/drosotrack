package plugins.fmp.areatrack;

// simple container for 2 values and a string descriptor
public class MeasureAndName {
	public String name;
	public double data;
	public double count;

	public MeasureAndName(String name, double data, double count) {
		this.name = name;
		this.data = data;
		this.count = count;
	}

}
