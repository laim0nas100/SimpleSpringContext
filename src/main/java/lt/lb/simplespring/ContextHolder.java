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
 * Component class holding ApplicationContex, for using simple manual injections.
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

    /**
     *
     * @return ApplicationContext, or null
     */
    public static ApplicationContext getApplicationContext() {
        return ContextHolder.ctx;
    }

    /**
     * Populate the given bean instance through applying after-instantiation
     * callbacks and bean property post-processing (e.g. for annotation-driven
     * injection).
     * <p>
     * Note: This is essentially intended for (re-)populating annotated fields
     * and methods, either for new instances or for deserialized instances. It
     * does
     * <i>not</i> imply traditional by-name or by-type autowiring of properties;
     * use {@link #autowireBeanProperties} for those purposes.
     *
     * @param existingBean the existing bean instance
     * @throws BeansException if wiring failed
     */
    public static <T> T autowireBean(T object) {
        ContextHolder.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(object);
        return object;
    }

    private static ConcurrentLinkedDeque<Runnable> initTasks = new ConcurrentLinkedDeque<>();
    private static ConcurrentLinkedDeque<Runnable> shutdownTasks = new ConcurrentLinkedDeque<>();

    /**
     * Add init task to run after init
     * @param run 
     */
    public static void addInitTask(Runnable run) {
        addOrRun(initDone, run, initTasks);
    }

    /**
     * Add shutdown task to run after shutdown
     * @param run 
     */
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
