package grails.plugin.cometd

import grails.plugin.spock.*

import org.cometd.server.BayeuxServerImpl

class ServiceCometdProcessorSpec extends UnitSpec {
	def bayeux = new BayeuxServerImpl()
	def processor = new ServiceCometdProcessor()
	
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
		given: "a cometd service"
		def service = new MethodInitializerService()
		
		when: "the service is processed"
		processor.process(service, bayeux)
		
		then: "a channel should be created and it should be persistent"
		def channel = bayeux.getChannel("/foo/bar")
		channel != null
		channel.persistent == true
	}
}

class BaseTestService {
	def getClazz() {
		return getClass()
	}
}

class CometdExposingService {
	static exposes = ["jms", "xfire", "cometd"]
}

class NonCometdService {
	static exposes = ["jms"]
}

class MethodInitializerService extends BaseTestService {
	static exposes = ["cometd"]
	
	@ChannelInitializer("/foo/bar")
	def configure(channel) {
		channel.persistent = true
	}
}