package fr.inrae.bibliome.ontolrws.Resources;

import fr.inrae.bibliome.ontolrws.JAXRSConfig;
import fr.inrae.bibliome.ontolrws.Settings.Ontology;
import fr.inrae.bibliome.ontolrws.Settings.Settings;
import fr.inrae.bibliome.ontolrws.Settings.User;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.semanticweb.owlapi.model.OWLOntologyChange;

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
        User authUser = getAuthUser(requestContext);

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

        User authUser = getAuthUser(requestContext);

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
            @QueryParam("pattern") @DefaultValue("") String pattern,
            @QueryParam("exactMatch") @DefaultValue("true") boolean exactmatch
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

            JsonArrayBuilder result = Json.createArrayBuilder();
            Collator currlocCollator = Collator.getInstance();

            ontoHnd.getClassesIdForMatchingLabelPattern(pattern)
                    .map(semClassId -> {
                        OWLClass semClass = ontoHnd.getSemanticClassesForId(semClassId).findFirst().get();
                        return getSemanticClassData(ontoHnd, semClass, false);
                    })
                    .sorted(
                            (Structs.DetailSemClassNTerms sc1, Structs.DetailSemClassNTerms sc2)
                            -> currlocCollator.compare(sc1.canonicLabel, sc2.canonicLabel)
                    )
                    .forEach(sc -> result.add(getSemanticClassResult(ontoHnd, sc, false, false))
                    );

            return Response.ok(result.build()).build();
        });
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
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

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
            List<OWLClass> vertexes = path.get(0).getVertexList();
            vertexes.subList(0, vertexes.size() - 1).forEach(v -> {
                levels.add(getSemanticClassResult(ontoHnd, v, false, false));

            });
            path.clear();

            JsonObject result = Json.createObjectBuilder()
                    .add("fromGroup", getSemanticClassResult(ontoHnd, fromclassid, false, false))
                    .add("toGroup", getSemanticClassResult(ontoHnd, toclassid, false, false))
                    .add("paths", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("levels", levels)))
                    .build();

            return Response.ok(result).build();
        });
    }

    // [ H ]
    @PUT
    @Path("projects/{projectid}/semClass/{semclassid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response replaceClassHyperonym(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("semclassid") String semclassid,
            @FormParam("semClassVersion") int version, //?~ "Missing semClassVersion parameter" ~> 400 ;
            @FormParam("prevHyperSemClassId") String prevhyperid,
            @FormParam("prevHyperSemClassVersion") int prevhyperversion, //?~ "missing prevHyperSemClassVersion parameter" ~> 400;
            @FormParam("newHyperSemClassId") String newhyperid,
            @FormParam("newHyperSemClassVersion") int newhyperversion //?~ "Missing newHyperSemClassVersion parameter" ~> 400);
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

            return checkSemClassExists(ontoHnd, semclassid, semClass -> {

                List<OWLOntologyChange> changes = new ArrayList<>();

                try {
                    changes.addAll(ontoHnd.testAndSetSemClassVersion(semClass, version));

                    if (!OboOntoHandler.isRootId(prevhyperid)) {
                        Optional<OWLClass> prevHyperOpt = ontoHnd.getSemanticClassesForId(prevhyperid).findFirst();
                        if (!prevHyperOpt.isPresent()) {
                            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity("Missing or invalid prevHyperSemClassId parameter").build();
                        }

                        changes.addAll(ontoHnd.testAndSetSemClassVersion(prevHyperOpt.get(), prevhyperversion));
                        try {
                            changes.add(ontoHnd.createRemoveHyponymyChange(prevHyperOpt.get(), semClass));

                        } catch (NoSuchElementException e) {
                            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity("Previous nyperonymy link not found!").build();
                        }

                    }
                    if (!OboOntoHandler.isRootId(newhyperid)) {
                        //Cycles may appear when creating a new hyponymy link : check that it won't, or report the error 

                        //Do NOT link a class to itself!
                        if (newhyperid.equals(semclassid)) {
                            return Response.status(UNPROCESSABLE).entity("Cannot link a class to itself!").build();
                        } else {

                            Optional<OWLClass> newHyperOpt = ontoHnd.getSemanticClassesForId(newhyperid).findFirst();
                            if (!newHyperOpt.isPresent()) {
                                return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity("Missing or invalid newHyperSemClassId parameter").build();
                            }

                            //if the specified new direct Hyperonym has already the specified semantic class for hyperonym (direct or not) then adding this link will create a cycle in the graph
                            if (ontoHnd.isHyponymsOf(semclassid, newhyperid)) {
                                return Response.status(UNPROCESSABLE).entity("Linking these classes would create a cycle!").build();
                            }

                            changes.addAll(ontoHnd.testAndSetSemClassVersion(newHyperOpt.get(), newhyperversion));
                            changes.add(ontoHnd.createAddHyponymyChange(newHyperOpt.get(), semClass));
                        }
                    }

                    return applyChangesAndSaveOnto(ontoHnd, changes, semclassid);

                } catch (OboOntoHandler.StaleVersionException ex) {
                    return Response.status(UNPROCESSABLE).entity(ex.getMessage()).build();
                }

            });
        });
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
            @FormParam("classVersion") int version, //?~ "missing classVersion parameter" ~> 400;
            @FormParam("surfaceForm") String surfForm, //?~ "missing surfaceForm parameter" ~> 400)
            @FormParam("lemmatizedForm") String lemma //Option

    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

            return checkSemClassExists(ontoHnd, semclassid, semClass -> {

                List<OWLOntologyChange> changes = new ArrayList<>();
                try {
                    changes.addAll(ontoHnd.testAndSetSemClassVersion(semClass, version));
                    //only surface form is stored in Obo ontology
                    changes.add(ontoHnd.createAddTermToClassChange(semClass, surfForm));

                    return applyChangesAndSaveOnto(ontoHnd, changes, semclassid);
                } catch (OboOntoHandler.StaleVersionException ex) {
                    return Response.status(UNPROCESSABLE).entity(ex.getMessage()).build();
                }
            });
        });
    }

    // [ J ]
    @POST
    @Path("projects/{projectid}/semClasses")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTermAsCanonicToNewClass(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @FormParam("hyperId") String semclassid, //?~ "missing hyperId parameter" ~> 400;
            @FormParam("classVersion") int version, //?~ "missing classVersion parameter" ~> 400;
            @FormParam("surfaceForm") String surfForm, //?~ "missing surfaceForm parameter" ~> 400)
            @FormParam("lemmatizedForm") String lemma, //Option
            @QueryParam("force") @DefaultValue("true") boolean force
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        });
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
            @FormParam("classVersion") int version //?~ "missing classVersion parameter" ~> 400
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {
            //This service can not be implemented because there's no termId stored in Ontology file (OBO)
            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        });
    }

    // [ L ]
    @POST
    @Path("projects/{projectid}/semClasses/merge")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mergeClasses(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @FormParam("semClassId1") String semclassid1, //?~ "Missing semClassId1 parameter" ~> 400 ;
            @FormParam("semClassVersion1") int version1, //?~ "Missing semClassVersion1 parameter" ~> 400 ;
            @FormParam("semClassId2") String semclassid2, //?~ "missing semClassId2 parameter" ~> 400;
            @FormParam("semClassVersion2") int version2 //?~ "Missing semClassVersion2 parameter" ~> 400)
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        });
    }

    // [ M ]
    @GET
    @Path("projects/{projectid}/changes/sinceversion/{fromversionnum}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChangesBetweenOntoVersions(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            @PathParam("fromversionnum") int fromversionnum,
            @QueryParam("semclassids") List<String> semclassids
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {
            //This service can not be implemented because there's no tracking of changes in the Ontology file (OBO)
            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        });
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
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {
            //This service can not be implemented because there's no tracking of changes in the Ontology file (OBO)
            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        });
    }

    @GET
    @Path("projects/{projectid}/getfile")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadOntology(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid
    ) {
        return checkUserIsAdmin(requestContext, projectid, (authUser, ontoHnd) -> {

            File file = ontoHnd.getOntoFilePath();
            return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
                    .build();
        });
    }

    @PUT
    @Path("projects/{projectid}")
    public Response uploadOntology(
            @Context ContainerRequestContext requestContext,
            @PathParam("projectid") String projectid,
            InputStream entityParam
    ) {
        User authUser = getAuthUser(requestContext);
        if (authUser.isIsAdmin()) {

            File targetFile;

            //check if the specified id already exists
            Optional<Ontology> ontology = app.getSettings().getOntology(projectid);
            if (ontology.isPresent()) {
                //replace existing file...
                targetFile = new File(ontology.get().getFilePath());
                //... but create backup copy just in case
                OboOntoHandler.createBackup(targetFile.toPath(), "_BeforeUpload");

            } else {
                String uploadFolder = app.getSettings().getUploadFolder();
                File uploadFolderFile = null;
                if (uploadFolder == null || uploadFolder.isEmpty()) {
                    logger.log(Level.SEVERE, "uploadFolder parameter not specified !");
                } else {
                    uploadFolderFile = new File(uploadFolder);
                    if (!uploadFolderFile.isDirectory() || !uploadFolderFile.canWrite()) {
                        logger.log(Level.SEVERE, "uploadFolder parameter is invalid: [{0}] ", uploadFolder);
                    }
                }
                if (uploadFolderFile == null) {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
                } else {
                    targetFile = new File(uploadFolderFile, projectid + ".obo");
                }
            }

            try {
                //FIXME check file validity before replacing/ registering existing
                java.nio.file.Files.copy(
                        entityParam,
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                if (!ontology.isPresent()) {
                    app.registerOntology(projectid, projectid, targetFile.toString());
                }
                return Response.status(Response.Status.CREATED).entity(projectid).build();

            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

        } else {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
    // -------------------------------------------------------------------------
    private static final Logger logger = Logger.getLogger(Resources.class.getName());

    private static final int UNPROCESSABLE = 422;

    @Context
    private JAXRSConfig app;

    private User getAuthUser(ContainerRequestContext requestContext) {
        return (User) requestContext.getProperty(Settings.AUTHUSER_PROPNAME);
    }

    private Response checkUserIsAdmin(
            ContainerRequestContext requestContext,
            String projectid,
            BiFunction<User, OboOntoHandler, Response> responseSupplier
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, true, responseSupplier);
    }

    private Response checkUserIsAuthForOnto(
            ContainerRequestContext requestContext,
            String projectid,
            BiFunction<User, OboOntoHandler, Response> responseSupplier
    ) {
        return checkUserIsAuthForOnto(requestContext, projectid, false, responseSupplier);
    }

    private Response checkUserIsAuthForOnto(
            ContainerRequestContext requestContext,
            String projectid, boolean ensureIsAdmin,
            BiFunction<User, OboOntoHandler, Response> responseSupplier
    ) {
        User authUser = getAuthUser(requestContext);

        if (ensureIsAdmin && !authUser.isIsAdmin()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } else {
            Optional<Ontology> ontology;
            if (authUser.isIsAdmin()) {
                //admin users can access any ontology registered in the config file
                ontology = app.getSettings().getOntology(projectid);
            } else {
                ontology = app.getSettings().getOntologyForUser(authUser, projectid);
            }

            if (!ontology.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            try (OboOntoHandler ontoHnd = OboOntoHandler.getHandler(ontology.get())) {
                return responseSupplier.apply(authUser, ontoHnd);
            }
        }
    }

    private Response checkSemClassExists(
            OboOntoHandler ontoHnd,
            String semclassid,
            Function<OWLClass, Response> responseSupplier
    ) {
        Optional<OWLClass> semClassOpt = ontoHnd.getSemanticClassesForId(semclassid).findFirst();

        if (semClassOpt.isPresent()) {
            return responseSupplier.apply(semClassOpt.get());
        } else {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).entity("Semantic class not found (" + semclassid + ")").build();
        }
    }

    private Response applyChangesAndSaveOnto(OboOntoHandler ontoHnd, List<OWLOntologyChange> changes, String semclassid) {
        if (ontoHnd.applyChangesAndSaveOnto(changes)) {
            JsonObjectBuilder result = getSemanticClassResult(ontoHnd, semclassid, true, false);
            return Response.ok(result.build()).build();

        } else {
            return Response.status(UNPROCESSABLE).entity("Could not perform modification").build();
        }
    }

    private JsonArrayBuilder getUserProjects(User authUser) {

        JsonArrayBuilder projects = Json.createArrayBuilder();
        if (authUser.isIsAdmin()) {
            //admin users can access any ontology registered in the config file
            app.getSettings().getOntologies()
                    .forEach(o -> projects.add(Json.createObjectBuilder()
                    .add("id", o.getId())
                    .add("name", o.getLongName())
            ));
        } else {
            app.getSettings().getOntologiesForUser(authUser)
                    .forEach(o -> projects.add(Json.createObjectBuilder()
                    .add("id", o.getId())
                    .add("name", o.getLongName())
            ));
        }
        return projects;
    }

    private JsonObjectBuilder getSemanticClassResult(OboOntoHandler ontoHnd, OWLClass semClass, boolean withHypoDetails, boolean withTerms) {
        return getSemanticClassResult(ontoHnd,
                getSemanticClassData(ontoHnd, semClass, withTerms),
                withHypoDetails, withTerms);
    }

    private Structs.DetailSemClassNTerms getSemanticClassData(OboOntoHandler ontoHnd, OWLClass semClass, boolean withTerms) {

        Structs.DetailSemClassNTerms classStruct;

        List<OWLClass> hypoClasses;

        if (semClass == null) {
            classStruct = new Structs.DetailSemClassNTerms();
            //virtual root class
            classStruct.groupId = OboOntoHandler.ROOT_ID;
            classStruct.canonicId = OboOntoHandler.ROOT_ID;
            classStruct.canonicLabel = "";
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

        hypoClasses.forEach(hypoClass -> classStruct.hypoClasses
                .add(getSemanticClassData(ontoHnd, hypoClass, false))
        );
        classStruct.hypoClasses.sort(
                (Structs.SemClass c1, Structs.SemClass c2) -> c1.canonicLabel.compareTo(c2.canonicLabel)
        );

        return classStruct;
    }

    private JsonObjectBuilder getSemanticClassResult(OboOntoHandler ontoHnd, Structs.DetailSemClassNTerms classStruct, boolean withHypoDetails, boolean withTerms) {
        JsonObjectBuilder semClassResult = Json.createObjectBuilder();

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
            classStruct.hypoClasses.forEach(hypo -> {
                hypoIds.add(hypo.canonicId);

                JsonObjectBuilder hypoResult = getSemanticClassResult(ontoHnd, hypo, false, false);
                hypoDetails.add(hypo.canonicId, hypoResult);
            });

            semClassResult.add("hypoGroupsDetails", hypoDetails);

        } else {
            classStruct.hypoClasses.forEach(hypo -> hypoIds.add(hypo.canonicId));
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
        return checkUserIsAuthForOnto(requestContext, projectid, (authUser, ontoHnd) -> {

            JsonObjectBuilder result = getSemanticClassResult(ontoHnd, semclassid, true, withTerms);
            return Response.ok(result.build()).build();
        });
    }

}
