package uk.ac.ebi.spot.ols.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Component
public class OboDatabaseUrlService {

    @Value("${ols.obo_xrefs_url:}")
    private String xrefUrls;

    Map<String, OboDatabase> databases = new HashMap<>();

    public OboDatabaseUrlService() {

        if(xrefUrls == null) {
            xrefUrls = "https://raw.githubusercontent.com/geneontology/go-site/master/metadata/db-xrefs.yaml";
        }

        for(String xrefUrl : xrefUrls.split(";")) {

            InputStream inputStream;

            try {
                if (xrefUrl.contains("://")) {
                    inputStream = new URL(xrefUrl).openStream();
                } else {
                    inputStream = new FileInputStream(xrefUrl);
                }
            } catch(IOException e) {
                throw new RuntimeException("Error loading xref url: " + xrefUrl);
            }

            Yaml yaml = new Yaml();

            List<Object> xrefMap = (List<Object>) yaml.load(inputStream);
            for (Object entry : xrefMap) {

                Map<String,Object> entryMap = (Map<String,Object>) entry;
                String databaseId = (String) entryMap.get("database");
                String databaseName = (String) entryMap.get("name");
                if (entryMap.containsKey("entity_types")) {
                    List<Object> entityO = (List<Object>) entryMap.get("entity_types");

                    for (Object type : entityO ) {
                        Map<String,Object> entityMap = (Map<String,Object>) type;
                        if (entityMap.containsKey("url_syntax")) {
                            OboDatabase db = new OboDatabase();
                            db.databaseId = databaseId;
                            db.databaseName = databaseName;
                            db.urlSyntax = (String) entityMap.get("url_syntax");
                            databases.put(databaseId, db);
                        }
                    }
                }
            }
        }
    }

    public String getUrlForId(String databaseId, String id) {

        OboDatabase db = databases.get(databaseId);

        if(db == null)
            return null;

        return db.getUrlForId(id);
    }



    private class OboDatabase {

        public String databaseId;
        public String databaseName;
        public String urlSyntax;

        public String getUrlForId(String id) {
            return urlSyntax.replace("[example_id]", id);
        }
    }

}