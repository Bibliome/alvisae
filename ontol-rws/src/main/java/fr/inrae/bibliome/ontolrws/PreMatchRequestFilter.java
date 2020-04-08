package fr.inrae.bibliome.ontolrws;

import fr.inrae.bibliome.ontolrws.Settings.Settings;
import fr.inrae.bibliome.ontolrws.Settings.User;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.DatatypeConverter;

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

        String authorization = requestContext.getHeaderString("Authorization");

        if (authorization == null || authorization.isEmpty() || !authorization.startsWith("Basic ")) {

            //if no credential or some of another type is provided, eed to challenge again the  caller with Basic authentication scheme 
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
                String decoded = new String(Base64.getDecoder().decode(encoded));
                String[] userPass = decoded.split(":");

                if (userPass.length == 2) {
                    String userName = userPass[0];
                    //unsalted hashing with MD5
                    String hashdPass = null;
                    try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] digest = md.digest(userPass[1].getBytes("UTF-8"));
                        hashdPass = DatatypeConverter.printHexBinary(digest).toUpperCase();
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(PreMatchRequestFilter.class.getName()).log(Level.SEVERE, "Unable to perform MD5 digest", ex);
                    }

                    //retrieved user corresponding to provided name and password
                    Optional<User> authUser = app.getSettings().getAuthenticatedUser(userName, hashdPass);

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
