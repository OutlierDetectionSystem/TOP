package top.core.lts.local.withviolation.withts.explanation;

import top.core.lts.local.withviolation.basic.SequencesInvertedIndex;
import top.core.lts.local.withviolation.basic.ViolationDetectionBase;
import top.core.lts.local.withviolation.basic.ViolationSequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yizhouyan on 6/16/17.
 */
public class ViolationDetection extends ViolationDetectionBase{
    private HashMap<String, ViolationSequence> violations;

    public ViolationDetection(int violationLocalSupport){
        this.violations = new HashMap<String, ViolationSequence>();
        this.violationLocalSupport = violationLocalSupport;
    }

    public void detectViolations(SequencesInvertedIndex previousFreqSeqs,
                                 HashMap<String, ArrayList<String>> violationCandidatesCurrentLen,
                                 Set<String> curFreqSeqs, SequencesInvertedIndex buildCurrentViolationCandidates){
        this.detectSubsequenceViolation(previousFreqSeqs, violationCandidatesCurrentLen);
//        this.detectSuperSequenceViolation(previousViolationCandidates, curFreqSeqs);
        this.detectSameLengthViolation(buildCurrentViolationCandidates, curFreqSeqs, violationCandidatesCurrentLen);
    }

    /**
     * detect subsequence violation: check whether these violationCandidates are subsequences of any existing frequent sequences
     * @param previousFreqSeqs
     * @param violationCandidatesCurrentLen
     */
    private void detectSubsequenceViolation(SequencesInvertedIndex previousFreqSeqs,
                                            HashMap<String, ArrayList<String>> violationCandidatesCurrentLen){
        for (String str: violationCandidatesCurrentLen.keySet()){
            ArrayList<String> sequenceWithAllElements = previousFreqSeqs.getSequencesWithAllElements(str);
            if(sequenceWithAllElements.size() > 0){
                HashSet<String> finalSuperSequences = this.detectSuperSequence(str, sequenceWithAllElements);
                if(finalSuperSequences.size() > 0){
                    this.addToViolations(str, finalSuperSequences, violationCandidatesCurrentLen.get(str));
                }
            }
        }
    }

    private void detectSameLengthViolation(SequencesInvertedIndex buildCurrentViolationCandidates, Set<String> curFreqSeqs,
                                           HashMap<String, ArrayList<String>> violationCandidatesCurrentLen){
        for (String str: curFreqSeqs){
            ArrayList<String> sequenceWithAllElements = buildCurrentViolationCandidates.getSequencesWithAllElements(str);
            for(String violationSeq: sequenceWithAllElements){
                if(disorderedSequence(str, violationSeq)) {
                    this.addToViolations(violationSeq, str, violationCandidatesCurrentLen.get(violationSeq));
                }
            }
        }
    }

    private void addToViolations(String curSeq, HashSet<String> finalSuperSequences, ArrayList<String> violationOccurrences){
        // add to violation
        if(violations.containsKey(curSeq)){
            violations.get(curSeq).addToViolations(finalSuperSequences);
        }else {
            ViolationSequence newViolationSeq = new ViolationSequence(curSeq, violationOccurrences);
            newViolationSeq.addToViolations(finalSuperSequences);
            this.violations.put(curSeq, newViolationSeq);
        }
    }

    private void addToViolations(String curSeq, String finalSuperSequence, ArrayList<String> violationOccurrences){
        // add to violation
        if(violations.containsKey(curSeq)){
            violations.get(curSeq).addToViolations(finalSuperSequence);
        }else {
            ViolationSequence newViolationSeq = new ViolationSequence(curSeq, violationOccurrences);
            newViolationSeq.addToViolations(finalSuperSequence);
            violations.put(curSeq, newViolationSeq);
        }
    }

    public HashMap<String, ViolationSequence> getViolations() {
        return violations;
    }

    public void setViolations(HashMap<String, ViolationSequence> violations) {
        this.violations = violations;
    }

    public static void main(String [] args){
        ViolationDetection vd = new ViolationDetection(2);
        System.out.println(vd.disorderedSequence("51,4,47", "51,4"));
    }

}
