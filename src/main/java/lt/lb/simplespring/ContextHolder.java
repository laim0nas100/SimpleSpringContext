package lt.lb.simplespring;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

/**
 *
 * Component class holding ApplicationContex, for using simple manual
 * injections, and controlling context events. For example - executor service
 * shutdown hooks can be configured here.
 *
 * Can hold multiple contexts. Usually the first one initialized becomes the
 * root, but traverses hierarchy to find root.
 *
 * Refreshes spring context as needed.
 *
 * @author laim0nas100
 */
@Configuration
@Component
public class ContextHolder implements ApplicationContextAware {

    private static Map<String, InnerCtx> contexts = new ConcurrentHashMap<>();
    private static volatile CtxTasks forEveryContext = new CtxTasks() {
        @Override
        public ApplicationContext getContext() {
            return null;
        }
    };

    private static InnerCtx getLazyInitInner(ApplicationContext ctx) {
        Objects.requireNonNull(ctx);
        return contexts.computeIfAbsent(ctx.getId(), c -> new InnerCtx(ctx));
    }

    @Component
    public static class RefreshEventListener implements ApplicationListener<ContextRefreshedEvent> {

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {

            ApplicationContext ctx = event.getApplicationContext();
            getLazyInitInner(ctx).runTasksByEvent(ctx, ContextEventType.REFRESH);
            forEveryContext.runTasksByEvent(ctx, ContextEventType.REFRESH);
        }

    }

    @Component
    public static class StopEventListener implements ApplicationListener<ContextStoppedEvent> {

        @Override
        public void onApplicationEvent(ContextStoppedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            getLazyInitInner(ctx).runTasksByEvent(ctx, ContextEventType.STOP);
            forEveryContext.runTasksByEvent(ctx, ContextEventType.STOP);
        }

    }

    @Component
    public static class StartEventListener implements ApplicationListener<ContextStartedEvent> {

        @Override
        public void onApplicationEvent(ContextStartedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            getLazyInitInner(ctx).runTasksByEvent(ctx, ContextEventType.START);
            forEveryContext.runTasksByEvent(ctx, ContextEventType.START);
        }

    }

    @Component
    public static class CloseEventListener implements ApplicationListener<ContextClosedEvent> {

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            getLazyInitInner(ctx).runTasksByEvent(ctx, ContextEventType.CLOSE);
            forEveryContext.runTasksByEvent(ctx, ContextEventType.CLOSE);
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        getLazyInitInner(applicationContext);
    }

    /**
     *
     * @return root {@link ApplicationContext}, or any other present
     */
    public static ApplicationContext getApplicationContext() {
        ApplicationContext[] ctxs = getApplicationContexts();
        if (ctxs.length == 0) {
            return null;
        }
        return ctxs[0];
    }

    /**
     *
     * @return Every configured {@link ApplicationContext}.
     */
    public static ApplicationContext[] getApplicationContexts() {
        return contexts.values().stream().filter(f -> f != null)
                .sorted().map(m -> m.ctx).toArray(s -> new ApplicationContext[s]);// root comes first
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
     * Add a task to run after {@link ContextStoppedEvent} in all contexts
     *
     * @param run
     */
    public static void addStopTask(CtxConsumer run) {
        forEveryContext.addOrRun(ContextEventType.STOP, run);
    }

    /**
     * Add a task to run after {@link ContextRefreshedEvent} in all contexts
     *
     * @param run
     */
    public static void addRefreshTask(CtxConsumer run) {
        forEveryContext.addOrRun(ContextEventType.REFRESH, run);
    }

    /**
     * Add a task to run after {@link ContextClosedEvent} in all contexts
     *
     * @param run
     */
    public static void addCloseTask(CtxConsumer run) {
        forEveryContext.addOrRun(ContextEventType.CLOSE, run);
    }

    /**
     * Add a task to run after {@link ContextStartedEvent} in all contexts
     *
     * @param run
     */
    public static void addStartTask(CtxConsumer run) {
        forEveryContext.addOrRun(ContextEventType.START, run);
    }

    /**
     * Add a task to run after {@link ContextStoppedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addStopTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).addOrRun(ContextEventType.STOP, run);
    }

    /**
     * Add a task to run after {@link ContextRefreshedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addRefreshTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).addOrRun(ContextEventType.REFRESH, run);
    }

    /**
     * Add a task to run after {@link ContextClosedEvent} in a specified context
     *
     * @param ctx
     * @param run
     */
    public static void addCloseTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).addOrRun(ContextEventType.CLOSE, run);
    }

    /**
     * Add a task to run after {@link ContextStartedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addStartTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).addOrRun(ContextEventType.START, run);
    }

    /**
     * If given {@link ApplicationContext} is closed
     *
     * @param ctx
     * @return
     */
    public static boolean isClosed(ApplicationContext ctx) {
        return getLazyInitInner(ctx).is(ContextEventType.CLOSE);
    }

    /**
     * If given {@link ApplicationContext} is started
     *
     * @param ctx
     * @return
     */
    public static boolean isStarted(ApplicationContext ctx) {
        return getLazyInitInner(ctx).is(ContextEventType.START);
    }

    /**
     * If any {@link ApplicationContext} is closed
     *
     * @return
     */
    public static boolean isClosed() {
        return forEveryContext.is(ContextEventType.CLOSE);
    }

    /**
     * If any {@link ApplicationContext} is started
     *
     * @return
     */
    public static boolean isStarted() {
        return forEveryContext.is(ContextEventType.START);
    }

}
