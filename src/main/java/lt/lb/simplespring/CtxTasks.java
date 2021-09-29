package lt.lb.simplespring;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author laim0nas100
 */
public class CtxTasks {

    public final AtomicBoolean closed = new AtomicBoolean(false);
    public final AtomicBoolean started = new AtomicBoolean(false);

    public final ConcurrentLinkedDeque<CtxConsumer> startTasks = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<CtxConsumer> closeTasks = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<CtxConsumer> refreshTasks = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<CtxConsumer> stopTasks = new ConcurrentLinkedDeque<>();

    public static void runTasks(ApplicationContext ctx, Collection<CtxConsumer> tasks) {
        Iterator<CtxConsumer> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            CtxConsumer next = iterator.next();
            if (next != null) {
                next.accept(ctx);
            }
        }
    }

    public boolean setStarted() {
        return started.compareAndSet(false, true);
    }

    public boolean setClosed() {
        return closed.compareAndSet(false, true);
    }

    public boolean isStarted() {
        return started.get();
    }

    public boolean isClosed() {
        return closed.get();
    }

}
