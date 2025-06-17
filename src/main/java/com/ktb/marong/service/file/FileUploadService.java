package com.ktb.marong.service.file;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileUploadService {
    String uploadFile(MultipartFile file, String dirName) throws IOException;
}