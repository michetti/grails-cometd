modules = {
	cometd {
		dependsOn 'jquery'
		resource url: [plugin: 'cometd', dir: 'js/cometd/org', file: 'cometd.js']
		resource url: [plugin: 'cometd', dir: 'js/cometd/jquery', file: 'jquery.json-2.2.js']
		resource url: [plugin: 'cometd', dir: 'js/cometd/jquery', file: 'jquery.cometd.js']
	}
	
}
