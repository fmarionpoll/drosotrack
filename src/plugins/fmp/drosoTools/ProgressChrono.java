package plugins.fmp.drosoTools;

import icy.gui.frame.progress.ProgressFrame;
import icy.system.profile.Chronometer;

public class ProgressChrono extends ProgressFrame {

	Chronometer chrono = null;
	private int nbframes;
	private int t0;
	
	
	public ProgressChrono(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public void startChrono() {
		chrono = new Chronometer("Tracking computation" );
		t0 =  (int) (chrono.getNanos() / 1000000000f);
	}
	
	public void initStuff(int nbframes) {
		this.nbframes = nbframes;
		setLength(nbframes);
		startChrono();
	}
	
	public void updatePositionAndTimeLeft (int currentframe) {
		int pos = (int)(100d * (double)currentframe / nbframes);
		setPosition( currentframe );
		int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
		double timeleft = ((double)nbSeconds)* (100d-pos) /pos;
		setMessage( "Processing: " + pos + "% - Estimated time left: " + (int) timeleft + " s");
	}
	
	public int getSecondsSinceStart() {
		int nbSeconds =  (int) (chrono.getNanos() / 1000000000f);
		return (nbSeconds-t0);
	}
}
