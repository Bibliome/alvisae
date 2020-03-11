# Pairing the UI with the corresponding web services

The pairing of Alvis UI and the web services is made easy by enforcing simple configuration rules.

### Alvis UI and its web services endpoints must :

1. have the same origin (ie.e. protocol+port+domain), so there's no need of any configuration on the server to allow the web browser to permit invocation of web services from the UI (see [SOP](http://en.wikipedia.org/wiki/Same_origin_policy),
2. share the same instance name,
3. share the same url prefix (to allow deployement of several instances can be deployed on the same server).



### The table below sums up the configuration for an instance called `NEWINSTANCE`

 Component | web application name (suggestion) | context root 
:---:|:---:|:--- 
*AlvisAE* client UI|`AlvisAE-NEWINSTANCE`|__`/alvisae/NEWINSTANCE/`__`AlvisAE`
*AlvisAE* web service|`cdxws-NEWINSTANCE`|__`/alvisae/NEWINSTANCE`__
*TyDI* web service extension (__suggestion__) |`tydiws-NEWINSTANCE`|`/tydirws/NEWINSTANCE`


Note : 

* actually, web services for the *TyDI* extension (Termino/Ontology) just need to follow the same origin, since the actual url of the web services is explicitely indicated in the annotation schema.
