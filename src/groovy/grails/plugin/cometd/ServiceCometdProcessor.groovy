package grails.plugin.cometd

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

import org.cometd.bayeux.Message
import org.cometd.bayeux.server.ServerChannel
import org.cometd.bayeux.server.ConfigurableServerChannel

class ServiceCometdProcessor {
    final static EXPOSES = "exposes"
    final static EXPOSE = "expose"
    final static COMETD = "cometd"
    
    def configurations = [
        "initializers": [:],
        "messageListeners": [:]
    ]

    /**
     * Determine if the service class would like to register itself as a cometd service.  It does so by declaring a staic variable "exposes"
     * or "expose", which is an array that must contain "cometd"
     */
    static exposesCometd(service) {
        GCU.getStaticPropertyValue(service, EXPOSES)?.contains(COMETD) || GCU.getStaticPropertyValue(service, EXPOSE)?.contains(COMETD)
    }
    
    def process(service, bayeux) {
        def clazz = service.class
        
        // If the service has declared itself a cometd service
        if (ServiceCometdProcessor.exposesCometd(clazz)) {
            // Setup a localsession for the service
            def localSession = bayeux.newLocalSession(clazz.simpleName)
            localSession.handshake();
            
            clazz.metaClass.seeOwnPublishes = false
            clazz.metaClass.localSession = localSession
            clazz.metaClass.serverSession = localSession.serverSession
            
            // Process each method looking for cometd annotations
            clazz.methods?.each { method ->
                processServiceMethod(service, method)
            }
            
            // Initialize all of the configurations. This allows us to control the order in which annotations are processed across many services
            configurations.initializers.each { channel, configurations ->
                configureInitializers(channel, configurations, bayeux)
            }
            
            // Initialize all of the messageListeners
            configurations.messageListeners.each { channel, configurations ->
                configureMessageListeners(channel, configurations, bayeux)
            }
        }
    }
    
    /**
     * Processes a method for cometd configuration annotations.
     */
    def processServiceMethod(service, method) {
        processInitializer(service, method)
        processMessageListener(service, method)
    }
    
    /**
     * Tries to determine if the method is a channel initializer.
     */
    def processInitializer(service, method) {
        def annotation = method.getAnnotation(ChannelInitializer)
        
        if (annotation) {
            // Method must have one parameter.
            if (method.parameterTypes.length != 1) {
                throw new IllegalArgumentException(getParameterMessage(ChannelInitializer.class.simpleName, service, method, "one parameter"))
            }
            
            // Add the method to the list of initializers for the given channel
            addConfigurationForChannel("initializers", annotation.value(), annotation, service, method)
        }
    }
    
    /**
     * Determines if the method is a message listener, and adds it to the configurations accordingly
     */
    def processMessageListener(service, method) {
        def annotation = method.getAnnotation(MessageListener)
        
        if (annotation) {
            // Method must declare 2..4 parameters
            if (!(2..4).contains(method.parameterTypes.length)) {
                throw new IllegalArgumentException(getParameterMessage(MessageListener.class.simpleName, service, method, "2 to 4 parameters"))
            }
            
            // Add the method to the list of message listeners for the channel
            addConfigurationForChannel("messageListeners", annotation.value(), annotation, service, method)
        }
    }
    
    /**
     * Each channel can have multiple initializers, possibly from multiple services.  Here we initialize them all for a given channel.
     */
    def configureInitializers(channel, configurations, bayeux) {
        bayeux.createIfAbsent(channel, { configurableServerChannel ->
            configurations.each { configuration ->
                configuration.method.invoke(configuration.service, configurableServerChannel)
            }
        } as ConfigurableServerChannel.Initializer)
    }
    
    def configureMessageListeners(channelId, configurations, bayeux) {
        bayeux.createIfAbsent(channelId)
        
        configurations.each { configuration ->
            def method = configuration.method
            def service = configuration.service
            def arguments = configuration.method.parameterTypes

            bayeux.getChannel(channelId).addListener({ session, channel, message ->
                    try {
                        if (service.seeOwnPublishes || session != service.serverSession) {
                            def data = Message.class.isAssignableFrom(arguments[1]) ? message : message.data
                            def reply
                    
                            switch (arguments.length) {
                                case 2:
                                    reply = method.invoke(service, session, data)
                                    break;
                                case 3:
                                    reply = method.invoke(service, session, data, message.id)
                                    break;
                                case 4:
                                    reply = method.invoke(service, session, message.channel, data, message.id)
                            }

                            if (reply instanceof Boolean && !reply){
                                return false
                            } else {
                                session.deliver(service.serverSession, message.channel, reply, message.id);
                            }
                        }
                    } catch (e) {
                        e.printStackTrace();
                    }
                
                return true
            } as ServerChannel.MessageListener)
        }
            
    }
    
    /**
     * Helper method to add a configuration object to the configuration container for a given channel
     */
    private addConfigurationForChannel(type, channel, annotation, service, method, ext) {
        def channelConfigurations = configurations[type][channel] ?: []
        def configuration = [annotation: annotation, service: service, method: method] << ext
        
        configurations[type][channel] = channelConfigurations << configuration
    }
    
    private addConfigurationForChannel(type, channel, annotation, service, method) {
        addConfigurationForChannel(type, channel, annotation, service, method, [:])
    }
    
    private getParameterMessage(annotation, service, method, requirement) {
        return "@${annotation}: '${service.class.simpleName}.${method.name}' declared ${method.parameterTypes.length} parameters, but must declare ${requirement}"
    }
}