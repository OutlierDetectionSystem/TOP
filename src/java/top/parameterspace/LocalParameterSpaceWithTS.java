package top.parameterspace;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class LocalParameterSpaceWithTS extends LocalParameterSpace {
    private long sequenceTimeInterval;
    private long itemTimeInterval;

    public LocalParameterSpaceWithTS(int minLocalSupport, int itemGapConstraint, int seqGapConstraint,
                                     long itemTimeInterval, long seqTimeInterval){
        super(minLocalSupport, itemGapConstraint, seqGapConstraint);
        this.itemTimeInterval = itemTimeInterval;
        this.sequenceTimeInterval = seqTimeInterval;
    }

    public long getSequenceTimeInterval() {
        return sequenceTimeInterval;
    }

    public void setSequenceTimeInterval(long sequenceTimeInterval) {
        this.sequenceTimeInterval = sequenceTimeInterval;
    }

    public long getItemTimeInterval() {
        return itemTimeInterval;
    }

    public void setItemTimeInterval(long itemTimeInterval) {
        this.itemTimeInterval = itemTimeInterval;
    }
}
