package top.utils;

public class SQConfig {
	/**============================ basic threshold ================ */
	public static final String strEventGap = "lts.local.eventgap";
	public static final String strSeqGap = "lts.local.seqgap";
	public static final String strEventTimeInterval = "lts.local.timestamp.event";
	public static final String strSeqTimeInterval = "lts.local.timestamp.sequence";
	public static final String strLocalSupport = "lts.local.localsupport";
	public static final String strViolationLocalSupport = "lts.local.violation.localsupport";
	public static final String strLocalSupportPortion = "lts.local.localsupport.portion";
	public static final String strGlobalSupport = "lts.local.globalsupport";
	public static final String strOutlierThreshold = "lts.local.outlierthres";
	public static final String strNumPartitions = "lts.local.partition.number";
	public static final String strNumReducers = "lts.local.reducer.number";
	
	/**============================= seperators ================ */
	/** seperator for items of every record in the index */
	public static final String sepStrForRecord = ",";
	public static final String sepStrForKeyValue = "\t";
	public static final String sepStrForIDDist = "|";
	public static final String sepSplitForIDDist = "\\|";
	
	/**============================= input/output path =================== */
	public static final String strInputDataset = "lts.local.dataset.input";
	public static final String strGlobalFreqElements = "lts.local.globalfreq.elements";
	public static final String strLocalFSOutputLTS = "lts.local.fsoutput.output";
	public static final String strLocalFSOutputSTL = "stl.local.fsoutput.output";
	public static final String strLocalViolationOutput = "lts.local.violation.output";
	public static final String strFinalViolationOutput = "lts.final.violation.output";
	public static final String strViolationWithExplanationOutput = "lts.final.violation.explanation.output";
	public static final String strFinalFilteredViolationOutput = "lts.final.violation.filtered.output";
	public static final String strDevicesWithViolations = "lts.final.violation.devices.output";
	public static final String strGlobalFSOutputForViolation = "lts.local.violation.globalfs.output";
	public static final String strMetaDataInput = "lts.local.metadata.input";
	public static final String strLocalOutlierOutput = "lts.local.outlier.output";
	public static final String strGlobalFSOutput = "lts.local.globalfs.output";
	
	/** =========================== clean dataset ======================== */
	public static final String strOriginalInput = "lts.local.dataset.original";
	public static final String strInputDatasetLarge = "lts.local.dataset.input.large";


	/** ======================= generate data ========================== */
	public static final String strDictionaryPath = "lts.local.dictionary";
	public static final String strOutlierPath = "lts.local.dictionary.outlier";
	public static final String strViolationPath = "lts.local.dictionary.violation";
	public static final String strAlphabetSize = "lts.local.alphabet.size";
	public static final String strSequenceLength = "lts.local.sequence.length";
	public static final String strMaxLenFreqPatterns = "lts.local.freqpatterns.maxlen";
	public static final String strNumFreqPatterns = "lts.local.freqpatterns.number";
	public static final String strNumPatterns = "lts.local.patterns.number";
	public static final String strNumDevices = "lts.local.numDevices";
	public static final String strDummyInput = "lts.local.dummy.input";
	public static final String strMinLocalSupport = "lts.local.min.localsupport";
	public static final String strMinGlobalSupport = "lts.local.min.globalsupport";

	/** ======================= basic statistics ========================== */
	public static final String strCountSyncPerDayOutput = "top.basic.countsync.output";
}
