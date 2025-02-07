package com.greenroom.server.api.utils;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.greenroom.server.api.enums.ResponseCodeEnum;
import com.greenroom.server.api.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class S3ImageUploader {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.image.path.user}")
    private String userImageDir;

    private final AmazonS3 amazonS3;

    public String uploadUserProfileImage(MultipartFile multipartFile){
        return uploadImage(multipartFile,userImageDir);
    }

    public String uploadImage(MultipartFile multipartFile, String dir) {

        String contentType = multipartFile.getContentType();

        //이미지 파일 형식 검증 & 사이즈 검증(10mb 제한)
        if ( !contentType.startsWith("image/") || (float) multipartFile.getSize() /(1024.0*1024.0) > (float) 10) {
            throw new CustomException(ResponseCodeEnum.INVALID_IMAGE_FORMAT);
        }

        UUID uuId = UUID.randomUUID();
        String serverFileName = dir+"/"+ uuId+ "-" + multipartFile.getOriginalFilename();

        //image metadata 설정
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try{
            amazonS3.putObject(bucket, serverFileName, multipartFile.getInputStream(), metadata);
        }
        catch (IOException e){throw new CustomException(ResponseCodeEnum.FAIL_TO_UPLOAD_IMAGE);}

        return serverFileName;
    }


}
