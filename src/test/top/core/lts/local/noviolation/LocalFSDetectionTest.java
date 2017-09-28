package top.core.lts.local.noviolation;

import org.junit.Test;
import top.inputs.InputSequence;
import top.parameterspace.LocalParameterSpace;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by yizhouyan on 9/20/17.
 */
public class LocalFSDetectionTest {
    @Test
    public void InitilizationTest() throws Exception {
        String s = "A,A,B,C,D,A,B,C,C,B,A,C,B,A,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C", "D" };
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, SinglePrefixClass> results = localFS.getPrefixSeqArray();
        assertEquals(3, results.size());
        assertEquals(7, results.get("A").getMaxLengthOfSeq());
        assertEquals(2, results.get("A").getOriginalSeqsOrderbyLength().get(3).size());
        assertEquals(7, localFS.getMaxLength());
    }

    @Test
    public void FrequentSequenceTest_AB() throws Exception {
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
        assertEquals(4, localFS.getPrefixSeqArray().get("A").getNumberOfSingleSequences());
        assertEquals(6, localFS.getMaxLength());
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,A,B",2);
        assertEquals(expectedRes, freqSeqRes);
    }

    @Test
    public void FrequentSequenceTest_ABC() throws Exception {
        String s = "A,B,A,B,C,A,B,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,A,B,C",2);
        assertEquals(expectedRes, freqSeqRes);
    }

    @Test
    public void FrequentSequenceTest_ABCD() throws Exception {
        String s = "A,B,C,D,A,B,C,A,B,C,D,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C", "D"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,C,D,A,B,C",2);
        assertEquals(expectedRes, freqSeqRes);
    }

    @Test
    public void FrequentSequenceTest_ABC_2() throws Exception {
        String s = "A,B,A,B,C,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,C",2);
        assertEquals(expectedRes, freqSeqRes);
    }

    @Test
    public void FrequentSequenceTest_ABC_AA() throws Exception {
        String s = "A,A,B,A,A,B,C,A,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,A,B,C",2);
        assertEquals(expectedRes, freqSeqRes);
    }

    @Test
    public void FrequentSequenceTest_ABC_3() throws Exception {
        String s = "A,A,B,A,A,B,A,A,B,A,A,B,A,A,B,A,A,B";
        int localSupport = 3;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,A,B,A,A,B",3);
        assertEquals(expectedRes, freqSeqRes);
    }

    @Test
    public void FrequentSequenceTest_testInPaper() throws Exception {
        String s = "A,B,C,D,A,B,C,A,C,B,E,A,C";
        int localSupport = 2;
        int itemGap = 1;
        int seqGap = 2;
        String[] globalFE = { "A", "B", "C", "D"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        InputSequence inputSequence = new InputSequence(s);
        LocalParameterSpace localParameterSpace = new LocalParameterSpace(localSupport, itemGap, seqGap);
        LocalFSDetection localFS = new LocalFSDetection(localParameterSpace, inputSequence);
        localFS.Initialization(globalFrequentElements);
        HashMap<String, Integer> freqSeqRes = localFS.LocalFrequentSequenceMining(0);
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,C",2);
        expectedRes.put("C,A,B",2);
        expectedRes.put("B,C,A",2);
        expectedRes.put("B,A,C",2);
        assertEquals(expectedRes, freqSeqRes);
    }
}