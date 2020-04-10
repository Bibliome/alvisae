package fr.inrae.bibliome.ontolrws.Resources;

import fr.inrae.bibliome.ontolrws.JAXRSConfig;
import fr.inrae.bibliome.ontolrws.Settings.Ontology;
import fr.inrae.bibliome.ontolrws.Settings.Settings;
import fr.inrae.bibliome.ontolrws.Settings.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.owlapi.model.OWLClass;

/**
 *
 * @author fpa
 */
@Path("")

public class Resources {

    @GET
    @Path("api/version")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiVersion() {
        return Response.ok(Json.createObjectBuilder().add("version", 1.2).build()).build();
    }

    // [ A ]
    @GET
    @Path("user/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyProjectList(@Context ContainerRequestContext requestContext
    ) {
        User authUser = (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);

        JsonObject result = Json.createObjectBuilder()
                .add("id", authUser.getId())
                .add("login", authUser.getName())
                .add("projects", getUserProjects(authUser))
                .build();

        return Response.ok(result).build();
    }

    // [ B ]
    @GET
    @Path("users/{userid}/projects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjectList(
            @Context ContainerRequestContext requestContext,
            @PathParam("userid") int userId
    ) {

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

    // [ C ]
    @GET
    @Path("projects/{projectid}/semClass/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid
    ) {

        return serveSemanticClass(requestContext, projectid, semclassid, false);
    }

    // [ D ]
    @PUT
    @Path("projects/{projectid}/semClass/{semclassid}/ensureVersion")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClassEnsureVersion(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid
    ) {

        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ E ]
    @GET
    @Path("projects/{projectid}/semClassNTerms/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClassAndTerms(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid
    ) {

        return serveSemanticClass(requestContext, projectid, semclassid, true);
    }

    // [ F ]
    @GET
    @Path("projects/{projectid}/semClasses")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClassFromPattern(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @QueryParam("pattern") @DefaultValue("*") String pattern,
            @QueryParam("exactMatch") @DefaultValue("true") boolean exactmatch
    ) {

        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ G ]
    @GET
    @Path("projects/{projectid}/branches/fromSemClass/{fromclassid}/toSemClass/{toclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHyperonymyPaths(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("fromclassid") String fromclassid,
            @PathParam("toclassid") String toclassid
    ) {
        User authUser = (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);

        Optional<Ontology> ontology = app.getSettings().getOntologyForUser(authUser, projectid);
        if (!ontology.isPresent()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (OboOntoHandler ontoHnd = OboOntoHandler.getHandler(ontology.get())) {
            List<GraphPath<OWLClass, DefaultEdge>> path = null;
            try {
                path = ontoHnd.getHyperonymyPaths(fromclassid, toclassid);

            } catch (NoSuchElementException e) {
            }
            if (path == null || path.isEmpty()) {
                //weird, but it was like that in TyDI RestWS 
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JsonArrayBuilder levels = Json.createArrayBuilder();
            path.get(0).getVertexList().forEach(v -> {
                levels.add(getSemanticClassResult(ontoHnd, v, false, false));

            });
            path.clear();

            JsonObject result = Json.createObjectBuilder()
                    .add("fromGroup", getSemanticClassResult(ontoHnd, fromclassid, false, false))
                    .add("toGroup", getSemanticClassResult(ontoHnd, toclassid, false, false))
                    .add("paths", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("levels", Json.createArrayBuilder()
                                            .add(levels))))
                    .build();

            return Response.ok(result).build();
        }
    }

    // [ H ]
    @PUT
    @Path("projects/{projectid}/semClass/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response replaceClassHyperonym(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid,
            @FormParam("semClassVersion") long version, //?~ "Missing semClassVersion parameter" ~> 400 ;
            @FormParam("prevHyperSemClassId") String prevhyperid, //?~ "Missing prevHyperSemClassId parameter" ~> 400 ;
            @FormParam("prevHyperSemClassVersion") String prevhyperversion, //?~ "missing prevHyperSemClassVersion parameter" ~> 400;
            @FormParam("newHyperSemClassId") String newhyperid, //?~ "Missing newHyperSemClassId parameter" ~> 400;
            @FormParam("newHyperSemClassVersion") String newhyperversion //?~ "Missing newHyperSemClassVersion parameter" ~> 400);
    ) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ I ]
    @POST
    @Path("projects/{projectid}/term")
    @Produces(MediaType.APPLICATION_JSON)
    //Create a new Term and add it as a synonym to an existing Semantic Class
    public Response createTermAsSynonymToClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @FormParam("classId") String semclassid, //?~ "missing classID parameter" ~> 400;
            @FormParam("classVersion") long version, //?~ "missing classVersion parameter" ~> 400;
            @FormParam("surfaceForm") String surfForm, //?~ "missing surfaceForm parameter" ~> 400)
            @FormParam("lemmatizedForm") String lemma //Option

    ) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ J ]
    @POST
    @Path("projects/{projectid}/semClasses")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTermAsCanonicToNewClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @FormParam("hyperId") String semclassid, //?~ "missing hyperId parameter" ~> 400;
            @FormParam("classVersion") long version, //?~ "missing classVersion parameter" ~> 400;
            @FormParam("surfaceForm") String surfForm, //?~ "missing surfaceForm parameter" ~> 400)
            @FormParam("lemmatizedForm") String lemma, //Option
            @QueryParam("force") @DefaultValue("true") boolean force
    ) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ K ]
    @POST
    @Path("projects/{projectid}/semClassNTerms/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addExistingTermAsSynonymToClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid,
            @FormParam("termId") String termid, // ?~ "missing termId parameter" ~> 400;
            @FormParam("classVersion") long version //?~ "missing classVersion parameter" ~> 400
    ) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ L ]
    @POST
    @Path("projects/{projectid}/semClasses/merge")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mergeClasses(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @FormParam("semClassId1") String semclassid1, //?~ "Missing semClassId1 parameter" ~> 400 ;
            @FormParam("semClassVersion1") long version1, //?~ "Missing semClassVersion1 parameter" ~> 400 ;
            @FormParam("semClassId2") String semclassid2, //?~ "missing semClassId2 parameter" ~> 400;
            @FormParam("semClassVersion2") long version2 //?~ "Missing semClassVersion2 parameter" ~> 400)
    ) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ M ]
    @GET
    @Path("projects/{projectid}/changes/sinceversion/{fromversionnum}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChangesBetweenOntoVersions(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("fromversionnum") long fromversionnum,
            @QueryParam("semclassids") List<String> semclassids
    ) {

        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ N ]
    @POST
    @Path("projects/{projectid}/checkchanges")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkClassesChangesSinceVersion(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            String entityParam
    ) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
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

    private JsonObjectBuilder getSemanticClassResult(OboOntoHandler ontoHnd, OWLClass semClass, boolean withHypoDetails, boolean withTerms) {

        Structs.DetailSemClassNTerms classStruct;

        JsonObjectBuilder semClassResult = Json.createObjectBuilder();

        List<OWLClass> hypoClasses;

        if (semClass == null) {
            classStruct = new Structs.DetailSemClassNTerms();
            //virtual root class
            classStruct.groupId = OboOntoHandler.ROOT_ID;
            classStruct.canonicId = OboOntoHandler.ROOT_ID;
            classStruct.canonicLabel = "";
            //FIXME
            classStruct.version = 1;

            //actual root classes presented as hyponyms of the virtual root
            hypoClasses = ontoHnd.getRootSemanticClasses().collect(Collectors.toList());

        } else {

            //fill up Semantic class structure from OwlClass properties
            classStruct = ontoHnd.initSemClassStruct(semClass);
            //virtual canonic id
            classStruct.canonicId = classStruct.groupId + "-C";

            if (withTerms) {
                //produce virtual term ids
                IntStream.range(0, classStruct.termMembers.size()).forEach(
                        i -> classStruct.termMembers.get(i).id = classStruct.groupId + "-" + i
                );
                //add virtual canonic term
                Structs.Term canonic = Structs.Term.createCanonic(classStruct.canonicLabel);
                canonic.id = classStruct.canonicId;
                classStruct.termMembers.add(0, canonic);
            }

            classStruct.hypoGroupIds = ontoHnd.getHyperonymsOf(semClass)
                    .map(hyper -> OboOntoHandler.getSemClassIdOf(hyper))
                    .collect(Collectors.toList());
            if (classStruct.hypoGroupIds.isEmpty()) {
                //actual root classes are presented as hyperonym of the virtual root 
                classStruct.hypoGroupIds.add(OboOntoHandler.ROOT_ID);
            }

            hypoClasses = ontoHnd.getHyponymsOf(semClass).collect(Collectors.toList());
        }

        //Json serialization 
        semClassResult.add("groupId", classStruct.groupId);
        semClassResult.add("canonicId", classStruct.canonicId);
        semClassResult.add("canonicLabel", classStruct.canonicLabel);
        semClassResult.add("version", classStruct.version);

        JsonArrayBuilder hyperIds = Json.createArrayBuilder();
        classStruct.hypoGroupIds.forEach(hyperid -> hyperIds.add(hyperid));
        semClassResult.add("hyperGroupIds", hyperIds);

        JsonArrayBuilder hypoIds = Json.createArrayBuilder();

        if (withHypoDetails) {
            JsonObjectBuilder hypoDetails = Json.createObjectBuilder();
            hypoClasses.forEach(hypo -> {
                String hypoid = OboOntoHandler.getSemClassIdOf(hypo);
                hypoIds.add(hypoid);

                JsonObjectBuilder hypoResult = getSemanticClassResult(ontoHnd, hypoid, false, false);
                hypoDetails.add(hypoid, hypoResult);
            });

            semClassResult.add("hypoGroupsDetails", hypoDetails);

        } else {
            hypoClasses.forEach(hypo -> hypoIds.add(OboOntoHandler.getSemClassIdOf(hypo)));
        }
        semClassResult.add("hypoGroupIds", hypoIds);

        if (withTerms && !OboOntoHandler.isRootId(classStruct.groupId)) {
            JsonArrayBuilder termMembers = Json.createArrayBuilder();
            classStruct.termMembers.forEach(term -> {
                termMembers.add(Json.createObjectBuilder()
                        .add("termId", term.id)
                        .add("form", term.form)
                        .add("memberType", term.memberType)
                        .add("linkedTerms", Json.createArrayBuilder())
                        //FIXME
                        .add("englobingGroups", Json.createArrayBuilder().add(classStruct.groupId))
                );
            });

            semClassResult.add("termMembers", termMembers);
        }

        return semClassResult;
    }

    private JsonObjectBuilder getSemanticClassResult(OboOntoHandler ontoHnd, String semclassid, boolean withHypoDetails, boolean withTerms) {
        OWLClass semClass = null;
        if (!OboOntoHandler.isRootId(semclassid)) {
            //only 1 single class expected to match 1 Id!
            semClass = ontoHnd.getSemanticClassesForId(semclassid).findFirst().get();
        }
        return getSemanticClassResult(ontoHnd, semClass, withHypoDetails, withTerms);
    }

    private Response serveSemanticClass(ContainerRequestContext requestContext, String projectid, String semclassid, boolean withTerms) {
        User authUser = (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);

        Optional<Ontology> ontology = app.getSettings().getOntologyForUser(authUser, projectid);
        if (!ontology.isPresent()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try (OboOntoHandler ontoHnd = OboOntoHandler.getHandler(ontology.get())) {
            JsonObjectBuilder result = getSemanticClassResult(ontoHnd, semclassid, true, withTerms);
            return Response.ok(result.build()).build();
        }
    }

}
