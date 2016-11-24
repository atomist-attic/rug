package com.atomist.project.common.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a method computes a parameter, based on
 * existing ParameterValues. The signature must take ParameterValues and return String.
 * Note that such methods must be public. The value indicates the name of the
 * exposed parameter, which need not be the same as the method name.
 *
 * @see ReflectiveParameterComputer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ComputedParameter {

    String value();
}
