package top.core.lts.local.withviolation.nots;

import top.core.lts.local.base.LocalFSDetectionBase;
import top.core.lts.local.base.SingleSequenceBase;
import top.core.lts.local.datamodel.HashMapList;
import top.core.lts.local.withviolation.basic.SequencesInvertedIndex;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;
import java.util.Map.Entry;

public class LocalFSDetectionWithViolation extends LocalFSDetectionBase{
    protected ViolationDetection detectViolations;
    protected SequencesInvertedIndex freqSeqInList;
    protected HashMap<String, SinglePrefixWithViolation> prefixSeqArray;

    public LocalFSDetectionWithViolation(InputSequence inputSequence, LocalParameterSpace localParameterSpace, int violationLS) {
        super(localParameterSpace, inputSequence);
        this.freqSeqInList = new SequencesInvertedIndex(100, 100);
        this.detectViolations = new ViolationDetection(violationLS);
    }

    public void Initialization(HashSet<String> globalFrequentElements) {
        inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        this.prefixSeqArray = new HashMap<String, SinglePrefixWithViolation>();
        for (String currentPrefix : inputSequence.getSingleItems()) {
            generateSeqencesWithPrefix(currentPrefix);
        }
    }
    /**
     * generate set of sequences from original string
     */
    protected void generateSeqencesWithPrefix(String prefix) {
        HashMapList<Integer, SingleSequenceBase> sequenceOrderbyLength =  new HashMapList<Integer, SingleSequenceBase>();
        int maxLengthOfString = prepareSinglePrefixClass(prefix, sequenceOrderbyLength);
        SinglePrefixWithViolation tempPrefix = new SinglePrefixWithViolation(prefix, sequenceOrderbyLength.getHashMapForCountResult(),
                maxLengthOfString);
        this.prefixSeqArray.put(prefix, tempPrefix);
        this.maxLength = Math.max(maxLength, tempPrefix.getMaxLengthOfSeq());
    }

    public ArrayList<String> LocalFrequentSequenceMining(int indexOfInput) {
        // if the max length is larger than 0 and there are sequences in the
        // array

        while (this.maxLength > 1 && prefixSeqArray.size() > 0) {
            // traverse the arraylist in each prefix and see if there is
            // anything that can be eliminated
            HashSet<Integer> eliminateIndexes = new HashSet<Integer>();
            HashSet<String> violationCandidatesCurrentLen = new HashSet<String>();
            HashMap<String, Integer> currentFrequentSequences = new HashMap<String, Integer>();
            for (Entry<String, SinglePrefixWithViolation> currentPrefix : this.prefixSeqArray.entrySet()) {
                currentPrefix.getValue().findFrequentSequences(inputSequence, eliminateIndexes,
                        currentFrequentSequences, this.maxLength, localParameterSpace,
                        violationCandidatesCurrentLen, this.detectViolations.getViolationLocalSupport());
            }

            SequencesInvertedIndex buildPrevViolationCandidates = new SequencesInvertedIndex(100,100);
            buildPrevViolationCandidates.addFreqSeqsToList(violationCandidatesCurrentLen);
            // first detect violations
            detectViolations.detectViolations(this.freqSeqInList, violationCandidatesCurrentLen,
                     currentFrequentSequences.keySet(), buildPrevViolationCandidates);

            // then update those information for violations
            this.freqSeqInList.addFreqSeqsToList(currentFrequentSequences.keySet());

            if (eliminateIndexes.size() > 0) {
                for (Integer i : eliminateIndexes) {
                    inputSequence.getAvailable().set(i, false);
                }
                // then update each array, delete those strings that has nothing
                // available
                ArrayList<String> deletePrefix = new ArrayList<String>();
                for (Entry<String, SinglePrefixWithViolation> currentPrefix : this.prefixSeqArray.entrySet()) {
                    if (!currentPrefix.getValue().updateSequences(inputSequence.getAvailable(), localParameterSpace.getMinLocalSupport()))
                        deletePrefix.add(currentPrefix.getKey());
                }
                for (String str : deletePrefix) {
                    this.prefixSeqArray.remove(str);
                }
            }
            this.maxLength--;
        }
        return this.freqSeqInList.getSequencesInList();
    }

    public HashMap<String, Set<String>> getViolations(){
        return this.detectViolations.getViolations();
    }

    public static void main(String[] args) {
		 String s = "A,B,C,A,B,C,A,C,B,A,C,D";
//        String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
//        String s = "A,B,C,D,A,B,C,A,B,C,C,A,D,D,D";
//        String s = "2,1,51,4,51,4,51,4,4,4,51,4,51,4,6,47,47,40,24,30,29,23,29,23,24,23,24,23,24,23,29,30,22,21,22,21,22,21,20,21,13,21,20,21,22,34,22,21,22,20,30,34,33,34,30,29,30,32,31,32,31,32,31,32,28,49,46,42,44";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 10;
        int violationSupport = 1;
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetectionWithViolation localFS = new LocalFSDetectionWithViolation(inputSequence, localParameterSpace,violationSupport);
        String[] globalFE = {"A", "B", "C","D"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        localFS.Initialization(globalFrequentElements);

        ArrayList<String> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        System.out.println("Frequent Sequence: ");
        for (String entry : freqSeqRes) {
            System.out.println(entry);
        }
        HashMap<String, Set<String>> violations = localFS.getViolations();
        for(Entry<String, Set<String>> entry: violations.entrySet()){
            System.out.println("Sequence: " + entry.getKey());
            for(String str: entry.getValue())
                System.out.print(str + "\t");
            System.out.println();
        }

    }
}
