package com.fjy.framework.anotation;


import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FAutowired {
    String value() default "";
}
