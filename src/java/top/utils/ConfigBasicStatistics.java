package top.utils;

/**
 * Created by yizhouyan on 9/27/17.
 */
public class ConfigBasicStatistics {
    /**============================ basic threshold ================ */
    public static final String strTimeInterval = "top.basic.timestamp.interval";
    public static final String strSupport = "top.basic.support";
    public static final String strEventType = "top.basic.eventType";
    public static final String strNumPartitions = "top.basic.partition.number";
    public static final String strNumReducers = "top.basic.reducer.number";

    /**============================= seperators ================ */
    /** seperator for items of every record in the index */
    public static final String sepStrForRecord = ",";
    public static final String sepStrForKeyValue = "\t";
    public static final String sepSplitForIDDist = "\\|";

    /**============================= input/output path =================== */
    public static final String strInputDataset = "top.basic.dataset.input";
    public static final String strCountSyncPerDayOutput = "top.basic.countsync.output";
}
