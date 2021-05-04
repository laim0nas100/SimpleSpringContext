package lt.lb.simplespring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
 * injections, and controlling context events. For example - executor service
 * shutdown hooks can be configured here.
 *
 * Can hold multiple contexts. First one initialized becomes the root.
 *
 * @author laim0nas100
 */
@Component
public class ContextHolder implements ApplicationContextAware {

    public static class InnerCtx extends CtxTasks {

        public final ApplicationContext ctx;
        
        
        public int parentLevel(ApplicationContext[] assing){
            int i = 0;
            ApplicationContext me = ctx;
            while(me.getParent() != null){
                i++;
                me = me.getParent();
            }
            assing[0] = me;
            return i;
        }

        public InnerCtx(ApplicationContext ctx) {
            this.ctx = Objects.requireNonNull(ctx);
        }

    }

    private static Map<ApplicationContext, InnerCtx> contexts = new ConcurrentHashMap<>();
    private static volatile CtxTasks forEveryContext = new CtxTasks();

    private static InnerCtx getLazyInitInner(ApplicationContext ctx) {
        Objects.requireNonNull(ctx);
        return contexts.computeIfAbsent(ctx, c -> new InnerCtx(c));
    }

    @Component
    public static class RefreshEventListener implements ApplicationListener<ContextRefreshedEvent> {

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            CtxTasks.runTasks(ctx, getLazyInitInner(ctx).refreshTasks);
            CtxTasks.runTasks(ctx, forEveryContext.refreshTasks);
        }

    }

    @Component
    public static class StopEventListener implements ApplicationListener<ContextStoppedEvent> {

        @Override
        public void onApplicationEvent(ContextStoppedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            CtxTasks.runTasks(ctx, getLazyInitInner(ctx).stopTasks);
            CtxTasks.runTasks(ctx, forEveryContext.stopTasks);
        }

    }

    @Component
    public static class StartEventListener implements ApplicationListener<ContextStartedEvent> {

        @Override
        public void onApplicationEvent(ContextStartedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            CtxTasks.runTasks(ctx, getLazyInitInner(ctx).startTasks);
            CtxTasks.runTasks(ctx, forEveryContext.startTasks);
        }

    }

    @Component
    public static class CloseEventListener implements ApplicationListener<ContextClosedEvent> {

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            ApplicationContext ctx = event.getApplicationContext();
            CtxTasks.runTasks(ctx, getLazyInitInner(ctx).closeTasks);
            CtxTasks.runTasks(ctx, forEveryContext.closeTasks);
            contexts.remove(ctx);
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
        Collection<InnerCtx> values = contexts.values();
        ApplicationContext root = null;
        int maxLevel = -1;
        
        for(InnerCtx inner:values){
            ApplicationContext[] newRoot = new ApplicationContext[1];
            int parentLevel = inner.parentLevel(newRoot);
            if(parentLevel > maxLevel){
                root = newRoot[0];
                maxLevel = parentLevel;
            }
        }
        return root;
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
     * Add a task to run after {@link ContextStoppedEvent} in all contexts
     *
     * @param run
     */
    public static void addStopTask(CtxConsumer run) {
        forEveryContext.stopTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextRefreshedEvent} in all contexts
     * context
     *
     * @param run
     */
    public static void addRefreshTask(CtxConsumer run) {
        forEveryContext.refreshTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextClosedEvent} in all contexts
     * context
     *
     * @param run
     */
    public static void addCloseTask(CtxConsumer run) {
        forEveryContext.closeTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextStartedEvent} in all contexts
     * context
     *
     * @param run
     */
    public static void addStartTask(CtxConsumer run) {
        forEveryContext.startTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextStoppedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addStopTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).stopTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextRefreshedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addRefreshTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).refreshTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextClosedEvent} in a specified context
     *
     * @param ctx
     * @param run
     */
    public static void addCloseTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).closeTasks.add(run);
    }

    /**
     * Add a task to run after {@link ContextStartedEvent} in a specified
     * context
     *
     * @param ctx
     * @param run
     */
    public static void addStartTask(ApplicationContext ctx, CtxConsumer run) {
        getLazyInitInner(ctx).startTasks.add(run);
    }

}
