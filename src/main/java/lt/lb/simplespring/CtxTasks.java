package lt.lb.simplespring;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author laim0nas100
 */
public abstract class CtxTasks {

    public static class Subset {

        public final ContextEventType type;
        public final ConcurrentLinkedDeque<CtxConsumer> tasks = new ConcurrentLinkedDeque<>();
        public final ConcurrentHashMap<CtxConsumer, Boolean> doneTasks = new ConcurrentHashMap<>();
        public final AtomicBoolean hasOccured = new AtomicBoolean(false);

        public Subset(ContextEventType type) {
            this.type = type;
        }

        protected void runIfOccured(ApplicationContext ctx, CtxConsumer consumer) {
            if (hasOccured.get()) {
                doneTasks.computeIfPresent(consumer, (k, v) -> {
                    if (!v) {
                        k.accept(ctx);
                    }
                    return true;
                });
            }
        }
    }

    public final EnumMap<ContextEventType, Subset> tasks = new EnumMap<>(ContextEventType.class);

    public CtxTasks() {
        tasks.put(ContextEventType.STOP, new Subset(ContextEventType.STOP));
        tasks.put(ContextEventType.START, new Subset(ContextEventType.START));
        tasks.put(ContextEventType.CLOSE, new Subset(ContextEventType.CLOSE));
        tasks.put(ContextEventType.REFRESH, new Subset(ContextEventType.REFRESH));

    }

    public void runTasksByEvent(ApplicationContext ctx, ContextEventType type) {
        Subset subset = tasks.get(type);
        subset.hasOccured.set(true);
        Iterator<CtxConsumer> iterator = subset.tasks.iterator();
        while (iterator.hasNext()) {
            CtxConsumer consumer = iterator.next();
            if(consumer != null){
                 subset.runIfOccured(ctx, consumer);
            }
        }
    }

    public void addOrRun(ContextEventType type, CtxConsumer task) {
        Subset subset = tasks.get(type);
        subset.doneTasks.computeIfAbsent(task, k -> {
            subset.tasks.add(k);
            return false;
        });
        subset.runIfOccured(getContext(), task);
    }
    
    public abstract ApplicationContext getContext();

    public void set(ContextEventType type) {
        tasks.get(type).hasOccured.set(true);
    }

    public boolean is(ContextEventType type) {
        return tasks.get(type).hasOccured.get();
    }

}
