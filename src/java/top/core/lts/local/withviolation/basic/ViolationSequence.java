package top.core.lts.local.withviolation.basic;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by yizhouyan on 6/28/17.
 */
public class ViolationSequence {

    private String violationSeq;
    private HashSet<String> violatedSeq;
    private ArrayList<String> occurrencesOfViolations;

    public ViolationSequence(String violationSeq, ArrayList<String> occurrencesOfViolations){
        this.violationSeq = violationSeq;
        this.occurrencesOfViolations = occurrencesOfViolations;
        this.violatedSeq = new HashSet<String>();
    }

    public void addToViolations(HashSet<String> finalSuperSequences){
        // add to violation
        this.violatedSeq.addAll(finalSuperSequences);
    }

    public void addToViolations(String finalSuperSequence){
        // add to violation
       this.violatedSeq.add(finalSuperSequence);
    }

    public String printViolation(){
        if(occurrencesOfViolations.size() <= 0 || violatedSeq.size() <=0 )
            return "";
        // output format: violation seq [occurrences-1][-2][-3] | violated seq1 | violated seq2
        String str = violationSeq ;
        for(String occurrence: occurrencesOfViolations){
            str += "[" + occurrence + "]";
        }
        for(String seq: violatedSeq){
            str += "|" + seq;
        }
        return str;
    }

    public String getViolationSeq() {
        return violationSeq;
    }

    public void setViolationSeq(String violationSeq) {
        this.violationSeq = violationSeq;
    }

    public HashSet<String> getViolatedSeq() {
        return violatedSeq;
    }

    public void setViolatedSeq(HashSet<String> violatedSeq) {
        this.violatedSeq = violatedSeq;
    }

    public ArrayList<String> getOccurrencesOfViolations() {
        return occurrencesOfViolations;
    }

    public void setOccurrencesOfViolations(ArrayList<String> occurrencesOfViolations) {
        this.occurrencesOfViolations = occurrencesOfViolations;
    }
}
