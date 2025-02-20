package com.greenroom.server.api.domain.greenroom.repository;

import com.greenroom.server.api.domain.greenroom.document.PlantDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PlantDocumentRepository extends ElasticsearchRepository<PlantDocument,String> {

    @Query("{ \"match\": {\"commonName\" : \"?0\"}}")
    List<PlantDocument> findPlantDocumentByCommonName(String commonName);

}
