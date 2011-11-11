/*
 * Copyright © 2010 MBTE Sweden AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.util.Environment

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.ServiceArtefactHandler

import org.cometd.server.BayeuxServerImpl
import org.cometd.server.CometdServlet
import org.cometd.bayeux.server.BayeuxServer
import grails.plugin.cometd.ServiceCometdProcessor

import org.springframework.web.context.support.ServletContextAttributeExporter

import org.apache.commons.logging.LogFactory

class CometdGrailsPlugin {
    def version = "0.2.6.1"
    def grailsVersion = "1.2.1 > *"
    def dependsOn = [:]
    def pluginExcludes = [
        'grails-app/services/**/test/',
        'grails-app/views/error.gsp',
        'scripts'
    ]

    def author = 'Marcus Better'
    def authorEmail = 'marcus@better.se'
    def title = 'Ajax push support using CometD'
    def description = '''\
CometD is a scalable HTTP-based event routing bus that uses a Ajax Push technology pattern known as Comet.
This plugin allows your Grails application to send asynchronous notifications to HTTP clients using
CometD and the Bayeux protocol.
    '''
    def documentation = "http://www.grails.org/plugin/cometd"
    def loadAfter = ['services', 'controllers']
    def observe = ['services', 'controllers']

    def doWithWebDescriptor = { xml ->
        def conf = ConfigurationHolder.config.plugins.cometd
        
        LOG.debug("Initializing with continutationFilter: ${!conf.continuationFilter.disable}")
        if (!conf.continuationFilter.disable) {
            def filters = xml.'filter'
            filters[filters.size() - 1] + {
                filter {
                    'filter-name'('continuation')
                    'filter-class'('org.eclipse.jetty.continuation.ContinuationFilter')
                }
            }
            
            def filterMappings = xml.'filter-mapping'
            filterMappings[filterMappings.size() - 1] + {
                'filter-mapping' {
                    'filter-name'('continuation')
                    'servlet-name'('cometd')
                }
            }
        }
        
        def servlets = xml.'servlet'
        servlets[servlets.size() - 1] + {
            servlet {
                'servlet-name'('cometd')
                'servlet-class'(CometdServlet.class.name)
                
                // Add Servlet 3.0 Async Support, based on config file
                if(conf.asyncSupport.enable){
                    LOG.debug("Enabling Servlet 3.0 Async Support")
                    'async-supported'('true')
                }

                // Add servlet init params from the config file
                if (conf.init?.params) {
                    LOG.debug("Initializing with init-params: ${conf.init.params}")
                    conf.init.params.each { key, value ->
                        'init-param' {
                            'param-name'(key)
                            'param-value'(value)
                        }
                    }
                }
            }
        }

        def mappings = xml.'servlet-mapping'
        mappings[mappings.size() - 1] + {
            'servlet-mapping' {
                'servlet-name'('cometd')
                'url-pattern'('/cometd/*')
            }
        }
    }

    def doWithSpring = {
        bayeux(BayeuxServerImpl) { bean ->
            bean.destroyMethod = 'stop'
        }

        // the CometdServlet will pick up the Bayeux object from the servlet context
        bayeuxAttributeExporter(ServletContextAttributeExporter) {
            attributes = [(BayeuxServer.ATTRIBUTE): ref('bayeux')]
        }
    }

    def processor = new ServiceCometdProcessor()

    def doWithDynamicMethods = { context ->     
        application.serviceClasses?.each { service ->
            processor.process(service.referenceInstance, context)
        }
    }

    def onChange = { event ->
        if (application.isServiceClass(event.source)) {
            def artefact = application.addArtefact(ServiceArtefactHandler.TYPE, event.source)
            
            processor.process(artefact.referenceInstance, event.ctx)
        }
    }
}
