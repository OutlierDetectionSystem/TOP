package top.distributed.stl;

import org.apache.hadoop.mapreduce.Reducer;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by yizhouyan on 9/22/17.
 */
public class SingleSequenceMiningTest {
    @Test
    public void SingleSequenceMining_1() throws Exception {
         String s = "A,B,C,D,A,B,C,A,C,B,E,A,C";
        int localSupport = 2;
        int itemGap = 1;
        int seqGap = 2;
        SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
        String[] globalFE = { "A", "B", "C"};
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(0, globalFrequentElements, null);
        HashMap<String, Integer>  freqSeqs = obj.getFrequentSequences();
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,C",2);
        expectedRes.put("C,A,B",2);
        expectedRes.put("B,C,A",2);
        expectedRes.put("B,A,C",2);
        assertEquals(expectedRes, freqSeqs);
    }

    @Test
    public void SingleSequenceMining_2() throws Exception {
        String s = "A,A,B,A,A,B,A,A,B,A,A,B,A,A,B,A,A,B";
        int localSupport = 3;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B"};
        SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(0, globalFrequentElements, null);
        HashMap<String, Integer>  freqSeqs = obj.getFrequentSequences();
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,A,B,A,A,B",3);
        assertEquals(expectedRes, freqSeqs);
    }

    @Test
    public void SingleSequenceMining_3() throws Exception {
        String s = "A,A,B,A,A,B,C,A,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C"};
        SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(0, globalFrequentElements, null);
        HashMap<String, Integer>  freqSeqs = obj.getFrequentSequences();
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,A,B,C",2);;
        assertEquals(expectedRes, freqSeqs);
    }

    @Test
    public void SingleSequenceMining_4() throws Exception {
        String s = "A,B,A,B,C,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C"};
        SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(0, globalFrequentElements, null);
        HashMap<String, Integer>  freqSeqs = obj.getFrequentSequences();
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,C",2);
        assertEquals(expectedRes, freqSeqs);
    }

    @Test
    public void SingleSequenceMining_5() throws Exception {
        String s = "A,B,C,D,A,B,C,A,B,C,D,A,B,C";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B", "C", "D"};
        SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(0, globalFrequentElements, null);
        HashMap<String, Integer>  freqSeqs = obj.getFrequentSequences();
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,C,D,A,B,C",2);
        assertEquals(expectedRes, freqSeqs);
    }

    @Test
    public void SingleSequenceMining_6() throws Exception {
        String s = "A,B,A,B,A,B,A,B";
        int localSupport = 2;
        int itemGap = 0;
        int seqGap = 5;
        String[] globalFE = { "A", "B"};
        SingleSequenceMining obj = new SingleSequenceMining(s, localSupport, itemGap, seqGap);
        HashSet<String> globalFrequentElements = new HashSet(Arrays.asList(globalFE));
        obj.findFreqSeqInOneString(0, globalFrequentElements, null);
        HashMap<String, Integer>  freqSeqs = obj.getFrequentSequences();
        HashMap<String, Integer> expectedRes = new HashMap<>();
        expectedRes.put("A,B,A,B",2);
        assertEquals(expectedRes, freqSeqs);
    }

}