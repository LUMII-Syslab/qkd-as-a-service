package lv.lumii.qkd;

public class Timer {

    public interface StopCondition {
        boolean fulfilled(long msElapsed);
    }

    private final StopCondition condition;

    public Timer(StopCondition condition) {
        this.condition = condition;
    }

    public void startAndWait() {
        long time0 = System.currentTimeMillis();
        long msElapsed = 0;

        while (!condition.fulfilled(msElapsed)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            msElapsed = System.currentTimeMillis()-time0;
        }
    }
}
