package top.parameterspace;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class LocalParameterSpace {
    private int minLocalSupport;
//    private int minGlobalSupport;
    private int itemGap;
    private int seqGap;

    public LocalParameterSpace(int minLocalSupport, int itemGap, int seqGap){
        this.minLocalSupport = minLocalSupport;
//        this.minGlobalSupport = minGlobalSupport;
        this.itemGap = itemGap;
        this.seqGap = seqGap;
    }

    public int getMinLocalSupport() {
        return minLocalSupport;
    }

    public void setMinLocalSupport(int minSupport) {
        this.minLocalSupport = minSupport;
    }

    public int getItemGap() {
        return itemGap;
    }

    public void setItemGap(int itemGap) {
        this.itemGap = itemGap;
    }

    public int getSeqGap() {
        return seqGap;
    }

    public void setSeqGap(int seqGap) {
        this.seqGap = seqGap;
    }
}
