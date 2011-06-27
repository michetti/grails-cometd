package grails.plugin.cometd;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface MessageListener {
	String value() default "";
	String channel() default "";
	boolean broadcast() default true;
}