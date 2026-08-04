package com.luna.skin.global.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    String store(MultipartFile file, String directory);
}
