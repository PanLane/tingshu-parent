package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.FileUploadService;
import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    MinioConstantProperties minioProperties;
    @Autowired
    MinioClient minioClient;

    @Override
    public String fileUpload(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        //获取桶名
        String bucket = minioProperties.getBucketName();
        //判断桶是否存在
        boolean is_exist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!is_exist) {
            //桶不存在，创建桶
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            //设置桶的访问权限
            String policy = """
                        {
                            "statement" : [{
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                            }],
                            "Version" : "2012-10-17"
                        }
                    """.formatted(bucket);
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        }

        //创建文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String objectName = new SimpleDateFormat("yyyyMMdd").format(new Date()) + "/" + "uuid" + "-" + uuid + "-" + file.getOriginalFilename();

        //上传
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());

        //路径拼接
        return String.join("/", minioProperties.getEndpointUrl(), bucket, objectName);
    }
}
