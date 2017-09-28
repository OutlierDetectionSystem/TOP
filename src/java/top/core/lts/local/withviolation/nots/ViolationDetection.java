package top.core.lts.local.withviolation.nots;

import top.core.lts.local.withviolation.basic.IViolationDetection;
import top.core.lts.local.withviolation.basic.SequencesInvertedIndex;
import top.core.lts.local.withviolation.basic.ViolationDetectionBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yizhouyan on 6/16/17.
 */
public class ViolationDetection extends ViolationDetectionBase implements IViolationDetection<HashSet<String>>{
    private HashMap<String, Set<String>> violations;

    public ViolationDetection(int violationLocalSupport){
        this.violations = new HashMap<String, Set<String>>();
        this.violationLocalSupport = violationLocalSupport;
    }

    public void detectViolations(SequencesInvertedIndex previousFreqSeqs,
                                 HashSet<String> violationCandidatesCurrentLen,
                                 Set<String> curFreqSeqs, SequencesInvertedIndex buildCurrentViolationCandidates){
        this.detectSubsequenceViolation(previousFreqSeqs, violationCandidatesCurrentLen);
//        this.detectSuperSequenceViolation(previousViolationCandidates, curFreqSeqs);
        this.detectSameLengthViolation(buildCurrentViolationCandidates, curFreqSeqs);
    }

    /**
     * detect subsequence violation: check whether these violationCandidates are subsequences of any existing frequent sequences
     * @param previousFreqSeqs
     * @param violationCandidatesCurrentLen
     */
    private void detectSubsequenceViolation(SequencesInvertedIndex previousFreqSeqs,
                                           HashSet<String> violationCandidatesCurrentLen){
        for (String str: violationCandidatesCurrentLen){
            ArrayList<String> sequenceWithAllElements = previousFreqSeqs.getSequencesWithAllElements(str);
            if(sequenceWithAllElements.size() > 0){
                HashSet<String> finalSuperSequences = this.detectSuperSequence(str, sequenceWithAllElements);
                if(finalSuperSequences.size() > 0){
                    this.addToViolations(str, finalSuperSequences);
                }
            }
        }
    }

    /**
     * detect super-sequence violation: check whether presaved super-sequences are violations of current detected frequent sequences
     */
//    private void detectSuperSequenceViolation(SequencesInvertedIndex previousViolationCandidates, Set<String> curFreqSeqs){
//        for(String str: curFreqSeqs){
//            ArrayList<String> sequenceWithAllElements = previousViolationCandidates.getSequencesWithAllElements(str);
//            if(sequenceWithAllElements.size() > 0){
//                HashSet<String> finalSuperSequences = this.detectSuperSequence(str, sequenceWithAllElements);
//                for(String violationSeq: finalSuperSequences){
//                    this.addToViolations(violationSeq, str);
//                }
//            }
//        }
//    }

    private void detectSameLengthViolation(SequencesInvertedIndex buildCurrentViolationCandidates, Set<String> curFreqSeqs){
        for (String str: curFreqSeqs){
            ArrayList<String> sequenceWithAllElements = buildCurrentViolationCandidates.getSequencesWithAllElements(str);
            for(String violationSeq: sequenceWithAllElements){
                if(disorderedSequence(str, violationSeq)) {
                    this.addToViolations(violationSeq, str);
                }
            }
        }
    }

    private void addToViolations(String curSeq, HashSet<String> finalSuperSequences){
        // add to violation
        if(violations.containsKey(curSeq)){
            violations.get(curSeq).addAll(finalSuperSequences);
        }else
            violations.put(curSeq, finalSuperSequences);
    }

    private void addToViolations(String curSeq, String finalSuperSequence){
        // add to violation
        if(violations.containsKey(curSeq)){
            violations.get(curSeq).add(finalSuperSequence);
        }else {
            HashSet<String> violatedSeqs = new HashSet<String>();
            violatedSeqs.add(finalSuperSequence);
            violations.put(curSeq, violatedSeqs);
        }
    }

    public HashMap<String, Set<String>> getViolations() {
        return violations;
    }

    public void setViolations(HashMap<String, Set<String>> violations) {
        this.violations = violations;
    }

    public static void main(String [] args){
        ViolationDetection vd = new ViolationDetection(2);
        System.out.println(vd.disorderedSequence("51,4,47", "51,4"));
    }

}
