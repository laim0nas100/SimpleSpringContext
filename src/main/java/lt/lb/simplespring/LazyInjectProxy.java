package lt.lb.simplespring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 *
 * Create an object from interface that is initialized on the first method call.
 *
 * @author laim0nas100
 */
public abstract class LazyInjectProxy {

    /**
     * Create a proxy object for given interface and bean name that fetches real
     * bean from {@link org.springframework.context.ApplicationContext} on a
     * first method call.
     *
     * {@link ContextHolder} must be configured
     *
     * @param <T>
     * @param qualifier
     * @param interf
     * @return
     */
    public static <T> T lazy(String qualifier, Class<T> interf) {
        Objects.requireNonNull(qualifier, "Qualifier is null");
        Objects.requireNonNull(interf, "Interface is null");
        return (T) lazyProxy(ContextHolder.class.getClassLoader(), interf, () -> {
            return ContextHolder.getApplicationContext().getBean(qualifier, interf);
        });
    }

    /**
     * Create a proxy object for given interface that fetches real bean from
     * {@link org.springframework.context.ApplicationContext} on a first method
     * call.
     *
     * {@link ContextHolder} must be configured
     *
     * @param <T>
     * @param interf
     * @return
     */
    public static <T> T lazy(Class<T> interf) {
        Objects.requireNonNull(interf, "Interface is null");
        return (T) lazyProxy(ContextHolder.class.getClassLoader(), interf, () -> {
            return ContextHolder.getApplicationContext().getBean(interf);
        });
    }

    /**
     * Create a proxy object for given interface and a real object
     * {@link Caller}. The {@link Callable} result is cached on the first proxy
     * method call.
     *
     * @param <T>
     * @param loader
     * @param interf
     * @param callable
     * @return
     */
    public static <T> T lazyProxy(ClassLoader loader, Class<T> interf, Callable<T> callable) {
        Objects.requireNonNull(loader);
        Objects.requireNonNull(interf);
        Objects.requireNonNull(callable);

        if (!interf.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are supported");
        }

        return (T) Proxy.newProxyInstance(loader, new Class[]{interf}, new LazyProxyInvocationHandler<>(callable));
    }

    public static class LazyProxyInvocationHandler<T> implements InvocationHandler {

        public LazyProxyInvocationHandler(Callable<T> callable) {
            future = new FutureTask<>(callable);
        }

        private final FutureTask<T> future;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!future.isDone()) {
                future.run();
            }
            return method.invoke(future.get(), args);
        }
    }

}
