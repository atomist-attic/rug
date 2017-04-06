package com.atomist.rug.runtime.js.interop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag annotation that marks that a method should be exposed as a function,
 * even if it has no args. This makes side-effecting methods more intuitive
 * to call.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExposeAsFunction {
}
