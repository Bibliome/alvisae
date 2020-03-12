# AlvisAE

AlvisAE is an editor that facilitates the annotation of text documents with the goal of extracting knowledge.

This project containsAlvisAE two main components: an Annotation Editor (alvisae-ui)  and an Web Service (alvisae-ws). 

## Try it...

> Note that, you must have [docker](https://www.docker.com/) installed

1. Run the following command
```
sudo docker run -d --rm --name alvisae.ws -p 8080:8080 -p 5432:5432  bibliome/alvisae:1.0.1
``` 

 2.1. Access Using Web Interface
   * Go to [http://localhost:8080/alvisae/alvisae-ws/AlvisAE/](http://localhost:8080/alvisae/alvisae-ws/AlvisAE)
   * Sing-In with login *annotator1* and password *annotator1*
  
  For help see the following [user guide](https://github.com/openminted/alvisae/blob/master/docs/user-guide.md)


2.2. Play with REST api

The web services implement the [AERO protocol](https://github.com/openminted/omtd-aero), their usage is detailed [here](alvisae-ws/documentation/ws-aero-usage.md).
