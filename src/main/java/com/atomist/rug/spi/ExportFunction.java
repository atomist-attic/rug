package com.atomist.rug.spi;

import java.lang.annotation.*;

/**
 * Used to annotate methods in Rug type implementation classes that are callable
 * from JavaScript.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExportFunction {

    boolean readOnly();

    boolean exposeAsProperty() default false;

    String description();

    String example() default "";
}
