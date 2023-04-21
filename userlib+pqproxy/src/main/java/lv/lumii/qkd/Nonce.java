package lv.lumii.qkd;

public class Nonce {

    private long lastValue;

    public Nonce() {
        lastValue = 0;
    }

    public synchronized long nextValue() {
        return ++lastValue;
    }
}
