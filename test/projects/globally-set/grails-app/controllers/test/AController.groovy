package test

import grails.converters.*

class AController {

    static transactional = true
    
    def get = {
        
        render A.get(params.id) as XML  
    }
    
    def list = {
        render A.list() as XML
    }
    
    def put = {
        def a = params.id ? A.get(params.id) : new A()
        a.value = params.value
        a.save(flush:true)
        if (params.error) {
            throw getClass().getClassLoader().loadClass(params.error).newInstance()
        }
        render a as XML
    }
    
    
    
    
}
