package com.greenroom.server.api.domain.greenroom.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "plant")
@Getter
@Setter
@AllArgsConstructor
@Setting(settingPath = "es-setting.json")
public class PlantDocument {

    @Id
    private Long plantId;

    @Field(type = FieldType.Text, analyzer = "custom_analyzer", searchAnalyzer = "keyword")
    private String commonName;

    @Field(type = FieldType.Keyword, index = false,docValues = false)
    private String scientificName;

    @Field(type = FieldType.Keyword, index = false,docValues = false)
    private String plantPictureUrlS3;

}
