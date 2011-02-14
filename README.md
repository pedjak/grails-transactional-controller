Summary
=======

The plugin enables executing controller's actions within a transaction, rolling it back, 
if an error occurs. The plugin adds semantically the same effect as when the action's code 
would be wrapped with a domain class withTransaction method.

Why
===

Many examples demonstrate how to add/update instances
of domain classes within a controller's action, but you should 
understand that this could leave the database in an inconsistent 
state, if an exception occurs. Consider the following example:

    class Controller {

      def update = {
        new A(value:params.valueA).save(validate:true, flush:true)
        new B(value:params.valueB).save(validate:true, flush:true)
      }

    }

Imagine now that an instance of B is not valid for some provided values.
Save rises the exception, but saving of A will not be rolled back.
These are the situations where the plugin is useful.

How
===

* assigning a closure to controller.actions.transactional property in Config.groovy. 
The closure receives as parameter an instance of GrailsControllerClass and should return true,
if the controller's actions should be made transactional. For example:

    controller.actions.transactional = { cc ->
	  cc.name == "Foo" ? true : false
    }  

The above statement specifies that the actions of FooController only should be made transactional.
Configuring via the application config is useful when we need to tweak controllers that are part of
some plugins we use.

CAUTION: a statement like controller.actions.transactional = { true } is valid, but not recommended, 
because it would significantly impact the application performances. In general, only actions that might 
leave the database in an inconsistent state should be made transactional.
 

* adding the following static field in the controller and setting
its value to 'true':

    static transactional = true

By setting the field's value to 'false', controller's actions will be not executed
within transactions (default Grails behavior anyway).

The field can accept as well a list of strings representing names of actions 
that should be transactional:

    class SampleController {

      static transactional = ["add", "update"]

      def add = { }

      def update = { }

      def get = { }
    } 

Similar to withTransaction method, transactional actions can query 
and edit the status of the transaction at runtime by accessing 
an additional injected request.txstatus property (an instance of Spring's TransactionStatus)
