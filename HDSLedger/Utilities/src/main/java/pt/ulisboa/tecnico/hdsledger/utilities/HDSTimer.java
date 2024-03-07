package pt.ulisboa.tecnico.hdsledger.utilities;

public class HDSTimer {
    public State getState() {
        return state;
    }

    public enum State {
        RUNNING,
        EXPIRED
    }

    private State state;
    public HDSTimer(long durationMillis) {
        state = State.RUNNING;
        Thread timerThread = new Thread(() -> {
            try {
                Thread.sleep(durationMillis);
                synchronized (this) {
                    state = State.EXPIRED;
                    notify();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timerThread.start();
    }

    public synchronized void waitExpiration() throws InterruptedException {
        while (!(state == State.EXPIRED)) {
            wait();
        }
    }
}
