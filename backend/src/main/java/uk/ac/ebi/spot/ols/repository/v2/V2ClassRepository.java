
package uk.ac.ebi.spot.ols.repository.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.ols.model.v2.V2Class;
import uk.ac.ebi.spot.ols.repository.neo4j.OlsNeo4jClient;
import uk.ac.ebi.spot.ols.repository.solr.Fuzziness;
import uk.ac.ebi.spot.ols.repository.solr.OlsSolrQuery;
import uk.ac.ebi.spot.ols.repository.solr.OlsSolrClient;
import uk.ac.ebi.spot.ols.repository.Validation;
import uk.ac.ebi.spot.ols.repository.v2.helpers.V2DynamicFilterParser;
import uk.ac.ebi.spot.ols.repository.v2.helpers.V2SearchFieldsParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Component
public class V2ClassRepository {

    @Autowired
    OlsSolrClient solrClient;

    @Autowired
    OlsNeo4jClient neo4jClient;


    public Page<V2Class> find(
            Pageable pageable, String lang, String search, String searchFields, String boostFields, Map<String,String> properties) throws IOException {

        Validation.validateLang(lang);

        if(search != null && searchFields == null) {
            searchFields = "http://www.w3.org/2000/01/rdf-schema#label^100 definition";
        }

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "class", Fuzziness.EXACT);
        V2SearchFieldsParser.addSearchFieldsToQuery(query, searchFields);
        V2SearchFieldsParser.addBoostFieldsToQuery(query, boostFields);
        V2DynamicFilterParser.addDynamicFiltersToQuery(query, properties);
        query.setSearchText(search);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> new V2Class(result, lang));
    }

    public Page<V2Class> findByOntologyId(
            String ontologyId, Pageable pageable, String lang, String search, String searchFields, String boostFields, Map<String,String> properties) throws IOException {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        if(search != null && searchFields == null) {
            searchFields = "label^100 definition";
        }

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "class", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        V2SearchFieldsParser.addSearchFieldsToQuery(query, searchFields);
        V2SearchFieldsParser.addBoostFieldsToQuery(query, boostFields);
        V2DynamicFilterParser.addDynamicFiltersToQuery(query, properties);
        query.setSearchText(search);

        return solrClient.searchSolrPaginated(query, pageable)
                .map(result -> new V2Class(result, lang));
    }

    public V2Class getByOntologyIdAndIri(String ontologyId, String iri, String lang) throws ResourceNotFoundException {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        OlsSolrQuery query = new OlsSolrQuery();
        query.addFilter("lang", lang, Fuzziness.EXACT);
        query.addFilter("type", "class", Fuzziness.EXACT);
        query.addFilter("ontologyId", ontologyId, Fuzziness.EXACT);
        query.addFilter("iri", iri, Fuzziness.EXACT);

        return new V2Class(solrClient.getOne(query), lang);
    }

    public Page<V2Class> getChildrenByOntologyId(String ontologyId, Pageable pageable, String iri, String lang) {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        String id = ontologyId + "+class+" + iri;

        return this.neo4jClient.getChildren("OntologyClass", id, Arrays.asList("directParent"), pageable)
            .map(record -> new V2Class(record, lang));
    }

    public Page<V2Class> getAncestorsByOntologyId(String ontologyId, Pageable pageable, String iri, String lang) {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        String id = ontologyId + "+class+" + iri;

        return this.neo4jClient.getAncestors("OntologyClass", id, Arrays.asList("directParent"), pageable)
                .map(record -> new V2Class(record, lang));
    }

    public Page<V2Class> getIndividualAncestorsByOntologyId(String ontologyId, Pageable pageable, String iri, String lang) {

        Validation.validateOntologyId(ontologyId);
        Validation.validateLang(lang);

        String id = ontologyId + "+individual+" + iri;

        return this.neo4jClient.getAncestors("OntologyEntity", id, Arrays.asList("directParent"), pageable)
                .map(record -> new V2Class(record, lang));
    }
}
