/*
* Copyright 2007-2008 Predrag Knezevic
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

import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.support.TransactionCallback

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU

class TransactionalControllerGrailsPlugin {
    // the plugin version
    def version = "0.1.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]
    def observe = ['controllers']
    def loadAfter = ['controllers']
    
    def author = "Predrag Knezevic"
    def authorEmail = "pedjak@gmail.com"
    def title = "Transactional Controller Plugin"
    def description = '''\\
It enables executing controller's actions within a transaction, rolling it back, 
if an error occurs. The plugin produces the same effect as when the action's code 
would be wrapped with a domain class withTransaction method.
'''

    // URL to the plugin's documentation
    def documentation = "http://github.com/pedjak/grails-transactional-controller"

    
    static String CONTROLLER_TX_FIELD = "transactional"
    
    def doWithDynamicMethods = { ctx ->
        configureControllers(ctx, application)
    }

    private void configureControllers(ctx, application) {
        for(cc in application.controllerClasses) {
            def controllerClass = cc.clazz
            def mc = controllerClass.metaClass
            def transactional = GCU.getStaticPropertyValue(controllerClass, CONTROLLER_TX_FIELD)
            def configProperty = application.config.controller.actions."${CONTROLLER_TX_FIELD}"
            def defaultActionTransactional = configProperty instanceof Closure ? configProperty.call(cc) : false
             
            if ((defaultActionTransactional && transactional != false)
                || (transactional != null && transactional != false)) {
                decorateClosures(controllerClass, mc, ctx)
            }
        }
    }
    boolean isInternalCall(stes, Class clazz) {

        String cmp = "${clazz.name}\$"        
        boolean internalCall = false
        int l = stes.length
        int i = 0;
        while (i<l) {
            if (stes[i].className.startsWith(cmp)) {
              internalCall = true
              break
            }
            i++
        }
        return internalCall
    }
    
    void decorateClosures(Class clazz, MetaClass mc, ctx) {
        def actionNameList = GCU.getStaticPropertyValue(clazz, CONTROLLER_TX_FIELD)
        if (actionNameList != null && !(actionNameList instanceof List)) {
             actionNameList = null
        }
        mc.invokeMethod = { String name, args ->
            def o 
            def mm = delegate.metaClass.getMetaMethod(name, args)            
            // if regular method, just invoke it
            if (mm) {
                o = mm.invoke(delegate, args)
            } else {
            
                // check if this is a closure call
                def mp = delegate.metaClass.getMetaProperty(name)
                // get the property
                if (mp) {
                    def v = mp.getProperty(delegate)
                    boolean internalCall = isInternalCall(Thread.currentThread().getStackTrace(), clazz)
                    if (v instanceof Closure) {
                        // do not wrap with transactions if invoked within controller, or action not specified in the transactional list
                        if (internalCall && (actionNameList == null || name in actionNameList)) {                            
                            v.metaClass.getMetaMethod("call", args).invoke(v, args)
                        } else {
                            def request = delegate.request
                            o = new TransactionTemplate(ctx.getBean('transactionManager')).execute({status ->
                                request['txstatus'] = status
                                try {
                                    def ret = v.metaClass.getMetaMethod("call", args).invoke(v, args)
                                    ctx.getBean('sessionFactory').currentSession.flush()
                                    return ret
                                } catch (Throwable t) {
                                    status.setRollbackOnly()
                                    throw t
                                }
                            } as TransactionCallback)
                        }
                        
                    } else {
                        throw new MissingMethodException(name, clazz, args)
                    }
                } else {
                    throw new MissingMethodException(name, clazz, args)
                }
            }
            return o
        }
        
        mc.getProperty = { String name ->
            def mp = delegate.metaClass.getMetaProperty(name)
            if (mp) {
                def v = mp.getProperty(delegate)
                boolean internalCall = isInternalCall(Thread.currentThread().getStackTrace(), clazz)
                if (v instanceof Closure && !internalCall && (actionNameList == null || name in actionNameList)) {
                    // it is closure, so wrap it
                    def request = delegate.request
                    
                    def cl = { Object[] args ->
                        new TransactionTemplate(ctx.getBean('transactionManager')).execute({status ->
                            request['txstatus'] = status
                            try {
                                def ret = v.metaClass.getMetaMethod("call", args).invoke(v, args)
                                ctx.getBean('sessionFactory').currentSession.flush()
                                return ret
                            } catch (Throwable t) {
                                status.setRollbackOnly()
                                throw t
                            }
                        } as TransactionCallback)
                        
                    }
                    return cl
          
                } else {
                    // just return property value
                    return v
                }
            } else {
                throw new MissingFieldException(name, clazz)
            }
            
        }
    }
    
    
    def onChange = {event ->
        if (event.source && application.isControllerClass(event.source)) {        
            def context = event.ctx
            if (!context) {
                if (log.isDebugEnabled()) {
                    log.debug("Application context not found. Can't reload")
                }

                return
            }
            configureControllers(event.ctx, application)            
        }
    }

}
