package lt.lb.simplespring;

import java.util.Objects;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author laim0nas100
 */
public class InnerCtx extends CtxTasks implements Comparable<InnerCtx> {

    public final ApplicationContext ctx;
    
    public int parentLevel(ApplicationContext[] assing) {
        int i = 0;
        ApplicationContext me = ctx;
        while (me.getParent() != null) {
            i++;
            me = me.getParent();
        }
        if (assing != null && assing.length > 0) {
            assing[0] = me;
        }
        return i;
    }

    public InnerCtx(ApplicationContext ctx) {
        this.ctx = Objects.requireNonNull(ctx);
    }

    @Override
    public int compareTo(InnerCtx o) {
        return Integer.compare(parentLevel(null), o.parentLevel(null));
    }

    @Override
    public ApplicationContext getContext() {
        return ctx;
    }

}
