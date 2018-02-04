package top.core.maximum.mining.violations;

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

    public void detectViolations(HashMap<String, ArrayList<String>> violationCandidatesCurrentLen,
                                 Set<String> curFreqSeqs, SequencesInvertedIndex buildCurrentViolationCandidates){
        this.detectSameLengthViolation(buildCurrentViolationCandidates, curFreqSeqs, violationCandidatesCurrentLen);
    }

    private void detectSameLengthViolation(SequencesInvertedIndex buildCurrentViolationCandidates, Set<String> curFreqSeqs,
                                           HashMap<String, ArrayList<String>> violationCandidatesCurrentLen){
        for (String str: curFreqSeqs){
            ArrayList<String> sequenceWithAllElements = buildCurrentViolationCandidates.getSequencesWithAllElements(str);
            for(String violationSeq: sequenceWithAllElements){
                if(str.equals(violationSeq))
                    continue;
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
