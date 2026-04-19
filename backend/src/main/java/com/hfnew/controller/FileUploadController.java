package com.hfnew.controller;

import com.hfnew.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. 验证文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, "请选择要上传的文件"));
        }

        // 2. 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, "文件大小不能超过5MB"));
        }

        // 3. 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, "文件名无效"));
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, "仅支持 jpg, jpeg, png 格式的图片"));
        }

        try {
            // 4. 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 5. 生成唯一文件名
            String newFilename = UUID.randomUUID().toString() + "." + extension;
            Path targetPath = uploadPath.resolve(newFilename);

            // 6. 保存文件
            Files.copy(file.getInputStream(), targetPath);

            // 7. 返回访问URL
            String fileUrl = "/uploads/" + newFilename;
            return ResponseEntity.ok(ApiResponse.success(fileUrl));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(500, "文件上传失败: " + e.getMessage()));
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex + 1);
    }
}
