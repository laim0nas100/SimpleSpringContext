package lt.lb.simplespring;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 *
 * @author laim0nas100
 */
@Component
public class ContextHolder implements ApplicationContextAware {

    @Component
    public static class InitEventListener implements ApplicationListener<ContextRefreshedEvent> {

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            if (initDone.compareAndSet(false, true)) {
                runTasks(initTasks);
            }
        }

    }

    @Component
    public static class ShutdownEventListener implements ApplicationListener<ContextClosedEvent> {

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            if (shutdownDone.compareAndSet(false, true)) {
                runTasks(shutdownTasks);
            }
        }

    }

    private static AtomicBoolean initDone = new AtomicBoolean(false);
    private static AtomicBoolean shutdownDone = new AtomicBoolean(false);
    private static ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ContextHolder.ctx = applicationContext;
    }

    public static ApplicationContext getApplicationContext() {
        return ContextHolder.ctx;
    }

    public static <T> T autowire(T object) {
        ContextHolder.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(object);
        return object;
    }

    private static ConcurrentLinkedDeque<Runnable> initTasks = new ConcurrentLinkedDeque<>();
    private static ConcurrentLinkedDeque<Runnable> shutdownTasks = new ConcurrentLinkedDeque<>();

    public static void addInitTask(Runnable run) {
        addOrRun(initDone, run, initTasks);
    }

    public static void addShutdownTask(Runnable run) {
        addOrRun(shutdownDone, run, shutdownTasks);
    }

    private static void addOrRun(AtomicBoolean toRun, Runnable task, Collection<Runnable> coll) {
        if (toRun.get()) {
            task.run();
        } else {
            coll.add(task);
        }
    }

    private static void runTasks(Collection<Runnable> tasks) {
        Iterator<Runnable> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            Runnable next = iterator.next();
            next.run();
            iterator.remove();
        }
    }

}
