package hywt.maplemandel;

public class Parcel<K, V>  {
    public final K key;
    public final V value;

    public Parcel(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
