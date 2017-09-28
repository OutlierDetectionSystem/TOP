package top.inputs;

import org.junit.Test;
import top.core.lts.local.noviolation.LocalFSDetection;
import top.core.lts.local.noviolation.SinglePrefixClass;
import top.parameterspace.LocalParameterSpace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * Created by yizhouyan on 9/20/17.
 */
public class InputSequenceTest {
    @Test
    public void removeInitSeqsItemIncludeGlobalInFreqRemoveLess() throws Exception {
        String s = "A,A,B,C,D,A,B,C";
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, 2);
        ArrayList<String> expectedElements = new ArrayList<>();
        expectedElements.add("A");
        expectedElements.add("B");
        expectedElements.add("C");
        assertEquals(expectedElements, inputSequence.getSingleItems());
        ArrayList expectedOriginal = new ArrayList(Arrays.asList("A,A,B,C,A,B,C".split(",")));
        assertEquals(expectedOriginal, inputSequence.getOriginalString());
    }

    @Test
    public void removeInitSeqsItemIncludeGlobalInFreqAll() throws Exception {
        String s = "A,A,B,C,D,A,B,C";
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, 1);
        ArrayList<String> expectedElements = new ArrayList<>();
        expectedElements.add("D");
        expectedElements.add("A");
        expectedElements.add("B");
        expectedElements.add("C");
        assertEquals(expectedElements, inputSequence.getSingleItems());
    }

    @Test
    public void removeInitSeqsItemIncludeGlobalInFreqRemoveNonGlobal() throws Exception {
        String s = "A,A,B,C,D,A,B,C";
        String[] globalFE = { "A", "B", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        inputSequence.removeInitSeqsItemIncludeGlobalInFreq(globalFrequentElements, 2);
        ArrayList<String> expectedElements = new ArrayList<>();
        expectedElements.add("A");
        expectedElements.add("B");
        assertEquals(expectedElements, inputSequence.getSingleItems());
    }

}