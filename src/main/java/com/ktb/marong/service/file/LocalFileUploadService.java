package com.ktb.marong.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@Profile("local")
public class LocalFileUploadService implements FileUploadService {

    @Value("${file.upload.directory}")
    private String uploadDir;

    @Value("${file.upload.url.prefix}")
    private String urlPrefix;

    @Override
    public String uploadFile(MultipartFile file, String dirName) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 원본 파일명에서 확장자 추출
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);

        // 파일명 중복 방지를 위한 UUID 생성
        String fileName = UUID.randomUUID().toString() + "." + extension;

        // 디렉토리 경로 생성 (절대 경로 사용)
        // 현재 경로에 uploadDir과 dirName을 붙여서 절대 경로 생성
        String directory = System.getProperty("user.dir") + File.separator + uploadDir + dirName;
        Path directoryPath = Paths.get(directory);

        // 디렉토리가 존재하지 않으면 생성
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // 파일 저장 경로 설정
        String filePath = directory + File.separator + fileName;
        Path targetPath = Paths.get(filePath);

        // 파일 저장
        file.transferTo(targetPath.toFile());

        log.info("파일 저장 완료: {}", filePath);

        // 접근 가능한 URL 경로 반환 (웹에서 접근할 수 있는 경로)
        return urlPrefix + dirName + "/" + fileName;
    }
}