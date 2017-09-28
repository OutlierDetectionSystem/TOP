package top.core.lts.local.base;

import org.junit.Test;
import top.core.datastructure.ItemPair;
import top.core.lts.local.datamodel.HashMapList;
import top.core.lts.local.noviolation.LocalFSDetection;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by yizhouyan on 9/20/17.
 */
public class SingleSequenceBaseTest {
    @Test
    public void compareToTest() throws Exception {
        ArrayList<ItemPair> set1 = new ArrayList<>();
        set1.add(new ItemPair("A",1));
        set1.add(new ItemPair("B",2));
        SingleSequenceBase s1 = new SingleSequenceBase(set1);
        ArrayList<ItemPair> set2 = new ArrayList<>();
        set2.add(new ItemPair("A",2));
        set2.add(new ItemPair("B",3));
        SingleSequenceBase s2 = new SingleSequenceBase(set2);
        ArrayList<SingleSequenceBase> sequenceSet = new ArrayList<>();
        sequenceSet.add(s1);
        sequenceSet.add(s2);
        Collections.sort(sequenceSet);
        assertEquals(s1, sequenceSet.get(0));
    }

    @Test
    public void prepareSinglePrefixClass_ABABCABC() throws Exception {
        String s = "A,B,A,B,A,B,A,B";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        ArrayList<SingleSequenceBase> sequences = new ArrayList<SingleSequenceBase>(localFS.getPrefixSeqArray().get("A").getAllOriginalSequences());
        for(SingleSequenceBase single: sequences) {
            System.out.println(single.getSequenceInStringWithIndex());
        }
        Collections.sort(sequences);
        for(SingleSequenceBase single: sequences) {
            System.out.println(single.getSequenceInStringWithIndex());
        }
    }
}