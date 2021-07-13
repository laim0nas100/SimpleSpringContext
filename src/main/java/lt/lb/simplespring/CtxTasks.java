package lt.lb.simplespring;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author laim0nas100
 */
public class CtxTasks {

    public ConcurrentLinkedDeque<CtxConsumer> startTasks = new ConcurrentLinkedDeque<>();
    public ConcurrentLinkedDeque<CtxConsumer> closeTasks = new ConcurrentLinkedDeque<>();
    public ConcurrentLinkedDeque<CtxConsumer> refreshTasks = new ConcurrentLinkedDeque<>();
    public ConcurrentLinkedDeque<CtxConsumer> stopTasks = new ConcurrentLinkedDeque<>();

    public static void runTasks(ApplicationContext ctx, Collection<CtxConsumer> tasks) {
        Iterator<CtxConsumer> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            CtxConsumer next = iterator.next();
            if (next != null) {
                next.accept(ctx);
            }
        }
    }

}
