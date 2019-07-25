package plugins.fmp.multicafeTools;

import plugins.fmp.multicafeSequence.SequenceVirtual;

public class BuildKymographsOptions {
	public int analyzeStep = 1;
	public int startFrame = 1;
	public int endFrame = 99999999;
	public SequenceVirtual vSequence = null;
	public int diskRadius = 5;
	public boolean doRegistration = false;
	
}
