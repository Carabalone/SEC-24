package pt.ulisboa.tecnico.hdsledger.utilities;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class HDSTimer {
    public enum State {
        STOPPED, RUNNING, EXPIRED
    }

    private State currentState = State.STOPPED;
    private int exponent;
    private long durationMillis;
    private Timer timer;
    private int consensusInstance = 0;
    private ConcurrentHashMap<String, TimerListener> subscribers = new ConcurrentHashMap<>();

    public HDSTimer() {
    }

    public HDSTimer(int consensusInstance) {
        this.consensusInstance = consensusInstance;
    }

    public void startOrRestart(int round) {
      if (currentState == State.RUNNING) {
        this.stop();
      }
      start(round);
    }

    public void start(int n) {
        this.exponent = n;
        this.durationMillis = (long) Math.pow(2, n) * 1000; // Convert seconds to milliseconds

        currentState = State.RUNNING;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentState = State.EXPIRED;
                notifySubscribers();
                timer.cancel();
            }
        }, durationMillis);
    }

    public void stop() {
        if (currentState == State.RUNNING) {
            timer.cancel();
            currentState = State.STOPPED;
        }
    }

    public State getState() {
        return currentState;
    }

    public void subscribe(String id, TimerListener listener) {
        subscribers.put(id, listener);
    }

    public void unsubscribe(String id) {
        subscribers.remove(id);
    }

    private void notifySubscribers() {
        for (TimerListener listener : subscribers.values()) {
            listener.onTimerExpired();
        }
    }

    public interface TimerListener {
        void onTimerExpired();
    }
}
