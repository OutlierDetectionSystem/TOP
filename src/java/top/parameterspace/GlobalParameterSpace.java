package top.parameterspace;

/**
 * Created by yizhouyan on 7/9/17.
 */
public class GlobalParameterSpace {
    private LocalParameterSpace localParameterSpace;
    private int minGlobalSupport;
    private int violationLocalSupport;
    private int thresholdForLocalFSOutliers;

    public GlobalParameterSpace(int minLocalSupport, int eventGap, int seqGap, int minGlobalSupport,
                                int violationLocalSupport, int thresholdForLocalFSOutliers){
        this.localParameterSpace = new LocalParameterSpace(minLocalSupport, eventGap, seqGap);
        this.minGlobalSupport = minGlobalSupport;
        this.violationLocalSupport = violationLocalSupport;
        this.thresholdForLocalFSOutliers = thresholdForLocalFSOutliers;
    }

    public LocalParameterSpace getLocalParameterSpace() {
        return localParameterSpace;
    }

    public void setLocalParameterSpace(LocalParameterSpace localParameterSpace) {
        this.localParameterSpace = localParameterSpace;
    }

    public int getMinGlobalSupport() {
        return minGlobalSupport;
    }

    public void setMinGlobalSupport(int minGlobalSupport) {
        this.minGlobalSupport = minGlobalSupport;
    }

    public int getViolationLocalSupport() {
        return violationLocalSupport;
    }

    public void setViolationLocalSupport(int violationLocalSupport) {
        this.violationLocalSupport = violationLocalSupport;
    }

    public int getThresholdForLocalFSOutliers() {
        return thresholdForLocalFSOutliers;
    }

    public void setThresholdForLocalFSOutliers(int thresholdForLocalFSOutliers) {
        this.thresholdForLocalFSOutliers = thresholdForLocalFSOutliers;
    }
}
