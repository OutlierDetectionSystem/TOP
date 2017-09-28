package top.core.lts.local.withviolation.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by yizhouyan on 7/9/17.
 */
public class ViolationDetectionBase {
    protected int violationLocalSupport = 10;

    public boolean disorderedSequence(String originalSeq, String targetSeq){
        HashMap<String, Integer> mapForOriginalSeq = new HashMap<String, Integer>();
        String [] originalSplits = originalSeq.split(",");
        for(String str: originalSplits){
            if(mapForOriginalSeq.containsKey(str))
                mapForOriginalSeq.put(str, mapForOriginalSeq.get(str)+1);
            else
                mapForOriginalSeq.put(str, 1);
        }
        HashMap<String, Integer> mapForTargetSeq = new HashMap<String, Integer>();
        String [] targetSplits = targetSeq.split(",");
        for(String str: targetSplits){
            if (mapForTargetSeq.containsKey(str))
                mapForTargetSeq.put(str, mapForTargetSeq.get(str) + 1);
            else
                mapForTargetSeq.put(str, 1);
        }
        if(mapForOriginalSeq.size() != mapForTargetSeq.size())
            return false;
        for(String str: mapForOriginalSeq.keySet()){
            if(!mapForTargetSeq.containsKey(str))
                return false;
            if(mapForOriginalSeq.get(str) != mapForTargetSeq.get(str)){
                return false;
            }
        }
        return true;
    }

    protected HashSet<String> detectSuperSequence(String curSeq, ArrayList<String> sequenceWithAllElements){
        HashSet<String> finalSuperSequences = new HashSet<String>();
        for(String superSequence: sequenceWithAllElements){
            if(strArrayContains(superSequence.split(","), curSeq.split(","))){
                finalSuperSequences.add(superSequence);
            }
        }
        return finalSuperSequences;
    }

    /**
     * check sub array
     *
     * @param strList1
     * @param strList2
     * @return
     */
    private boolean strArrayContains(String [] strList1, String [] strList2) {
        boolean isContained = false;

        for (int i = 0; i < strList1.length - strList2.length + 1; i++) {
            int k = i;
            int j = 0;
            while (k < strList1.length && j < strList2.length) {
                if (strList1[k].equals(strList2[j])) {
                    k++;
                    j++;
                } else {
                    k++;
                }
            }
            if (j == strList2.length) {
                isContained = true;
                break;
            }
        }
        return isContained;
    }

    public int getViolationLocalSupport() {
        return violationLocalSupport;
    }

    public void setViolationLocalSupport(int violationLocalSupport) {
        this.violationLocalSupport = violationLocalSupport;
    }

}
