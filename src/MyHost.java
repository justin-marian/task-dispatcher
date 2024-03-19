import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.System.currentTimeMillis;

public class MyHost extends Host {
    private Task task;
    private final Lock mutex;
    private final List<Task> queue;
    private final AtomicBoolean isRunning;
    private final AtomicInteger leftTime;
    private long time;


    public MyHost() {
        mutex = new ReentrantLock();
        queue = new ArrayList<>();
        isRunning = new AtomicBoolean(true);
        leftTime = new AtomicInteger(0);
        time = currentTimeMillis();
    }

    /**
     * Compares two tasks for sorting based on priority and start time.
     *
     * @param o1 The first task.
     * @param o2 The second task.
     * @return Comparison result.
     */
    private static int compare(Task o1, Task o2) {
        int isScheduled = Integer.compare(o2.getPriority(), o1.getPriority());
        if (isScheduled == 0) return Integer.compare(o1.getStart(), o2.getStart());
        return isScheduled;
    }

    /**
     * Sorts the task queue based on priority and start time.
     */
    private void sortTasks() {
        synchronized (mutex) {
            queue.sort(MyHost::compare);
        }
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            sortTasks();
            checkPreempt();
            updateTask();
            updateTime();
        }
    }

    /**
     * Checks if a preemptive task should be added to the queue.
     */
    private void checkPreempt() {
        synchronized (mutex) {
            if (queue.isEmpty()) return;
            if (task == null || !task.isPreemptible()) return;
            if (task.getPriority() >= queue.get(0).getPriority()) return;

            queue.add(task);
            task = queue.get(0);
            queue.remove(0);
            time = currentTimeMillis();
        }
    }

    /**
     * Updates the current working task and finishes it if it's completed.
     */
    private void updateTask() {
        synchronized (mutex) {
            if (task != null && task.getLeft() == 0) {
                task.finish();
                task = null;
                leftTime.set(0);
                return;
            }

            if (task != null || queue.isEmpty()) return;

            task = queue.get(0);
            queue.remove(0);
            leftTime.set(task.getLeft().intValue());
            time = currentTimeMillis();
        }
    }

    /**
     * Updates the time elapsed for the current task.
     */
    private void updateTime() {
        if (task == null) return;

        long currentTime = currentTimeMillis();
        long elapsedTime = currentTime - time;

        task.setLeft(task.getLeft() - elapsedTime);
        leftTime.set(task.getLeft().intValue());
        time = currentTime;
    }

    @Override
    public void addTask(Task task) {
        synchronized (mutex) {
            queue.add(task);
            checkPreempt();
        }
    }

    @Override
    public int getQueueSize() {
        synchronized (mutex) {
            return queue.size() + (leftTime.get() > 0 ? 1 : 0);
        }
    }

    @Override
    public long getWorkLeft() {
        synchronized (mutex) {
            return queue.stream().mapToLong(Task::getLeft).sum() + leftTime.get();
        }
    }

    @Override
    public void shutdown() {
        isRunning.set(false);
    }
}
