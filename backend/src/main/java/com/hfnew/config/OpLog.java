package com.hfnew.config;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作描述
     */
    String operation() default "";
}
