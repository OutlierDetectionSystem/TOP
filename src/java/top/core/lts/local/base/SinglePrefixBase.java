package top.core.lts.local.base;

import top.distributed.cleanrealdata.SingleSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yizhouyan on 7/9/17.
 */
public class SinglePrefixBase {
    protected String prefix;
    protected HashMap<Integer, ArrayList<SingleSequenceBase>> originalSeqsOrderbyLength;
    protected int maxLengthOfSeq = -1;
    protected ArrayList<SingleSequenceBase> candidateSeqs;
    protected int candidateLengthTo = Integer.MAX_VALUE;
    protected int absoluteMaxLength = -1;

    public SinglePrefixBase(String prefix, HashMap<Integer, ArrayList<SingleSequenceBase>> originalSeqs,
                             int maxLength) {
        this.prefix = prefix;
        this.originalSeqsOrderbyLength = originalSeqs;
        this.setMaxLengthOfSeq(maxLength);
        absoluteMaxLength = maxLength;
    }

    public void generateCandidateSequences(int absoluteMaxLength, int curLength){
        if (this.candidateSeqs == null) {
            this.candidateSeqs = new ArrayList<SingleSequenceBase>();
            for (int i = absoluteMaxLength; i >= curLength; i--) {
                if (originalSeqsOrderbyLength.containsKey(i)) {
                    this.candidateSeqs.addAll(originalSeqsOrderbyLength.get(i));
                }
            }
            this.candidateLengthTo = curLength;
        } else {
            for (int i = this.candidateLengthTo - 1; i >= curLength; i--) {
                if (originalSeqsOrderbyLength.containsKey(i)) {
                    this.candidateSeqs.addAll(originalSeqsOrderbyLength.get(i));
                }
                this.candidateLengthTo = curLength;
            }
        }
    }
    public void adjustMaxLengthOfSeq(int minSupport) {
        ArrayList<Integer> allLengthArray = new ArrayList<Integer>(originalSeqsOrderbyLength.keySet());
        Collections.sort(allLengthArray, Collections.reverseOrder());
        int count = 0;
        for (Integer length : allLengthArray) {
            count += originalSeqsOrderbyLength.get(length).size();
            if (count >= minSupport) {
                this.maxLengthOfSeq = length;
                break;
            }
        }
    }
    /**
     * Update sequences under this prefix, check if there is any sequences that
     * is not available, if so, remove that sequence from current list
     *
     * @param availability
     * @return true if there is enough sequence remained under this prefix
     */
    public boolean updateSequences(ArrayList<Boolean> availability, int localSupport) {
        int maxLength = -1;
        HashMap<Integer, ArrayList<SingleSequenceBase>> remainSeqsInMap = new HashMap<Integer, ArrayList<SingleSequenceBase>>();
        int currentSizeSeqs = 0;
        for (Map.Entry<Integer, ArrayList<SingleSequenceBase>> entry : this.originalSeqsOrderbyLength.entrySet()) {
            ArrayList<SingleSequenceBase> remainSeqs = new ArrayList<SingleSequenceBase>();
            for (SingleSequenceBase seq : entry.getValue()) {
                if (seq.checkAvailablity(availability)) {
                    remainSeqs.add(seq);
                    maxLength = Math.max(maxLength, seq.getItemPairList().size());
                } else {
                    if(this.candidateSeqs !=null)
                        this.candidateSeqs.remove(seq);
                }
            } // end for entry.getvalue()
            if (remainSeqs.size() > 0) {
                remainSeqsInMap.put(entry.getKey(), remainSeqs);
                currentSizeSeqs += remainSeqs.size();
            }
        }
        this.originalSeqsOrderbyLength = remainSeqsInMap;
        this.setMaxLengthOfSeq(Math.min(this.maxLengthOfSeq, maxLength));
        if (currentSizeSeqs >= localSupport)
            return true;
        else
            return false;

    }
    public int getMaxLengthOfSeq() {
        return maxLengthOfSeq;
    }
    public void setMaxLengthOfSeq(int maxLengthOfSeq) {
        this.maxLengthOfSeq = maxLengthOfSeq;
    }

    public HashMap<Integer, ArrayList<SingleSequenceBase>> getOriginalSeqsOrderbyLength() {
        return originalSeqsOrderbyLength;
    }

    public void setOriginalSeqsOrderbyLength(HashMap<Integer, ArrayList<SingleSequenceBase>> originalSeqsOrderbyLength) {
        this.originalSeqsOrderbyLength = originalSeqsOrderbyLength;
    }
    public int getNumberOfSingleSequences(){
        int count = 0;
        for(ArrayList<SingleSequenceBase> tempList: originalSeqsOrderbyLength.values()){
            count+= tempList.size();
        }
        return count;
    }

    public ArrayList<SingleSequenceBase> getAllOriginalSequences(){
        ArrayList<SingleSequenceBase> listOfPrefixBase = new ArrayList<>();
        for(ArrayList<SingleSequenceBase> tempList: originalSeqsOrderbyLength.values()){
            listOfPrefixBase.addAll(tempList);
        }
        return listOfPrefixBase;
    }
}
