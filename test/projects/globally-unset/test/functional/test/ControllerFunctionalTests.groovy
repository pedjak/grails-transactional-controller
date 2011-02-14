package test

class ControllerFunctionalTests extends functionaltestplugin.FunctionalTestCase {
    def parser
    def response
    
    protected void setUp() {
        super.setUp()
        parser = new XmlParser()
    }
    
    void testAControllerWithTransactionalFieldSetToTrue() {
        // put one
        get("/a/put?value=v1")
        assertStatus 200
        
        response = parser.parseText(page.content)
        def id = response['@id']
        
        // update it
        get("/a/put?value=v2&id=${id}")
        assertStatus 200
        
        response = parser.parseText(page.content)
        
        // simulate error by update
        get("/a/put?value=v3&id=${id}&error=${Throwable.name}")
        assertStatus 500
        
        // simulate error by insert
        get("/a/put?value=v4&error=${Throwable.name}")
        assertStatus 500
        
        // we should have still only one item at the end in DB
        // update it
        get("/a/list")
        assertStatus 200
        
        response = parser.parseText(page.content)
        assertEquals(1, response.a.size())
        assertEquals(id, response.a[0]['@id'])
        assertEquals("v2", response.a[0].value[0].text())
    }
    
    void testBControllerWithoutTransactionalFieldButGloballyUnset() {
        // put one
        get("/b/put?value=v1")
        assertStatus 200
        
        response = parser.parseText(page.content)
        def id = response['@id']
        
        // update it
        get("/b/put?value=v2&id=${id}")
        assertStatus 200
        
        response = parser.parseText(page.content)
        
        // simulate error by insert
        get("/b/put?value=v4&error=${Throwable.name}")
        assertStatus 500
        
        // we should have still only one item at the end in DB
        // update it
        get("/b/list")
        assertStatus 200
        
        response = parser.parseText(page.content)
        assertEquals(2, response.b.size())        
    }
    
    void testCControllerWithTransactionalFieldSetToFalse() {
        // put one
        get("/c/put?value=v1")
        assertStatus 200
        
        response = parser.parseText(page.content)
        def id = response['@id']
        
        // update it
        get("/c/put?value=v2&id=${id}")
        assertStatus 200
        
        response = parser.parseText(page.content)
        
        
        // simulate error by insert
        get("/c/put?value=v4&error=${Throwable.name}")
        assertStatus 500
        
        // we have two items unfortunatelly now
        // update it
        get("/c/list")
        assertStatus 200
        
        response = parser.parseText(page.content)
        assertEquals(2, response.c.size())
    }
    
    void testDControllerWithTransactionalFieldAsListOfActionNames() {
        // put one
        get("/d/put?value=v1")
        assertStatus 200
        
        response = parser.parseText(page.content)
        def id = response['@id']
        
        // update it
        get("/d/put?value=v2&id=${id}")
        assertStatus 200
        
        response = parser.parseText(page.content)
        
        
        // simulate error by insert
        get("/d/put?value=v4&error=${Throwable.name}")
        assertStatus 500
        
        // simulate error by insert
        get("/d/putTx?value=v5&error=${Throwable.name}")
        assertStatus 500
        
        // we have two items unfortunatelly now
        // update it
        get("/d/list")
        assertStatus 200
        
        response = parser.parseText(page.content)
        assertEquals(2, response.d.size())
        ["v2", "v4"].each { v ->
            assertEquals(1, response.d.grep { it.value.text() == v }.size())
        }
             
    }
}
