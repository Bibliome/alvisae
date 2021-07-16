package fr.inrae.bibliome.ontolrws.Settings;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author fpa
 */
public class Utils {
        public static String hashPassword(String rawPassword) {
        //unsalted hashing with MD5
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            Logger.getLogger(Utils.class.getCanonicalName()).info("rawPassword.getBytes(\"UTF-8\") = " + Arrays.toString(rawPassword.getBytes("UTF-8")));
            byte[] digest = md.digest(rawPassword.getBytes("UTF-8"));
            hash = DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, "Unable to perform MD5 digest", ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hash;
    }

}
