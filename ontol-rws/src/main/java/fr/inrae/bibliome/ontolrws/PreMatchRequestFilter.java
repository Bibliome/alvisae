package fr.inrae.bibliome.ontolrws;

import fr.inrae.bibliome.ontolrws.Settings.Settings;
import fr.inrae.bibliome.ontolrws.Settings.User;
import fr.inrae.bibliome.ontolrws.Settings.Utils;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author fpa
 */
@PreMatching
@Provider
public class PreMatchRequestFilter implements ContainerRequestFilter {

    @Context
    private JAXRSConfig app;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
    	Logger logger = Logger.getLogger(getClass().getName());
        String authorization = requestContext.getHeaderString("Authorization");
        logger.info("authorization = " + authorization);

        if (authorization == null || authorization.isEmpty() || !authorization.startsWith("Basic ")) {

            //if no credential or some of another type is provided, need to challenge again the caller with Basic authentication scheme 
            requestContext.abortWith(Response.status(
                    Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"Access to OntoLR Web Services\"")
                    .build());

        } else {

            //check that provided credentials are well-formed and valid
            boolean authenticated = false;

            try {
                //base64 encoded creadential can be found after "Basic " prefix in header
                String encoded = authorization.substring(6);
                logger.info("encoded = " + encoded);
                String decoded = new String(Base64.getDecoder().decode(encoded));
                logger.info("decoded = " + decoded);
                String[] userPass = decoded.split(":");

                if (userPass.length == 2) {
                    logger.info("userPass[0] = " + userPass[0]);
                    logger.info("userPass[1] = " + userPass[1]);
                    String userName = userPass[0];
                    //retrieved user corresponding to provided name and password
                    logger.info("Utils.hashPassword(userPass[1]) = " + Utils.hashPassword(userPass[1]));
                    Optional<User> authUser = app.getSettings().getAuthenticatedUser(userName, Utils.hashPassword(userPass[1]));
                    logger.info("authUser = " + authUser);

                    if (authUser.isPresent()) {
                        authenticated = true;

                        //store authenticated user for later use during request processing
                        requestContext.setProperty(Settings.AUTHUSER_PROPNAME, authUser.get());
                    }
                }
            } catch (IllegalArgumentException e) {
            }

            //could not check that provided credentials are well-formed and valid
            if (!authenticated) {
                requestContext.abortWith(Response.status(
                        Response.Status.UNAUTHORIZED)
                        .build());
            }
        }
    }

}
