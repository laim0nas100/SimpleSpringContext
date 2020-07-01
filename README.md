Simple Spring context.

Add ContextHolder component class to your Spring project configuration.

Also supports init and shutdown events.

Will only run after init event and after shutdown event.

    ContextHolder.addInitTask(() -> {
    ... some initialization at any point of application.
    });

    ContextHolder.addShutdownTask(() -> {
    ... some cleanup at any point of application
    });



