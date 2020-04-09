package fr.inrae.bibliome.ontolrws;


import fr.inrae.bibliome.ontolrws.Settings.Utils;
import java.util.Arrays;


/**
 *
 * @author fpa
 */
public class CLIHandler {
    public static void main(String[] args) {
        //generate MD5 hash output for every arguments on th ecommand line 
        Arrays.asList(args).forEach(a -> System.out.println( Utils.hashPassword(a) + " <== " + a));
    }
}
