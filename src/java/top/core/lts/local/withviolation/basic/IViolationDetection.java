package top.core.lts.local.withviolation.basic;

import java.util.Set;

/**
 * Created by yizhouyan on 7/9/17.
 */
public interface IViolationDetection<T> {
    public void detectViolations(SequencesInvertedIndex previousFreqSeqs,
                                 T violationCandidatesCurrentLen,
                                 Set<String> curFreqSeqs, SequencesInvertedIndex buildCurrentViolationCandidates);

}
