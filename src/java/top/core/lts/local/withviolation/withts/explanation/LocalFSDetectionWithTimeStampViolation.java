package top.core.lts.local.withviolation.withts.explanation;

import top.core.datastructure.ItemPair;
import top.core.lts.local.base.*;
import top.core.lts.local.datamodel.HashMapList;
import top.core.lts.local.withviolation.basic.SequencesInvertedIndex;
import top.core.lts.local.withviolation.basic.ViolationSequence;
import top.inputs.InputSequence;
import top.inputs.InputSequenceWithTS;
import top.parameterspace.LocalParameterSpace;
import top.parameterspace.LocalParameterSpaceWithTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class LocalFSDetectionWithTimeStampViolation extends LocalFSDetectionBase {
    private ViolationDetection detectViolations;
    private SequencesInvertedIndex freqSeqInList;
    private HashMap<String, SinglePrefixWithViolationExplanation> prefixSeqArray;

    public LocalFSDetectionWithTimeStampViolation(InputSequence inputSequence, LocalParameterSpace localParameterSpace,
                                                  int violationLS) {
        super(localParameterSpace, inputSequence);
        this.freqSeqInList = new SequencesInvertedIndex(100, 100);
        this.detectViolations = new ViolationDetection(violationLS);
    }

    public void Initialization(HashSet<String> globalFrequentElements) {
        inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        this.prefixSeqArray = new HashMap<String, SinglePrefixWithViolationExplanation>();
        for (String currentPrefix : inputSequence.getSingleItems()) {
            generateSeqencesWithPrefix(currentPrefix);
        }
    }

    /**
     * generate set of sequences from original string
     */
    private void generateSeqencesWithPrefix(String prefix) {
        HashMapList<Integer, SingleSequenceBase> sequenceOrderbyLength =  new HashMapList<Integer, SingleSequenceBase>();
        int maxLengthOfString = prepareSinglePrefixClass(prefix, sequenceOrderbyLength);
        SinglePrefixWithViolationExplanation tempPrefix = new SinglePrefixWithViolationExplanation(prefix, sequenceOrderbyLength.getHashMapForCountResult(),
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

    public ArrayList<String> LocalFrequentSequenceMining(int indexOfInput) {
        // if the max length is larger than 0 and there are sequences in the
        // array
        while (this.maxLength > 1 && prefixSeqArray.size() > 0) {
            // traverse the arraylist in each prefix and see if there is
            // anything that can be eliminated
            HashSet<Integer> eliminateIndexes = new HashSet<Integer>();
            HashMap<String, ArrayList<String>> violationCandidatesCurrentLen = new HashMap<String, ArrayList<String>>();
            HashMap<String, Integer> currentFrequentSequences = new HashMap<String, Integer>();
            for (Entry<String, SinglePrefixWithViolationExplanation> currentPrefix : this.prefixSeqArray.entrySet()) {
                currentPrefix.getValue().findFrequentSequences(inputSequence,
                        eliminateIndexes, currentFrequentSequences, this.maxLength, localParameterSpace,
                        violationCandidatesCurrentLen, this.detectViolations.getViolationLocalSupport());
            }

            SequencesInvertedIndex buildPrevViolationCandidates = new SequencesInvertedIndex(100,100);
            buildPrevViolationCandidates.addFreqSeqsToList(violationCandidatesCurrentLen.keySet());
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
                for (Entry<String, SinglePrefixWithViolationExplanation> currentPrefix : this.prefixSeqArray.entrySet()) {
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


    public HashMap<String, ViolationSequence> getViolations(){
        return this.detectViolations.getViolations();
    }

    public static void main(String[] args) {
//		 String s = "A,B,C,A,B,C,A,C,B,A,C,D";
//        String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
//        String s = "A,B,C,D,A,B,C,A,B,C,C,A,D,D,D";
//        String s = "A|2017-02-05 02:19:01.038,B|2017-02-05 02:19:01.038,C|2017-02-05 02:19:01.217,A|2017-02-05 02:20:00.988," +
//                "B|2017-02-05 02:20:01.157,C|2017-02-05 02:20:01.115,A|2017-02-05 02:21:01.293,C|2017-02-05 02:21:02.332," +
//                "B|2017-02-05 02:21:02.332,A|2017-02-05 02:21:02.934,C|2017-02-05 02:21:03.332,D|2017-02-05 02:21:03.332," +
//                "A|2017-02-05 02:19:01.038,B|2017-02-05 02:19:01.038,C|2017-02-05 02:19:01.217,A|2017-02-05 02:20:00.988," +
//                "B|2017-02-05 02:20:01.157,C|2017-02-05 02:20:01.115,A|2017-02-05 02:21:01.293,C|2017-02-05 02:21:02.332," +
//                "B|2017-02-05 02:21:02.332,A|2017-02-05 02:21:02.934,C|2017-02-05 02:21:03.332,D|2017-02-05 02:21:03.332";
        String s= "A|1473902373698,B|1473902433758,C|1473902433815,A|1473902433843,B|1473902485507,C|1473902485507,A|1473902485507,C|1473902485512,B|1473902485512,C|1473902485532," +
                "A|1473902485574,C|1473902485628,B|1473902485633,A|1473902486252,C|1473902487718,D|1473902487072";
        int localSupport = 3;
        int itemGap = 0;
        int seqGap = 10;
        long itemInterval = 10000;
        long sequenceInterval = 60000;
        int violationSupport = 2;
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        LocalParameterSpace localParameterSpace = new LocalParameterSpaceWithTS(localSupport, itemGap, seqGap, itemInterval, sequenceInterval);
        InputSequence inputSequence = new InputSequenceWithTS(s);
        LocalFSDetectionWithTimeStampViolation localFS = new LocalFSDetectionWithTimeStampViolation(inputSequence, localParameterSpace, violationSupport);
        localFS.Initialization(globalFrequentElements);

        ArrayList<String> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        System.out.println("Frequent Sequence: ");
        for (String entry : freqSeqRes) {
            System.out.println(entry);
        }
        HashMap<String, ViolationSequence> violations = localFS.getViolations();
        for(Entry<String, ViolationSequence> entry: violations.entrySet()){
            System.out.println(entry.getValue().printViolation());
            System.out.println();
        }

    }
}
