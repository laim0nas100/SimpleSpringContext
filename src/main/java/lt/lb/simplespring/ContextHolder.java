package lt.lb.simplespring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

/**
 *
 * Component class holding ApplicationContex, for using simple manual
 * injections, and controlling context refresh and stop events. For example -
 * executor service shutdown hooks or init hooks can be easily configured here.
 *
 * Can hold multiple contexts. First one initialized becomes the default. Can
 * overwrite the default any time.
 *
 * @author laim0nas100
 */
@Component
public class ContextHolder implements ApplicationContextAware {

    public static class InnerCtx {

        public InnerCtx(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        public static InnerCtx defaultCtx() {
            return new InnerCtx(null);
        }

        public AtomicBoolean refreshDone = new AtomicBoolean(false);
        public AtomicBoolean stopDone = new AtomicBoolean(false);
        public AtomicBoolean startDone = new AtomicBoolean(false);
        public AtomicBoolean closeDone = new AtomicBoolean(false);
        public ApplicationContext ctx;

        public ConcurrentLinkedDeque<Runnable> startTasks = new ConcurrentLinkedDeque<>();
        public ConcurrentLinkedDeque<Runnable> closeTasks = new ConcurrentLinkedDeque<>();
        public ConcurrentLinkedDeque<Runnable> refreshTasks = new ConcurrentLinkedDeque<>();
        public ConcurrentLinkedDeque<Runnable> stopTasks = new ConcurrentLinkedDeque<>();

        public void onCtxRefresh() {
            if (this.refreshDone.compareAndSet(false, true)) {
                runTasks(this.refreshTasks);
            }
        }

        public void onCtxStop() {
            if (this.stopDone.compareAndSet(false, true)) {
                runTasks(this.stopTasks);
            }
        }

        public void onCtxStart() {
            if (this.startDone.compareAndSet(false, true)) {
                runTasks(this.startTasks);
            }
        }

        public void onCtxClose() {
            if (this.closeDone.compareAndSet(false, true)) {
                runTasks(this.closeTasks);
            }
        }

        /**
         * Add a task to run after {@link ContextStoppedEvent}
         *
         * @param run
         */
        public void addStopTask(Runnable run) {
            addOrRun(stopDone, run, stopTasks);
        }

        /**
         * Add a task to run after {@link ContextRefreshedEvent}
         *
         * @param run
         */
        public void addRefreshTask(Runnable run) {
            addOrRun(refreshDone, run, refreshTasks);
        }

        /**
         * Add a task to run after {@link ContextClosedEvent}
         *
         * @param run
         */
        public void addCloseTask(Runnable run) {
            addOrRun(closeDone, run, closeTasks);
        }

        /**
         * Add a task to run after {@link ContextStartedEvent}
         *
         * @param run
         */
        public void addStartTask(Runnable run) {
            addOrRun(startDone, run, startTasks);
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

    private static Map<ApplicationContext, InnerCtx> contexts = new ConcurrentHashMap<>();
    private static volatile InnerCtx defaultCtx = new InnerCtx(null);
    private static AtomicBoolean firstInit = new AtomicBoolean(false);

    private static InnerCtx getLazyInitInner(ApplicationContext ctx) {
        Objects.requireNonNull(ctx);
        if (firstInit.compareAndSet(false, true)) {
            defaultCtx.ctx = ctx;
            contexts.put(ctx, defaultCtx);
            return defaultCtx;
        }
        return contexts.computeIfAbsent(ctx, c -> new InnerCtx(c));
    }

    @Component
    public static class RefreshEventListener implements ApplicationListener<ContextRefreshedEvent> {

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            getLazyInitInner(event.getApplicationContext()).onCtxRefresh();
        }

    }

    @Component
    public static class StopEventListener implements ApplicationListener<ContextStoppedEvent> {

        @Override
        public void onApplicationEvent(ContextStoppedEvent event) {
            getLazyInitInner(event.getApplicationContext()).onCtxStop();
        }

    }

    @Component
    public static class StartEventListener implements ApplicationListener<ContextStartedEvent> {

        @Override
        public void onApplicationEvent(ContextStartedEvent event) {
            getLazyInitInner(event.getApplicationContext()).onCtxStart();
        }

    }

    @Component
    public static class CloseEventListener implements ApplicationListener<ContextClosedEvent> {

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            getLazyInitInner(event.getApplicationContext()).onCtxClose();
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        getLazyInitInner(applicationContext);
    }

    public void setDefaultApplicationContext(ApplicationContext applicationContext) {
        defaultCtx = getLazyInitInner(applicationContext);
    }

    /**
     *
     * @return default {@link ApplicationContext}, or null
     */
    public static ApplicationContext getApplicationContext() {
        return defaultCtx.ctx;
    }

    /**
     *
     * @return Every configured {@link ApplicationContext}.
     */
    public static List<ApplicationContext> getApplicationContexts() {
        return new ArrayList<>(contexts.keySet());
    }

    /**
     * Populate the given bean instance through applying after-instantiation
     * callbacks and bean property post-processing (e.g.for annotation-driven
     * injection).
     *
     * Note: This is essentially intended for (re-)populating annotated fields
     * and methods, either for new instances or for deserialized instances. It
     * does
     * <i>not</i> imply traditional by-name or by-type autowiring of properties;
     * use {@link #autowireBeanProperties} for those purposes.
     *
     * @throws BeansException if wiring failed
     */
    public static <T> T autowireBean(T object) {
        ContextHolder.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(object);
        return object;
    }

    /**
     * Add a task to run after {@link ContextStoppedEvent} in current default
     * context
     *
     * @param run
     */
    public static void addStopTask(Runnable run) {
        defaultCtx.addStopTask(run);
    }

    /**
     * Add a task to run after {@link ContextRefreshedEvent} in current default
     * context
     *
     * @param run
     */
    public static void addRefreshTask(Runnable run) {
        defaultCtx.addRefreshTask(run);
    }

    /**
     * Add a task to run after {@link ContextClosedEvent} in current default
     * context
     *
     * @param run
     */
    public static void addCloseTask(Runnable run) {
        defaultCtx.addCloseTask(run);
    }

    /**
     * Add a task to run after {@link ContextStartedEvent} in current default
     * context
     *
     * @param run
     */
    public static void addStartTask(Runnable run) {
        defaultCtx.addStartTask(run);
    }

    /**
     * Add a task to run after {@link ContextStoppedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addStopTask(ApplicationContext ctx, Runnable run) {
        getLazyInitInner(ctx).addStopTask(run);
    }

    /**
     * Add a task to run after {@link ContextRefreshedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addRefreshTask(ApplicationContext ctx, Runnable run) {
        getLazyInitInner(ctx).addRefreshTask(run);
    }

    /**
     * Add a task to run after {@link ContextClosedEvent} in a specified context
     *
     * @param ctx
     * @param run
     */
    public static void addCloseTask(ApplicationContext ctx, Runnable run) {
        getLazyInitInner(ctx).addCloseTask(run);
    }

    /**
     * Add a task to run after {@link ContextStartedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addStartTask(ApplicationContext ctx, Runnable run) {
        getLazyInitInner(ctx).addStartTask(run);
    }

}
