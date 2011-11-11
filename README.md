# Grails Cometd Plugin

[CometD](http://cometd.org) is a scalable HTTP-based event routing bus that uses an Ajax Push technology pattern known as [Comet](http://en.wikipedia.org/wiki/Comet_(programming\)).  This plugin allows your Grails application to push asynchronous notifications to HTTP clients using CometD and the [Bayeux](http://cometd.org/documentation/bayeux) protocol.

## Installation
NOTE: Because of a bug this won't work in grails 1.3.2 (fixed in 1.3.3):

	grails install-plugin cometd
	
You can always download the [source](https://github.com/michetti/grails-cometd) and then from the plugin directory:

	grails package-plugin
	
And then from your application directory:

	grails install-plugin /path/to/grails-cometd-plugin-zip-file

## Usage

### CometD Servlet
***

The plugin configures a CometdServlet, mapped to the path cometd relative to your web application's context path.

### Bayeux Service
***

A bean named bayeux is made available to your application. It is an instance of [BayeuxServer](http://download.cometd.org/bayeux-api-2.0.beta0-javadoc/org/cometd/bayeux/server/BayeuxServer.html). This is used to interact with the Comet server.

### Annotations
***

You can annotate your service classes and the plugin will wire everything up for you.  This needs a bit of Cometd knowledge, but it's fairly straight forward.  In order to register your service as a Cometd service you just have to add a static property to the service, much like the JMS and XFire plugins. (though this might be moving to a class level annotation, we'll see.)

	static exposes = ["cometd"]

#### @ChannelInitializer
When you annotate a service method with the @ChannelInitializer annotation, the plugin will register it as a ConfigurableServerChannel.Initializer with the bayeux server.  Your service method will be called with one argument, which is the ConfigurableServerChannel instance of the channel. Example:

	@ChannelInitializer("/foo/bar")
	def configure(channel) {
		channel.persistent = true
	}

#### @MessageListener
Annotating a service method with the @MessageListener annotation will register the method as a ServerChannel.MessageListener on the provided channel.  The functionality is similar to that found with the AbstractService that comes with the Cometd distribution.  Depending on the signature of your method, it will be called with different arguments.  Possible options include:

	def service(session, data) // The "from" Session and Message.data
	def service(session, data, id) // The "from" Session, Message.data, and the client id
	def service(session, channel, message, id) // The "from" Session, the channel(String), the Message, and the client id
	
If you were to explicitly define the parameter type for message you will get the actual Message object and not just the data field on the Message:

	def service(session, Message message) // The "from" Session and Message
	
If you return false from the method, the message will not be broadcast.  If you return nothing or true, the message will be broadcast.  If you return an object or message, that message will be delivered to the client that initialized the service call.

The actual use of this annotation would look thusly:

	@MessageListener("/foo/bar")
	def onFooBar(session, data) {
		// do something with the data
		
		[message: "thanks for the info mr. client"]
	}


## Client Resources
***
To add the basic jquery cometd plugin resources to your views:

#### TagLib

	<cometd:resources />
	
#### Resources Plugin

	<r:required module="cometd" />


### Configuration
***

The plugin is configured in Config.groovy, with options prefixed with "plugins.cometd". The following options are defined:

* **plugins.cometd.continuationFilter.disable**: if set, do not install the to [ContinuationFilter](http://download.eclipse.org/jetty/stable-7/apidocs/org/eclipse/jetty/continuation/ContinuationFilter.html)
* **plugins.cometd.asyncSupport.enable**:  if set, enable asynchronous support for Servlet 3.0 support (http://cometd.org/node/106)
* **plugins.cometd.init.params**: a map of init-name -> init-value pairs for an init-param element to be applied to the cometd servlet definition. Make sure you read the javadoc for org.cometd.bayeux.server.ServerTransport for how the init params are applied. Some useful settings include:
	* **timeout**: In the default long polling http transport is the period of time the server waits before answering to a long poll
	* **interval**: In the default long polling http transport is the period of time that the client waits between long polls
	* **maxInterval**: In the default long polling http transport is the period of time that must elapse before the server consider the client being lost

## Contributing
Contributions are welcome, preferably by pull request on GitHub
	
	git clone git@github.com:michetti/grails-cometd.git

	
This plugin is a fork from

	git clone git://github.com/marcusb/grails-cometd.git

## History


### Version 0.2.6.1
***
* Add support for resources plugin to import cometd js dependencies
* Fix an issue with getting serverSession

### Version 0.2.6
***
* Bump cometd dependecies to 2.3.1
* Taglib to import cometd js dependecies

### Version 0.2.1
***
* Install the [ContinuationFilter](http://download.eclipse.org/jetty/stable-7/apidocs/org/eclipse/jetty/continuation/ContinuationFilter.html) by default, it is needed for Tomcat 6

### Version 0.2
***
* Rewritten from scratch for CometD 2.0