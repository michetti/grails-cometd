package grails.plugin.cometd

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.cometd.bayeux.server.ConfigurableServerChannel

class ServiceCometdProcessor {
	final static EXPOSES = "exposes"
	final static EXPOSE = "expose"
	final static COMETD = "cometd"
	
	def configuration = [:]
	
	def process(service, bayeux) {
		def clazz = service.clazz
		
		// If the service has declared itself a cometd service
		if (ServiceCometdProcessor.exposesCometd(clazz)) {
			// Process each method looking for cometd annotations
			clazz.methods?.each { method ->
				processServiceMethod(service, method, bayeux)
			}
		}
	}
	
	/**
	 * Processes a method for cometd configuration annotations.
	 */
	def processServiceMethod(service, method, bayeux) {
		processInitializer(service, method, bayeux)
	}
	
	/**
	 * Tries to determine if the method is a channel initializer.
	 */
	def processInitializer(service, method, bayeux) {
		def annotation = method.getAnnotation(ChannelInitializer)
		
		if (annotation) {
			// Method must have one parameter.
			if (method.parameterTypes.length != 1) {
				throw new IllegalArgumentException("@ChannelInitializer: '${service.clazz.simpleName}.${method.name}' must declare one parameter")
			}
			
			// TODO: Save off the config and do the initialization later
			bayeux.createIfAbsent(annotation.value(), { channel ->
				method.invoke(service, channel)
			} as ConfigurableServerChannel.Initializer)
		}
	}
	
	/**
	 * Determine if the service class would like to register itself as a cometd service.  It does so by declaring a staic variable "exposes"
	 * or "expose", which is an array that must contain "cometd"
	 */
	static exposesCometd(service) {
		GCU.getStaticPropertyValue(service, EXPOSES)?.contains(COMETD) || GCU.getStaticPropertyValue(service, EXPOSE)?.contains(COMETD)
	}
}