package lt.lb.simplespring.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.spi.PersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;

/**
 *
 * @author laim0nas100
 */
public class PersistenceUnitInfoMap implements PersistenceUnitManager {

    protected Map<Object, PersistenceUnitInfo> infoMap = Collections.synchronizedMap(new LinkedHashMap<>());
    protected volatile PersistenceUnitInfo defaultInfo;

    public PersistenceUnitInfoMap add(PersistenceUnitInfo info) {
        Objects.requireNonNull(info, "PersistenceUnitInfo is null");
        if (defaultInfo == null) {
            defaultInfo = info;
        }
        Object key = assertGetKey(info);
        PersistenceUnitInfo absent = infoMap.putIfAbsent(key, info);
        if (absent != null) {
            throw new IllegalArgumentException("PersistenceUnitInfo by key " + key + " is allready present");
        }
        return this;
    }

    protected Object assertGetKey(PersistenceUnitInfo info) {
        return Objects.requireNonNull(info.getPersistenceUnitName(), "PersistenceUnitInfo name is null");
    }

    public PersistenceUnitInfoMap setDefault(PersistenceUnitInfo info) {
        infoMap.put(assertGetKey(info), info);
        defaultInfo = info;
        return this;
    }

    @Override
    public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException {
        if (defaultInfo == null) {
            throw new IllegalStateException("No default PersistenceUnitInfo configured");
        }
        return defaultInfo;
    }

    @Override
    public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName) throws IllegalArgumentException, IllegalStateException {
        if (infoMap.isEmpty()) {
            throw new IllegalStateException("No PersistenceUnitInfos configured");
        }
        PersistenceUnitInfo get = infoMap.get(persistenceUnitName);
        if (get == null) {
            throw new IllegalArgumentException("PersistenceUnitInfo by name " + persistenceUnitName + " is not present");
        }
        return get;
    }

    public List<PersistenceUnitInfo> getAll() {
        return new ArrayList<>(infoMap.values());
    }

}
