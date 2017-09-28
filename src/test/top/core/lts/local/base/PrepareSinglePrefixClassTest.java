package top.core.lts.local.base;

import org.junit.Test;
import top.core.lts.local.datamodel.HashMapList;
import top.core.lts.local.noviolation.LocalFSDetection;
import top.core.lts.local.noviolation.SinglePrefixClass;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by yizhouyan on 9/20/17.
 */
public class PrepareSinglePrefixClassTest {
    @Test
    public void prepareSinglePrefixClass_A() throws Exception {
        String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.getInputSequence().removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        HashMapList<Integer, SingleSequenceBase> actualSequenceOrderbyLength = new HashMapList<>();
        localFS.prepareSinglePrefixClass("A", actualSequenceOrderbyLength);
        for(Map.Entry<Integer, ArrayList<SingleSequenceBase>> entry: actualSequenceOrderbyLength.getHashMapForCountResult().entrySet()){
            for(SingleSequenceBase single: entry.getValue()) {
                System.out.println(entry.getKey() + "|" + single.getSequenceInStringWithIndex());
            }
        }
    }

    @Test
    public void prepareSinglePrefixClass_B_Gap() throws Exception {
        String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
        int localSupport = 2;
        int itemGap = 1;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.getInputSequence().removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        HashMapList<Integer, SingleSequenceBase> actualSequenceOrderbyLength = new HashMapList<>();
        localFS.prepareSinglePrefixClass("B", actualSequenceOrderbyLength);
        for(Map.Entry<Integer, ArrayList<SingleSequenceBase>> entry: actualSequenceOrderbyLength.getHashMapForCountResult().entrySet()){
            for(SingleSequenceBase single: entry.getValue()) {
                System.out.println(entry.getKey() + "|" + single.getSequenceInStringWithIndex());
            }
        }
    }

    @Test
    public void prepareSinglePrefixClass_C() throws Exception {
        String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
        int localSupport = 1;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.getInputSequence().removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        HashMapList<Integer, SingleSequenceBase> actualSequenceOrderbyLength = new HashMapList<>();
        localFS.prepareSinglePrefixClass("C", actualSequenceOrderbyLength);
        for(Map.Entry<Integer, ArrayList<SingleSequenceBase>> entry: actualSequenceOrderbyLength.getHashMapForCountResult().entrySet()){
            for(SingleSequenceBase single: entry.getValue()) {
                System.out.println(entry.getKey() + "|" + single.getSequenceInStringWithIndex());
            }
        }
    }

    @Test
    public void prepareSinglePrefixClass_ABABABAB() throws Exception {
        String s = "A,B,C,A,B,C,A,B,A,B";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.getInputSequence().removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        HashMapList<Integer, SingleSequenceBase> actualSequenceOrderbyLength = new HashMapList<>();
        localFS.prepareSinglePrefixClass("A", actualSequenceOrderbyLength);
        for(Map.Entry<Integer, ArrayList<SingleSequenceBase>> entry: actualSequenceOrderbyLength.getHashMapForCountResult().entrySet()){
            for(SingleSequenceBase single: entry.getValue()) {
                System.out.println(entry.getKey() + "|" + single.getSequenceInStringWithIndex());
            }
        }
    }

    @Test
    public void prepareSinglePrefixClass_ABABCABC() throws Exception {
        String s = "A,B,A,B,C,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.getInputSequence().removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, localParameterSpace.getMinLocalSupport());
        HashMapList<Integer, SingleSequenceBase> actualSequenceOrderbyLength = new HashMapList<>();
        localFS.prepareSinglePrefixClass("A", actualSequenceOrderbyLength);
        for(Map.Entry<Integer, ArrayList<SingleSequenceBase>> entry: actualSequenceOrderbyLength.getHashMapForCountResult().entrySet()){
            for(SingleSequenceBase single: entry.getValue()) {
                System.out.println(entry.getKey() + "|" + single.getSequenceInStringWithIndex());
            }
        }
    }
}