[![](https://jitpack.io/v/laim0nas100/SimpleSpringContext.svg)](https://jitpack.io/#laim0nas100/SimpleSpringContext)

Simple Spring context.

Add ContextHolder component class to your Spring project configuration, to statically access root or any configured context.

Also supports all ApplicationContextEvent types.

    ContextHolder.addRefreshTask(ctx -> {
    ... some refresh at any point of application lifetime
    });

    ContextHolder.addStartTask(ctx -> {
    ... some initialization at any point of application lifetime
    });

    ContextHolder.addStopTask(ctx -> {
    ... some cleanup at any point of application lifetime
    });

    ContextHolder.addCloseTask(ctx -> {
    ... some shut-down cleanup (no more refresh possible) at any point of application lifetime
    });



