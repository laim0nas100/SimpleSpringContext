[![](https://jitpack.io/v/laim0nas100/SimpleSpringContext.svg)](https://jitpack.io/#laim0nas100/SimpleSpringContext)

Simple Spring context.

Add ContextHolder component class to your Spring project configuration, to statically access default or any configured context.

Also supports all ApplicationContextEvent type events.

Will only run after init event and after shutdown event.

    ContextHolder.addRefreshTask(() -> {
    ... some refresh at any point of application lifetime
    });

    ContextHolder.addStartTask(() -> {
    ... some initialization at any point of application lifetime
    });

    ContextHolder.addStopTask(() -> {
    ... some cleanup at any point of application lifetime
    });

    ContextHolder.addCloseTask(() -> {
    ... some shut-down cleanup (no more refresh possible) at any point of application lifetime
    });



