package fr.inrae.bibliome.ontolrws.Resources;

import fr.inrae.bibliome.ontolrws.JAXRSConfig;
import fr.inrae.bibliome.ontolrws.Settings.Ontology;
import fr.inrae.bibliome.ontolrws.Settings.Settings;
import fr.inrae.bibliome.ontolrws.Settings.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.semanticweb.owlapi.model.OWLClass;

/**
 *
 * @author fpa
 */
@Path("")

public class Resources {

    @GET
    @Path("user/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyProjectList(@Context ContainerRequestContext requestContext) {
        User authUser = (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);

        JsonObject result = Json.createObjectBuilder()
                .add("id", authUser.getId())
                .add("login", authUser.getName())
                .add("projects", getUserProjects(authUser))
                .build();

        return Response.ok(result).build();
    }

    @GET
    @Path("users/{userid}/projects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjectList(
            @Context ContainerRequestContext requestContext,
            @PathParam("userid") int userId) {

        User authUser = (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);

        User subjectUser;
        if (authUser.getId() == userId) {
            subjectUser = authUser;
        } else {
            Optional<User> subUserOpt = app.getSettings().getUser(userId);
            if (subUserOpt.isPresent()) {
                subjectUser = subUserOpt.get();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
        }

        JsonArray result = getUserProjects(subjectUser).build();

        return Response.ok(result).build();
    }

    @GET
    @Path("projects/{projectid}/semClass/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid) {

        User authUser = (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);

        Optional<Ontology> ontology = app.getSettings().getOntologyForUser(authUser, projectid);
        if (!ontology.isPresent()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (OboOntoHandler ontoHnd = OboOntoHandler.getHandler(ontology.get())) {
            JsonObjectBuilder result = getSemanticClassResult(ontoHnd, semclassid, true);
            return Response.ok(result.build()).build();
        }
    }


    // -------------------------------------------------------------------------
    @Context
    private JAXRSConfig app;

    private JsonArrayBuilder getUserProjects(User authUser) {

        JsonArrayBuilder projects = Json.createArrayBuilder();
        app.getSettings().getOntologiesForUser(authUser)
                .forEach(o -> projects.add(Json.createObjectBuilder()
                .add("id", o.getId())
                .add("name", o.getLongName())
        ));
        return projects;
    }

    private JsonObjectBuilder getSemanticClassResult(OboOntoHandler ontoHnd, String semclassid, boolean withHypoDetails) {

        JsonObjectBuilder semClassResult = Json.createObjectBuilder();
        JsonArrayBuilder hyperIds = Json.createArrayBuilder();
        List<OWLClass> hypoClasses;

        if (OboOntoHandler.isRootId(semclassid)) {
            //virtual root class
            semClassResult.add("groupId", OboOntoHandler.ROOT_ID);
            semClassResult.add("canonicId", OboOntoHandler.ROOT_ID);
            semClassResult.add("canonicLabel", "");
            //FIXME
            semClassResult.add("version", 1);

            //actual root classes presented as hyponyms of the virtual root
            hypoClasses = ontoHnd.getRootSemanticClasses().collect(Collectors.toList());
            
        } else {
            
            //only on class expected for an Id!
            OWLClass semClass = ontoHnd.getSemanticClassesForId(semclassid).findFirst().get();

            ontoHnd.classProps(semClass).forEach(pkv -> {
                if ("id".equals(pkv.getKey())) {
                    semClassResult.add("groupId", pkv.getValue());
                    semClassResult.add("canonicId", pkv.getValue());
                    //FIXME
                    semClassResult.add("version", 1);

                } else if ("label".equals(pkv.getKey())) {
                    semClassResult.add("canonicLabel", pkv.getValue());

                }
            });

            List<String> hyperIdList = ontoHnd.getHyperonymsOf(semClass)
                    .map(hyper -> ontoHnd.getSemClassIdOf(hyper))
                    .collect(Collectors.toList());

            if (hyperIdList.isEmpty()) {
                //actual root classes are presented as hyperonym of the virtual root 
                hyperIds.add(OboOntoHandler.ROOT_ID);
            } else {
                hyperIdList.stream().forEach(hyperid -> hyperIds.add(hyperid));
            }

            hypoClasses = ontoHnd.getHyponymsOf(semClass).collect(Collectors.toList());
        }
        semClassResult.add("hyperGroupIds", hyperIds);

        JsonArrayBuilder hypoIds = Json.createArrayBuilder();

        if (withHypoDetails) {
            JsonObjectBuilder hypoDetails = Json.createObjectBuilder();
            hypoClasses.forEach(hypo -> {
                String hypoid = OboOntoHandler.getSemClassIdOf(hypo);
                hypoIds.add(hypoid);

                JsonObjectBuilder hypoResult = getSemanticClassResult(ontoHnd, hypoid, false);
                hypoDetails.add(hypoid, hypoResult);
            });

            semClassResult.add("hypoGroupsDetails", hypoDetails);

        } else {
            hypoClasses.forEach(hypo -> hypoIds.add(OboOntoHandler.getSemClassIdOf(hypo)));
        }
        semClassResult.add("hypoGroupIds", hypoIds);

        return semClassResult;
    }

}
