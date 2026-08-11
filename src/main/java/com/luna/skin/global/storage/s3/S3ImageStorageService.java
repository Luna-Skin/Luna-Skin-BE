package com.luna.skin.global.storage.s3;


import com.luna.skin.global.exception.CommonErrorCode;
import com.luna.skin.global.exception.CustomException;
import com.luna.skin.global.storage.ImageStorageService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Primary  // LocalImageStorageService 대신 주입
@Service
@RequiredArgsConstructor
public class S3ImageStorageService implements ImageStorageService {

  private final S3Client s3Client;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucket;

  @Override
  public String store(MultipartFile file, String directory) {
    String fileName = directory + "/" + UUID.randomUUID() + extractExtension(file.getOriginalFilename());
    try {
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(fileName)
              .contentType(file.getContentType())
              .build(),
          RequestBody.fromBytes(file.getBytes())
      );
    } catch (IOException e) {
      throw new CustomException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + fileName;
  }

  private String extractExtension(String originalFilename) {
    if (originalFilename == null || !originalFilename.contains(".")) return "";
    return originalFilename.substring(originalFilename.lastIndexOf("."));
  }
}