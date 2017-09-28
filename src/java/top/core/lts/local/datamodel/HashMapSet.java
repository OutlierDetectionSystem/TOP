package top.core.lts.local.datamodel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class HashMapSet<K,V> {
    private HashMap<K, HashSet<V>> hashMapSet;

    public HashMapSet(){
        this.hashMapSet = new HashMap<>();
    }

    public HashMap<K, HashSet<V>> getHashMapSetResult(){
        return hashMapSet;
    }

    public void addSets(K key, ArrayList<V> value){
        if(hashMapSet.containsKey(key)){
            hashMapSet.get(key).addAll(value);
        }else {
            hashMapSet.put(key, new HashSet<V>());
            hashMapSet.get(key).addAll(value);
        }
    }
}
