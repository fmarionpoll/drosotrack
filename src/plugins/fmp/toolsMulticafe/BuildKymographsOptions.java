package plugins.fmp.toolsMulticafe;

import plugins.fmp.toolsSequence.SequenceVirtual;

public class BuildKymographsOptions {
	public int analyzeStep = 1;
	public int startFrame = 1;
	public int endFrame = 99999999;
	public SequenceVirtual vSequence = null;
	public int diskRadius = 5;
	public boolean doRegistration = false;
	
}
