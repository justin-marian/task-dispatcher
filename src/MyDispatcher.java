import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MyDispatcher extends Dispatcher {
    Queue<Task> tasks;
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public MyDispatcher(SchedulingAlgorithm algorithm, List<Host> hosts) {
        super(algorithm, hosts);
        tasks = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void addTask(Task task) {
        tasks.offer(task);

        switch (algorithm) {
            case ROUND_ROBIN -> assignRoundRobin(task);
            case SHORTEST_QUEUE -> assignShortestQueue(task);
            case SIZE_INTERVAL_TASK_ASSIGNMENT -> assignBySizeInterval(task);
            case LEAST_WORK_LEFT -> assignLeastWorkLeft(task);
        }
    }

    /**
     * Assigns a task to hosts in a round-robin fashion.
     *
     * @param task The task to be assigned.
     */
    private void assignRoundRobin(Task task) {
        int index = roundRobinIndex.getAndUpdate(i -> (i + 1) % hosts.size());
        hosts.get(index).addTask(task);
    }

    /**
     * Assigns a task to the host with the shortest queue.
     *
     * @param task The task to be assigned.
     */
    private void assignShortestQueue(Task task) {
        Host shortestQueueHost = hosts.stream()
                .min(Comparator.comparingInt(Host::getQueueSize))
                .orElseThrow(() -> new IllegalStateException("No hosts available"));
        shortestQueueHost.addTask(task);
    }

    /**
     * Assigns a task based on its type (short, medium, long).
     *
     * @param task The task to be assigned.
     */
    private void assignBySizeInterval(Task task) {
        switch (task.getType()) {
            case SHORT:
                hosts.get(0).addTask(task);
                break;
            case MEDIUM:
                hosts.get(1).addTask(task);
                break;
            case LONG:
                hosts.get(2).addTask(task);
                break;
            default:
                throw new IllegalArgumentException("Unknown task type: " + task.getType());
        }
    }

    /**
     * Assigns a task to the host with the least work left.
     *
     * @param task The task to be assigned.
     */
    private void assignLeastWorkLeft(Task task) {
        Host leastWorkHost = hosts.stream()
                .min(Comparator.comparingLong(Host::getWorkLeft))
                .orElseThrow(() -> new IllegalStateException("No hosts available"));
        leastWorkHost.addTask(task);
    }
}
