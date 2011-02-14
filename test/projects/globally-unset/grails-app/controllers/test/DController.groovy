package test

import grails.converters.*

class DController {

    static transactional = ["putTx"]
    
    def get = {
        
        render D.get(params.id) as XML
    }
    
    def list = {
        render D.list() as XML
    }
    
    def put = {
        def d = params.id ? D.get(params.id) : new D()
        d.value = params.value
        d.save()
        if (params.error) {
            throw getClass().getClassLoader().loadClass(params.error).newInstance()
        }
        render d as XML
    }
    
    def putTx = {
        put()
    }
}
