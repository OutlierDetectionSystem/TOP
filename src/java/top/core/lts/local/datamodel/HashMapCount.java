package top.core.lts.local.datamodel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yizhouyan on 7/8/17.
 */
public class HashMapCount<K> {
    private HashMap<K, Integer> hashMapForCount;

    public HashMapCount(){
        this.hashMapForCount = new HashMap<>();
    }

    public HashMap<K, Integer> getHashMapForCountResult(){
        return hashMapForCount;
    }

    public void addObject(K key){
        if(hashMapForCount.containsKey(key)){
            hashMapForCount.put(key, hashMapForCount.get(key).intValue()+1);
        }else
            hashMapForCount.put(key, 1);
    }

    public void removeObject(K key){
        if(hashMapForCount.containsKey(key))
            hashMapForCount.put(key, hashMapForCount.get(key).intValue()-1);
    }

    public static void main(String [] args){
        HashMapCount<String> hashMapCount = new HashMapCount<String>();
        hashMapCount.addObject("10");
        hashMapCount.addObject("2");
        hashMapCount.addObject("10");
        HashMap<String, Integer> results = hashMapCount.getHashMapForCountResult();
        for(Map.Entry<String, Integer> entry: results.entrySet()){
            System.out.println(entry.getKey() + "," + entry.getValue());
        }
    }
}
