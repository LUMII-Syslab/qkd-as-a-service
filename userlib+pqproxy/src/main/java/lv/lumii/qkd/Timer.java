package lv.lumii.qkd;

public class Timer {

    public interface StopCondition {
        boolean fulfilled(long msElapsed);
    }

    private final StopCondition condition;

    public Timer(StopCondition condition) {
        this.condition = condition;
    }

    private long nowMs() {
        // return System.currentTimeMillis();
        return System.nanoTime() / 1000000;
    }

    public void startAndWait() {
        long time0 = nowMs();
        long msElapsed = 0;

        while (!condition.fulfilled(msElapsed)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            msElapsed = nowMs()-time0;
        }
    }
}
