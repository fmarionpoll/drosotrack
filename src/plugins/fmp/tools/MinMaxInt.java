package plugins.fmp.tools;

public class MinMaxInt {
	public int max=0;
	public int min=0;
	
	public MinMaxInt() {
	}
	
	public MinMaxInt(int val1, int val2 ) {
		if (val1 >= val2) {
			max = val1;
			min = val2;
		}
		else {
			min = val1;
			max = val2;
		}
	}
	
	public MinMaxInt getMaxMin(int value1, int value2) {
		getMaxMin(value1);
		getMaxMin(value2);
		return this;
	}
	
	public MinMaxInt getMaxMin(MinMaxInt val) {
		getMaxMin(val.min);
		getMaxMin(val.max);
		return this;
	}
	
	public MinMaxInt getMaxMin(int value) {
		if (value > max)
			max = value;
		if (value < min)
			min = value;
		return this;
	}

}
