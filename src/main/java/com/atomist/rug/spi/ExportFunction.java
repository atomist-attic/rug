package com.atomist.rug.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate methods in Rug type implementation classes that are callable
 * from Rug DSL and JavaScript.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExportFunction {

    boolean readOnly();

    String description();

    String example() default "";
}
