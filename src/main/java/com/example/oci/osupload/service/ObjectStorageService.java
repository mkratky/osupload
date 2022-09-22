package com.example.oci.osupload.service;

import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {
    public void init(String compartmentId, String bucketName);
    public void save(MultipartFile file);
    public void savea(MultipartFile file);
    public void savem(MultipartFile file);
}
