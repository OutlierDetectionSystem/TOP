package top.core.datastructure;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * Created by yizhouyan on 9/22/17.
 */
public class SequenceTest {
    @Test
    public void compareTo() throws Exception {
        ArrayList<ItemPair> set1 = new ArrayList<>();
        set1.add(new ItemPair("A",1));
        set1.add(new ItemPair("B",2));
        Sequence s1 = new Sequence(set1);
        ArrayList<ItemPair> set2 = new ArrayList<>();
        set2.add(new ItemPair("A",2));
        set2.add(new ItemPair("B",3));
        Sequence s2 = new Sequence(set2);
        ArrayList<Sequence> sequenceSet = new ArrayList<>();
        sequenceSet.add(s1);
        sequenceSet.add(s2);
        Collections.sort(sequenceSet);
        assertEquals(s1, sequenceSet.get(0));
    }

}