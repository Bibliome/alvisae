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
    public Response getMyProjectList(@Context ContainerRequestContext requestContext) {
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

    // [ C ]
    @GET
    @Path("projects/{projectid}/semClass/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid) {

        return serveSemanticClass(requestContext, projectid, semclassid, false);
    }

    // [ D ]
    @PUT
    @Path("projects/{projectid}/semClass/{semclassid}/ensureVersion")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClassEnsureVersion(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid) {

        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    // [ E ]
    @GET
    @Path("projects/{projectid}/semClassNTerms/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSemanticClassAndTerms(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid) {

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
            @PathParam("toclassid") String toclassid) {

        return serveSemanticClass(requestContext, projectid, fromclassid, true);
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
            @QueryParam("semclassids") List<String> semclassids) {

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

    private JsonObjectBuilder getSemanticClassResult(OboOntoHandler ontoHnd, String semclassid, boolean withHypoDetails, boolean withTerms) {

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

            //only 1 single class expected to match 1 Id!
            OWLClass semClass = ontoHnd.getSemanticClassesForId(semclassid).findFirst().get();

            JsonArrayBuilder terms = Json.createArrayBuilder();

            ontoHnd.classProps(semClass).forEach(pkv -> {
                if ("id".equals(pkv.getKey())) {
                    semClassResult.add("groupId", pkv.getValue());
                    semClassResult.add("canonicId", pkv.getValue());
                    //FIXME
                    semClassResult.add("version", 1);

                } else if ("label".equals(pkv.getKey())) {
                    semClassResult.add("canonicLabel", pkv.getValue());

                } else if (withTerms) {
                    if ("relsyn".equals(pkv.getKey())) {
                        JsonObjectBuilder term = Json.createObjectBuilder();
                        term.add("termId", 99999)
                                .add("form", pkv.getValue())
                                //FIXME ??
                                .add("memberType", 15)
                                //FIXME ??
                                .add("linkedTerms", Json.createArrayBuilder())
                                //FIXME ??
                                .add("englobingGroups", Json.createArrayBuilder().add(semclassid));
                        terms.add(term);
                    }
                }
            });

            if (withTerms) {
                semClassResult.add("termMembers", terms);
            }

            List<String> hyperIdList = ontoHnd.getHyperonymsOf(semClass)
                    .map(hyper -> OboOntoHandler.getSemClassIdOf(hyper))
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

                JsonObjectBuilder hypoResult = getSemanticClassResult(ontoHnd, hypoid, false, false);
                hypoDetails.add(hypoid, hypoResult);
            });

            semClassResult.add("hypoGroupsDetails", hypoDetails);

        } else {
            hypoClasses.forEach(hypo -> hypoIds.add(OboOntoHandler.getSemClassIdOf(hypo)));
        }
        semClassResult.add("hypoGroupIds", hypoIds);

        return semClassResult;
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
