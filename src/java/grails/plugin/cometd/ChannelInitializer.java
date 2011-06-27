package grails.plugin.cometd;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
public @interface ChannelInitializer {
	String value() default "";
}