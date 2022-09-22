package com.example.oci.osupload;

import javax.annotation.Resource;

import com.example.oci.osupload.service.ObjectStorageService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OsuploadApplication implements CommandLineRunner {
	@Resource
	ObjectStorageService storageService;
	public static void main(String[] args) {
		SpringApplication.run(OsuploadApplication.class, args);
	}
	@Override
	public void run(String... args) throws Exception {
		final String compartmentId = args[0];
		final String bucketName = args[1];

		storageService.init(compartmentId, bucketName);
	}

}