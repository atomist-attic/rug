package com.atomist.rug.spi;

import java.lang.annotation.*;

/**
 * Used to annotate methods in Rug type implementation classes that are callable
 * from JavaScript. Allows specifying that no-arg methods should be exposed as
 * JavaScript properties.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExportFunction {

    boolean readOnly();

    boolean exposeAsProperty() default false;

    String description();

    String example() default "";

    /**
     * @return should we expose the result directly to Nashorn?
     * Only applicable if it's a JM object.
     * In this case it will offer property access all the way down,
     * using safe proxies that conceal the full range of JVM object access.
     */
    boolean exposeResultDirectlyToNashorn() default false;
}
