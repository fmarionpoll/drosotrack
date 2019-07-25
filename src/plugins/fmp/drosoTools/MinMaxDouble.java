package plugins.fmp.drosoTools;

public class MinMaxDouble {
	public double max=0;
	public double min=0;
	
	public MinMaxDouble() {
	}
	
	public MinMaxDouble(double val1, double val2 ) {
		if (val1 >= val2) {
			max = val1;
			min = val2;
		}
		else {
			min = val1;
			max = val2;
		}
	}
	
	public MinMaxDouble getMaxMin(double value1, double value2) {
		getMaxMin(value1);
		getMaxMin(value2);
		return this;
	}
	
	public MinMaxDouble getMaxMin(MinMaxDouble val) {
		getMaxMin(val.min);
		getMaxMin(val.max);
		return this;
	}
	
	public MinMaxDouble getMaxMin(double value) {
		if (value > max)
			max = value;
		if (value < min)
			min = value;
		return this;
	}

}
