package com.fjy.framework.anotation;

        import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FController {
    String value() default "";
}
