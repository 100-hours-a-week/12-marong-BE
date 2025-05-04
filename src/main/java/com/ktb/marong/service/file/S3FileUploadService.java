package com.ktb.marong.service.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class S3FileUploadService implements FileUploadService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadFile(MultipartFile file, String dirName) throws IOException {
        // 파일 검증
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 원본 파일명
        String originalFileName = file.getOriginalFilename();
        // 파일 확장자 추출
        String ext = originalFileName.substring(originalFileName.lastIndexOf("."));
        // 저장할 파일명 (UUID)
        String savedFileName = UUID.randomUUID().toString() + ext;
        // 저장 경로
        String filePath = dirName + "/" + savedFileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            // S3에 업로드
            amazonS3.putObject(
                    new PutObjectRequest(bucket, filePath, inputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead)
            );
        }

        // 업로드된 파일의 S3 URL 반환
        return amazonS3.getUrl(bucket, filePath).toString();
    }
}