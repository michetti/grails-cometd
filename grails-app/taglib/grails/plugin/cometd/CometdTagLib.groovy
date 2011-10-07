package grails.plugin.cometd

class CometdTagLib {
	
	static namespace = "cometd"
	
	def jquery = { attrs, body ->
		out << "${g.javascript(library: 'cometd/org/cometd', plugin: 'cometd')}"
		out << "${g.javascript(library: 'cometd/jquery/jquery.json-2.2', plugin: 'cometd')}"
		out << "${g.javascript(library: 'cometd/jquery/jquery.cometd', plugin: 'cometd')}"
	}

}
