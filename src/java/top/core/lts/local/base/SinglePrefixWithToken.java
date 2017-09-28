package top.core.lts.local.base;

import top.core.lts.local.datamodel.HashMapCount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by yizhouyan on 7/9/17.
 */
public class SinglePrefixWithToken extends SinglePrefixBase{
    protected HashMapCount<String> tokenFrequency;
    protected int tokenCountLengthTo = Integer.MAX_VALUE;
    public SinglePrefixWithToken(String prefix, HashMap<Integer, ArrayList<SingleSequenceBase>> originalSeqs,
                                 int maxLength) {
        super(prefix, originalSeqs, maxLength);
    }
    public HashSet<String> setupTokenFrequency(int curLength, int minSupport){
        // summarize the frequency of each token, if one token does not appear
        // at least support times, it cannot be in any frequent sequence
        // if the token frequency never appears, init the frequency
        if (this.tokenFrequency == null) {
            this.tokenFrequency = new HashMapCount<String>();
            for (SingleSequenceBase singleSeq : this.candidateSeqs) {
                HashSet<String> tokensInCurStr = singleSeq.getItemSetInString();
                for (String singleToken : tokensInCurStr) {
                    this.tokenFrequency.addObject(singleToken);
                }
            }
            this.tokenCountLengthTo = curLength;
        } else {
            for (SingleSequenceBase singleSeq : this.candidateSeqs) {
                if (singleSeq.getItemPairList().size() < this.tokenCountLengthTo) {
                    HashSet<String> tokensInCurStr = singleSeq.getItemSetInString();
                    for (String singleToken : tokensInCurStr) {
                        this.tokenFrequency.addObject(singleToken);
                    }
                }
            }
            this.tokenCountLengthTo = curLength;
        }
        HashSet<String> frequentTokens = new HashSet<String>();
        for (Map.Entry<String, Integer> singleToken : tokenFrequency.getHashMapForCountResult().entrySet()) {
            if (singleToken.getValue() >= minSupport)
                frequentTokens.add(singleToken.getKey());
        }
        return frequentTokens;
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
                    if (seq.getItemPairList().size() >= this.tokenCountLengthTo) {
                        HashSet<String> tokensInCurStr = seq.getItemSetInString();
                        for (String singleToken : tokensInCurStr) {
                            tokenFrequency.removeObject(singleToken);
                        }
                    }
                }
            } // end for entry.getvalue()
            if (remainSeqs.size() > 0) {
                remainSeqsInMap.put(entry.getKey(), remainSeqs);
                currentSizeSeqs += remainSeqs.size();
            }
        }
        // ArrayList<SingleSequence> remainSeqs = new
        // ArrayList<SingleSequence>();
        this.originalSeqsOrderbyLength = remainSeqsInMap;
        this.setMaxLengthOfSeq(Math.min(this.maxLengthOfSeq, maxLength));
        if (currentSizeSeqs >= localSupport)
            return true;
        else
            return false;
    }
}
