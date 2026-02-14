package com.ecommerce.project.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that should be audited
 *
 * Usage:
 * @Auditable(action = "CREATE_PRODUCT", entityType = "Product")
 * public ProductDTO addProduct(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Action being performed
     */
    String action();

    /**
     * Type of entity being acted upon
     */
    String entityType() default "";

    /**
     * Whether to log the request body
     */
    boolean logRequestBody() default false;

    /**
     * Whether to log the response body
     */
    boolean logResponseBody() default false;
}
