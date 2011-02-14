package test

import grails.converters.*


class BController {

    def get = {
        render B.get(params.id) as XML
    }
    
    def list = {
        render B.list() as XML
    }
    
    def put = {
        def b = params.id ? B.get(params.id) : new B()
        b.value = params.value
        b.save(flush:true)        
        if (params.error) {
            throw getClass().getClassLoader().loadClass(params.error).newInstance()
        }
        render b as XML
    }
}
