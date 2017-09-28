package top.core.lts.local.datamodel;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class HashMapList<K,V> {
    private HashMap<K, ArrayList<V>> hashMapList;

    public HashMapList(){
        this.hashMapList = new HashMap<>();
    }

    public HashMap<K, ArrayList<V>> getHashMapForCountResult(){
        return hashMapList;
    }

    public void addObject(K key, V value){
        if(hashMapList.containsKey(key)){
            hashMapList.get(key).add(value);
        }else {
            hashMapList.put(key, new ArrayList<V>());
            hashMapList.get(key).add(value);
        }
    }
}
