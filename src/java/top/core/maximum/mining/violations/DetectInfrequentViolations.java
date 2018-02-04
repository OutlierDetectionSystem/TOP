package top.core.maximum.mining.violations;

import top.core.datastructure.ItemPair;
import top.core.lts.local.datamodel.HashMapList;

import top.core.lts.local.withviolation.basic.SequencesInvertedIndex;
import top.core.lts.local.withviolation.basic.ViolationSequence;

import top.inputs.InputSequence;
import top.inputs.InputSequenceWithTS;
import top.parameterspace.LocalParameterSpace;
import top.parameterspace.LocalParameterSpaceWithTS;

import java.util.*;
import java.util.Map.Entry;

public class DetectInfrequentViolations {
    protected int maxLength = -1;
    protected InputSequence inputSequence;
    protected LocalParameterSpace localParameterSpace;

    private ViolationDetection detectViolations;
    private HashMap<Integer, HashSet<String>> freqSeqInListOrderByLength;
    private HashMap<String, SinglePrefixWithViolation> prefixSeqArray;

    public DetectInfrequentViolations(InputSequence inputSequence, LocalParameterSpace localParameterSpace,
                                      int violationLS) {
        this.inputSequence = inputSequence;
        this.localParameterSpace = localParameterSpace;
        this.freqSeqInListOrderByLength = new HashMap<>();
        this.detectViolations = new ViolationDetection(violationLS);
    }

    public void Initialization(HashSet<String> globalFrequentPatterns) {
        // add global frequent patterns to the list
        HashSet<String> globalFrequentElements = addGlobalFrequentPatternsToList(globalFrequentPatterns);
        inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        this.prefixSeqArray = new HashMap<String, SinglePrefixWithViolation>();
        for (String currentPrefix : inputSequence.getSingleItems()) {
            generateSeqencesWithPrefix(currentPrefix);
        }
    }
    public HashSet<String> addGlobalFrequentPatternsToList(HashSet<String> globalFrequentPatterns){
        HashSet<String> globalFrequentElements = new HashSet<String>();
        // get global frequent elements
        for(String curFP: globalFrequentPatterns){
            String [] tempStr = curFP.split(",");
            globalFrequentElements.addAll(new ArrayList<String>(Arrays.asList(tempStr)));
            int curLen = tempStr.length;
            if(freqSeqInListOrderByLength.containsKey(curLen)){
                freqSeqInListOrderByLength.get(curLen).add(curFP);
            }else{
                freqSeqInListOrderByLength.put(curLen, new HashSet<String>());
                freqSeqInListOrderByLength.get(curLen).add(curFP);
            }
        }
        return globalFrequentElements;
    }

