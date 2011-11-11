package grails.plugin.cometd

import grails.plugin.spock.*

import org.cometd.bayeux.Message
import org.cometd.server.BayeuxServerImpl

class ServiceCometdProcessorSpec extends UnitSpec {
    def bayeux = new BayeuxServerImpl()
    def processor = new ServiceCometdProcessor()
    def context = [bayeux: bayeux]
    
    def "can identify services that expose cometd functionality"() {
        given: "a cometd service"
        def service = CometdExposingService
        
        expect: "the processor to find that the service exposes cometd functionality"
        ServiceCometdProcessor.exposesCometd(service) == true
    }
    
    def "will ignore services that don't expose cometd functionality"() {
        given: "a non-cometd service"
        def service = NonCometdService
        
        expect: "the processor to find that the service doesn't expose cometd functionality"
        ServiceCometdProcessor.exposesCometd(service) == false
    }
    
    def "can find channel initializer method"() {
        given: "a cometd service which has a @ChannelInitializer annotated method"
        def service = new MethodInitializerService()
        
        when: "the service is processed"
        processor.process(service, context)
        
        then: "a channel should be created and it should be persistent"
        def channel = bayeux.getChannel("/foo/bar")
        channel != null
        channel.persistent == true
    }
    
    def "encounters an exception when the annotated initializer method doesn't have the correct arguments"() {
        given: "a cometd service which has a @ChannelInitializer annotated method with no arguments defined"
        def service = new MethodInitializerExceptionService()
        
        when: "the service is processed"
        processor.process(service, context)
        
        then: "an exception should be thrown"
        IllegalArgumentException iae = thrown()
    }
    
    def "can register multiple initializers for one channel"() {
        when: "the service is processed"
        processor.process(service, context)
        
        then: "the channel should be initialized"
        bayeux.getChannel("/foo/bar").persistent == true
        
        where: "there are two services"
        service << [new MethodInitializerService(), new MethodInitializerService()]
    }
    
    def "can find message listener method and initialize it so that it is listening to channel publish events"() {
        given: "a cometed service which has a @MessageListener annotated method"
        def service = new MessageListenerService()
        context["messageListenerService"] = service
        def local = bayeux.newLocalSession("local")
        local.handshake()
        
        when: "the service is processed and the channel is published to"
        processor.process(service, context)
        bayeux.getChannel("/foo/bar").publish(local.serverSession(), [body: "hola"], null)
        
        then: "the method listener should be registered and called"
        bayeux.getChannel("/foo/bar") != null
        service.body == "hola"
    }
    
    def "can correctly initialize message listeners with different signatures"() {
        given: "a cometed service which has @MessageListener annotated methods all with different method signatures"
        def service = new MessageListenerSignaturesService()
        context["messageListenerSignaturesService"] = service
        def local = bayeux.newLocalSession("local")
        local.handshake()
        
        when: "the service is processed and a message is published on the channel"
        processor.process(service, context)
        bayeux.getChannel("/foo/bar").publish(local.serverSession(), [body: "hola"], "17")
        
        then: "all listeners should be initialized and called"
        service.called.sort() == ["two", "twoTyped", "three", "four"].sort()
    }

    def "can reload message listeners gracefully"() {
        given: "a cometed service with @MessageListeners"
        def service = new MessageListenerService()
        context["messageListenerService"] = service
        def local = bayeux.newLocalSession("local")
        local.handshake()
        
        when: "the service is processed and then reprocessed"
        processor.process(service, context)
        processor.process(service, context)
        
        then: "the channel should have the correct listeners attached"
        bayeux.getChannel("/foo/bar").listeners.size() == 1
    }
}

class CometdExposingService {
    static exposes = ["jms", "xfire", "cometd"]
}

class NonCometdService {
    static exposes = ["jms"]
}

class MethodInitializerService {
    static exposes = ["cometd"]
    
    @ChannelInitializer("/foo/bar")
    def configure(channel) {
        channel.persistent = true
    }
}

class MethodInitializerExceptionService {
    static exposes = ["cometd"]

    @ChannelInitializer("/foo/bar")
    def configure() {}
}

class MessageListenerService {
    static exposes = ["cometd"]
    def body
    
    @MessageListener(channel = "/foo/bar", broadcast = false)
    def receive(session, message) {
        body = message.body
    }
}

class MessageListenerSignaturesService {
    static exposes = ["cometd"]
    def called = []
    
    @MessageListener("/foo/bar")
    def two(session, message) {
        if (message.body == "hola") {
            called << "two"
        }
    }

    @MessageListener("/foo/bar")
    def twoTyped(session, Message message) {
        if (message.data.body == "hola") {
            called << "twoTyped"
        }
    }

    @MessageListener("/foo/bar")
    def three(session, message, id) {
        if (message.body == "hola" && id == "17") {
            called << "three"
        }
    }

    @MessageListener("/foo/bar")
    def four(session, channel, message, id) {
        if (message.body == "hola" && id == "17" && channel == "/foo/bar") {
            called << "four"
        }
    }
}