package lt.lb.simplespring.jpa;

import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import lt.lb.uncheckedutils.SafeOpt;
import lt.lb.uncheckedutils.func.UncheckedConsumer;
import lt.lb.uncheckedutils.func.UncheckedFunction;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 *
 * @author laim0nas100
 */
public interface PlatformManagerSelector extends PlatformTransactionManager {
    
    public default PlatformTransactionManager getCurrentPlatformManager() {
        String currentIdentifier = getCurrentContext();
        Optional<PlatformTransactionManager> optional = resolvePlatformManager(currentIdentifier);
        if (!optional.isPresent()) {
            throw new IllegalStateException("No PlatformTransactionManager for id " + currentIdentifier);
        }
        return optional.get();
    }
    
    public default EntityManagerFactory getEntityManagerFactory() {
        String currentIdentifier = getCurrentContext();
        Optional<EntityManagerFactory> optional = resolveEntityManagerFactory(currentIdentifier);
        if (!optional.isPresent()) {
            throw new IllegalStateException("No EntityManagerFactory for id " + currentIdentifier);
        }
        return optional.get();
    }
    
    public default EntityManager getEntityManager() {
        return EntityManagerFactoryUtils.getTransactionalEntityManager(getEntityManagerFactory());
    }
    
    public default <T> SafeOpt<T> runIn(String ctx, UncheckedFunction<EntityManager, T> supl) {
        String prev = getCurrentContext();
        setCurrentContext(ctx);
        SafeOpt<T> result = SafeOpt.ofGet(() -> supl.applyUnchecked(getEntityManager()));
        setCurrentContext(prev);
        return result;
        
    }
    
    public default Optional<Throwable> runIn(String ctx, UncheckedConsumer<EntityManager> cons) {
        return runIn(ctx, em -> {
            cons.accept(em);
            return null;
        }).getError().asOptional();
    }
    
    public String getCurrentContext();
    
    public void setCurrentContext(String context);
    
    public Optional<PlatformTransactionManager> resolvePlatformManager(String context);
    
    public Optional<EntityManagerFactory> resolveEntityManagerFactory(String context);
    
    @Override
    public default TransactionStatus getTransaction(TransactionDefinition td) throws TransactionException {
        return getCurrentPlatformManager().getTransaction(td);
    }
    
    @Override
    public default void commit(TransactionStatus ts) throws TransactionException {
        getCurrentPlatformManager().commit(ts);
    }
    
    @Override
    public default void rollback(TransactionStatus ts) throws TransactionException {
        getCurrentPlatformManager().rollback(ts);
    }
    
}
