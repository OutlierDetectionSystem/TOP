package top.core.lts.local.base;

import top.core.datastructure.ItemPair;
import top.core.lts.local.datamodel.HashMapList;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.ArrayList;

/**
 * Created by yizhouyan on 7/9/17.
 */
public class LocalFSDetectionBase {
    protected int maxLength = -1;
    protected InputSequence inputSequence;
    protected LocalParameterSpace localParameterSpace;

    public LocalFSDetectionBase(LocalParameterSpace localParameterSpace, InputSequence inputSequence) {
        this.inputSequence = inputSequence;
        this.localParameterSpace = localParameterSpace;
    }

    protected int prepareSinglePrefixClass(String prefix, HashMapList<Integer, SingleSequenceBase> sequenceOrderbyLength){
        ArrayList<ItemPair> previousSeq = new ArrayList<ItemPair>();
        int startIndex = 0;
        // first search for starting index
        for (int i = 0; i < inputSequence.getOriginalString().size(); i++) {
            if (inputSequence.getOriginalString().get(i).equals(prefix)) {
                startIndex = i;
                break;
            }
        }
        int maxLengthOfString = -1;

        for (int i = startIndex; i < inputSequence.getOriginalString().size(); i++) {
            if (inputSequence.getOriginalString().get(i).equals(prefix) && previousSeq.size() != 0) {
                // save previous sequence to the final sequence array
                int tempInc = i;
                while(previousSeq.size() < localParameterSpace.getSeqGap() + 2 &&
                        tempInc < inputSequence.getOriginalString().size()){
                    previousSeq.add(new ItemPair(inputSequence.getOriginalString().get(tempInc), tempInc));
                    tempInc++;
                }
                ArrayList<ItemPair> newSeq = cleanedItemPairs(previousSeq);
                int curSeqLength = newSeq.size();
                sequenceOrderbyLength.addObject(curSeqLength, new SingleSequenceBase(newSeq));
                maxLengthOfString = Math.max(maxLengthOfString, newSeq.size());
                previousSeq.clear();
            }
            // save the original string and index in originalString/
            // originalIndexes and available, if further index is needed, then
            // need to find the corresponding index
            previousSeq.add(new ItemPair(inputSequence.getOriginalString().get(i), i));
        }
        if (!previousSeq.isEmpty()) {
            ArrayList<ItemPair> newSeq = cleanedItemPairs(previousSeq);
            int curSeqLength = newSeq.size();
            sequenceOrderbyLength.addObject(curSeqLength, new SingleSequenceBase(newSeq));
            maxLengthOfString = Math.max(maxLengthOfString, newSeq.size());
        }
        return maxLengthOfString;
    }

    protected ArrayList<ItemPair> cleanedItemPairs(ArrayList<ItemPair> previousSeq) {
        ArrayList<ItemPair> newSeq = new ArrayList<ItemPair>();
        int startIndex = inputSequence.getOriginalIndexes().get(previousSeq.get(0).getIndex());
        int previousIndex = startIndex;
        newSeq.add(previousSeq.get(0));
        for (int i = 1; i < previousSeq.size(); i++) {
            int currentIndex = inputSequence.getOriginalIndexes().get(previousSeq.get(i).getIndex());
            if (currentIndex - startIndex > localParameterSpace.getSeqGap() + 1 || currentIndex - previousIndex > localParameterSpace.getItemGap() + 1)
                break;
            else {
                newSeq.add(previousSeq.get(i));
                previousIndex = currentIndex;
            }
        }
        return newSeq;
    }

    public InputSequence getInputSequence() {
        return inputSequence;
    }

    public void setInputSequence(InputSequence inputSequence) {
        this.inputSequence = inputSequence;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
