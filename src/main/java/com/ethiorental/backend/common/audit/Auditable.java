package com.ethiorental.backend.common.audit;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();
    String module();
    String entityIdParam() default "";
    
}
