package com.fjy.framework.anotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FRequestMapping {
    String value() default "";
}