    protected int prepareSinglePrefixClass(String prefix, HashMapList<Integer, SingleSequence> sequenceOrderbyLength){
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
                sequenceOrderbyLength.addObject(curSeqLength, new SingleSequence(newSeq));
                maxLengthOfString = Math.max(maxLengthOfString, newSeq.size());
                previousSeq.clear();
            }
            previousSeq.add(new ItemPair(inputSequence.getOriginalString().get(i), i));
        }
        if (!previousSeq.isEmpty()) {
            ArrayList<ItemPair> newSeq = cleanedItemPairs(previousSeq);
            int curSeqLength = newSeq.size();
            sequenceOrderbyLength.addObject(curSeqLength, new SingleSequence(newSeq));
            maxLengthOfString = Math.max(maxLengthOfString, newSeq.size());
        }
        return maxLengthOfString;
    }
    /**
     * generate set of sequences from original string
     */
    private void generateSeqencesWithPrefix(String prefix) {
        HashMapList<Integer, SingleSequence> sequenceOrderbyLength =  new HashMapList<Integer, SingleSequence>();
        int maxLengthOfString = prepareSinglePrefixClass(prefix, sequenceOrderbyLength);
        SinglePrefixWithViolation tempPrefix = new SinglePrefixWithViolation(prefix, sequenceOrderbyLength.getHashMapForCountResult(),
                maxLengthOfString);

        this.prefixSeqArray.put(prefix, tempPrefix);
        this.maxLength = Math.max(maxLength, tempPrefix.getMaxLengthOfSeq());
    }

    public ArrayList<ItemPair> cleanedItemPairs(ArrayList<ItemPair> previousSeq) {
        ArrayList<ItemPair> newSeq = new ArrayList<ItemPair>();

        long startTS = ((InputSequenceWithTS)inputSequence).getOriginalTimeStamps().get(previousSeq.get(0).getIndex());
        int startIndex = inputSequence.getOriginalIndexes().get(previousSeq.get(0).getIndex());
        long previousTS = startTS;
        int prevIndex = startIndex;
        newSeq.add(previousSeq.get(0));
        for (int i = 1; i < previousSeq.size(); i++) {
            long currentTS = ((InputSequenceWithTS)inputSequence).getOriginalTimeStamps().get(previousSeq.get(i).getIndex());
            int curIndex = inputSequence.getOriginalIndexes().get(previousSeq.get(i).getIndex());
            LocalParameterSpaceWithTS ts = (LocalParameterSpaceWithTS) localParameterSpace;
            if (currentTS - startTS > ts.getSequenceTimeInterval() + 1
                    || currentTS - previousTS > ts.getItemTimeInterval() + 1
                    || curIndex - startIndex > ts.getSeqGap() + 1
                    || curIndex - prevIndex > ts.getItemGap() + 1)
                break;
            else {
                newSeq.add(previousSeq.get(i));
                prevIndex = curIndex;
                previousTS = currentTS;
            }
        }
        return newSeq;
    }

    public HashMap<String, ViolationSequence> ViolationPatternMining() {
        // if the max length is larger than 0 and there are sequences in the
        // array
        while (this.maxLength > 1 && prefixSeqArray.size() > 0) {
            if(!this.freqSeqInListOrderByLength.containsKey(this.maxLength)) {
                this.maxLength--;
                continue;
            }
            // traverse the arraylist in each prefix and see if there is
            // anything that can be eliminated
            HashMap<String, ArrayList<String>> violationCandidatesCurrentLen = new HashMap<String, ArrayList<String>>();

            for (Entry<String, SinglePrefixWithViolation> currentPrefix : this.prefixSeqArray.entrySet()) {
                currentPrefix.getValue().findFrequentSequences(inputSequence,
                        this.maxLength, localParameterSpace,
                        violationCandidatesCurrentLen, this.detectViolations.getViolationLocalSupport());
            }

            SequencesInvertedIndex buildPrevViolationCandidates = new SequencesInvertedIndex(100,100);
            buildPrevViolationCandidates.addFreqSeqsToList(violationCandidatesCurrentLen.keySet());
            // first detect violations
            detectViolations.detectViolations(violationCandidatesCurrentLen,
                    this.freqSeqInListOrderByLength.get(this.maxLength), buildPrevViolationCandidates);
            this.maxLength--;
        }
        return this.getViolations();
    }


    public HashMap<String, ViolationSequence> getViolations(){
        return this.detectViolations.getViolations();
    }

    public static void main(String[] args) {
        String s= "A|1473902373698,B|1473902433758,C|1473902433815,A|1473902433843,B|1473902485507,C|1473902485507,A|1473902485507,C|1473902485512,B|1473902485512,C|1473902485532," +
                "A|1473902485574,C|1473902485628,B|1473902485633,A|1473902486252,C|1473902487718,D|1473902487072";
        int localSupport = 3;
        int itemGap = 0;
        int seqGap = 10;
        long itemInterval = 10000;
        long sequenceInterval = 60000;
        int violationSupport = 1;
        String[] globalFE = { "C,A,B"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        LocalParameterSpace localParameterSpace = new LocalParameterSpaceWithTS(localSupport, itemGap, seqGap, itemInterval, sequenceInterval);
        InputSequence inputSequence = new InputSequenceWithTS(s);
        DetectInfrequentViolations localFS = new DetectInfrequentViolations(inputSequence, localParameterSpace, violationSupport);
        localFS.Initialization(globalFrequentElements);

        HashMap<String, ViolationSequence> violations = localFS.ViolationPatternMining();

        for(Entry<String, ViolationSequence> entry: violations.entrySet()){
            System.out.println(entry.getValue().printViolation());
            System.out.println();
        }

    }
}
