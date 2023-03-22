package lt.lb.simplespring.jpa;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.persistence.EntityManagerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Tries to match EntityManagerFactory/PlatformTransactionManager bean name
 * prefix with context name.
 *
 * @author laim0nas100
 */
@Scope("singleton")
public class PrefixedTransactionSelector implements PlatformManagerSelector {

    public PrefixedTransactionSelector() {
    }
    
    public PrefixedTransactionSelector(String defContext) {
        this.defaultContext = defContext;
    }

    private String defaultContext = "";
    
    @Autowired
    private Map<String, PlatformTransactionManager> txMap; // should not change once set

    private Map<String, Supplier<PlatformTransactionManager>> txMapCache = new ConcurrentHashMap<>();

    @Autowired
    private Map<String, EntityManagerFactory> emfMap;// should not change once set

    private Map<String, Supplier<EntityManagerFactory>> edfMapCache = new ConcurrentHashMap<>();

    private ThreadLocal<String> context = ThreadLocal.withInitial(this::getDefaultContext);

    public String getDefaultContext() {
        return defaultContext;
    }

    public void setDefaultContext(String defaultContext) {
        this.defaultContext = defaultContext;
    }

    @Override
    public void setCurrentContext(String id) {
        context.set(id);
    }

    @Override
    public String getCurrentContext() {
        return context.get();
    }

    private static <T> Optional<T> getFirstPrefixAndStore(String prefix, Map<String, T> map, Map<String, Supplier<T>> cache) {
        if (StringUtils.isBlank(prefix)) { // don't store empty things
            return getFirstSubEntry(prefix, map).map(m -> m.getValue());
        }
        return Optional.ofNullable(cache.computeIfAbsent(prefix, pref -> {
            Optional<Map.Entry<String, T>> firstSubEntry = getFirstSubEntry(prefix, map);
            if (firstSubEntry.isPresent()) {
                Map.Entry<String, T> entry = firstSubEntry.get();
                String key = entry.getKey();
                return () -> map.get(key);
            } else {
                return null;
            }
        })).map(m -> m.get());
    }

    private static <T> Optional<Map.Entry<String, T>> getFirstSubEntry(String prefix, Map<String, T> map) {
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlatformTransactionManager> resolvePlatformManager(String context) {
        return getFirstPrefixAndStore(context, txMap, txMapCache);
    }

    @Override
    public Optional<EntityManagerFactory> resolveEntityManagerFactory(String context) {
        return getFirstPrefixAndStore(context, emfMap, edfMapCache);
    }

    public Map<String, PlatformTransactionManager> getTxMap() {
        return txMap;
    }

    public void setTxMap(Map<String, PlatformTransactionManager> txMap) {
        this.txMap = txMap;
    }

    public Map<String, EntityManagerFactory> getEmfMap() {
        return emfMap;
    }

    public void setEmfMap(Map<String, EntityManagerFactory> emfMap) {
        this.emfMap = emfMap;
    }
}
