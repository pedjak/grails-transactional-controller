package test

import grails.converters.*

class CController {

    static transactional = false
    
    def get = {
        
        render C.get(params.id) as XML
    }
    
    def list = {
        render C.list() as XML
    }
    
    def put = {
        def c = params.id ? C.get(params.id) : new C()
        c.value = params.value
        c.save(flush:true)
        if (params.error) {
            throw getClass().getClassLoader().loadClass(params.error).newInstance()
        }
        render c as XML
    }
}
