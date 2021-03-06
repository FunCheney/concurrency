package com.fchen.concurrency.annoations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Classname NotThreadSafe
 * @Description 用来标记线程不安全的类
 * @Date 2019/4/26 11:53
 * @Author by Fchen
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface NotThreadSafe {

    String value() default "";
}
