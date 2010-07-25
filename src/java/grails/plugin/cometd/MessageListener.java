package grails.plugin.cometd;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface MessageListener {
	String value() default "";
}