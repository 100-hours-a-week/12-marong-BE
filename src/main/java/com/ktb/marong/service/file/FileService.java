package com.ktb.marong.service.file;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ktb.marong.exception.CustomException;
import com.ktb.marong.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 이미지 파일 업로드
     */
    public String uploadImage(MultipartFile file, String dirName) {
        // 파일 검증
        validateFile(file);

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
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        // 업로드된 파일의 S3 URL 반환
        return amazonS3.getUrl(bucket, filePath).toString();
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/jpeg") && !contentType.startsWith("image/png"))) {
            throw new CustomException(ErrorCode.INVALID_FILE_FORMAT);
        }

        if (file.getSize() > 2 * 1024 * 1024) { // 2MB 제한
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
    }
}