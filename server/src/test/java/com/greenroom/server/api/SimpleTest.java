package com.greenroom.server.api;

import com.greenroom.server.api.config.TestExecutionListener;
import com.greenroom.server.api.utils.S3ImageUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith({RestDocumentationExtension.class})
@TestExecutionListeners(value = TestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)

public class SimpleTest {

    @Autowired
    private S3ImageUploader s3ImageUploader;

    @Test
    public void test1(){
        List<String> test = new ArrayList<>();
        for(int i=0;i<2700;i++){
            test.add("q");
        }
        s3ImageUploader.deleteImageInBatch(test);
    }

    @Test
    public void test2(){
        List<String> imageUrlList = List.of(""
        );
        s3ImageUploader.deleteImageInBatch(imageUrlList);
    }

}
