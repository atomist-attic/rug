package com.atomist.rug.spi;

import java.lang.annotation.*;

/**
 * Used to annotate methods in Rug type implementation classes that are callable
 * from Rug DSL and JavaScript.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface ExportFunction {

    boolean readOnly();

    String description();

    String example() default "";
}
