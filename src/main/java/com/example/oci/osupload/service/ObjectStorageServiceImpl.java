package com.example.oci.osupload.service;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.CreateBucketDetails;
import com.oracle.bmc.objectstorage.requests.*;
import com.oracle.bmc.objectstorage.responses.*;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.responses.AsyncHandler;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.http.ResteasyClientConfigurator;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.ClientBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

@Service
public class ObjectStorageServiceImpl implements ObjectStorageService {
    private static final String CONFIG_LOCATION = "~/.oci/config";
    private static final String CONFIG_PROFILE = "DEFAULT";
    private AuthenticationDetailsProvider provider;
    private ObjectStorage objectStorageClient;
    private ObjectStorageAsyncClient objectStorageAsyncClient;
    private String namespace;
    private String bucketName;

    @Override
    public void init(String compartmentId, String pBucketName) {
        bucketName = pBucketName;
        try {
            provider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());

            System.setProperty(ClientBuilder.JAXRS_DEFAULT_CLIENT_BUILDER_PROPERTY,"org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder");

            objectStorageClient = ObjectStorageClient.builder()
                    .additionalClientConfigurator(new ResteasyClientConfigurator())
                    .build(provider);

            objectStorageAsyncClient =  ObjectStorageAsyncClient.builder()
                    .additionalClientConfigurator(new ResteasyClientConfigurator())
                    .build(provider);

            namespace = objectStorageClient
                    .getNamespace(GetNamespaceRequest.builder().build())
                    .getValue();
            System.out.println("Using namespace: " + namespace);

        }

        catch(Exception exception)
        {
            System.out.println(new StringBuilder().append("Could not initialize the ObjectStorage service ").append(exception.getMessage()).toString());
        }

        try {
            GetBucketRequest request =
                    GetBucketRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucketName)
                            .build();

            System.out.println("Checking if bucket exists");
            GetBucketResponse response = objectStorageClient.getBucket(request);
        }catch (BmcException bmcException) {
            if (bmcException.getStatusCode() == 404) {
                CreateBucketRequest createBucketRequest =
                        CreateBucketRequest.builder()
                                .namespaceName(namespace)
                                .createBucketDetails(
                                        CreateBucketDetails.builder()
                                                .compartmentId(compartmentId)
                                                .name(bucketName)
                                                .build())
                                .build();
                CreateBucketResponse createBucketResponse = objectStorageClient.createBucket(createBucketRequest);
                System.out.println("New bucket location: " + createBucketResponse.getLocation());
            } else {
                System.out.println("Can't find the existing Bucket Name : " + bucketName);
                throw bmcException;
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize bucket for upload!" + e.getMessage());
        }
        System.out.println("Using existing Bucket Name : " + bucketName);
    }

    @Override
    public void save(MultipartFile file) {
        System.out.println("Beginning the file upload to OS via sync client: " + bucketName + "/" + file.getOriginalFilename());
        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucketName)
                            .objectName(file.getOriginalFilename())
                            .putObjectBody(file.getInputStream())
                            .contentLength(file.getSize())
                            .contentEncoding("application/octet-stream")
                            .build();

            PutObjectResponse putObjectResponse = objectStorageClient.putObject(putObjectRequest);
            System.out.println("New object md5: " + putObjectResponse.getOpcContentMd5());

        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public void savea(MultipartFile file) {
        System.out.println("Beginning the file upload to OS via async client: " + bucketName + "/" + file.getOriginalFilename());
        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucketName)
                            .objectName(file.getOriginalFilename())
                            .putObjectBody(file.getInputStream())
                            .contentLength(file.getSize())
                            .contentEncoding("application/octet-stream")
                            .build();

            ResponseHandler<PutObjectRequest, PutObjectResponse> putObjectHandler =
                    new ResponseHandler<>();
            Future<PutObjectResponse> putObjectResponseFuture = objectStorageAsyncClient.putObject(putObjectRequest, putObjectHandler);

        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
    @Override
    public void savem(MultipartFile file) {
        System.out.println("Beginning the file multi-part upload to OS via UploadManager : " + bucketName + "/" + file.getOriginalFilename());
        try {
            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .namespaceName(namespace)
                            .bucketName(bucketName)
                            .objectName(file.getOriginalFilename())
                            .putObjectBody(file.getInputStream())
                            .contentLength(file.getSize())
                            .contentEncoding("application/octet-stream")
                            .build();

            UploadConfiguration uploadConfiguration =
                    UploadConfiguration.builder()
                            .allowMultipartUploads(true)
                            .allowParallelUploads(true)
                            .lengthPerUploadPart(100)
                            .build();

            UploadManager uploadManager = new UploadManager(objectStorageClient, uploadConfiguration);
            UploadManager.UploadRequest uploadDetails =
                    UploadManager.UploadRequest.builder(file.getInputStream(),file.getSize()).allowOverwrite(true).build(putObjectRequest);
            // if multi-part is used, and any part fails, the entire upload fails and will throw BmcException
            UploadManager.UploadResponse response = uploadManager.upload(uploadDetails);
            System.out.println(response);

        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }
    static class ResponseHandler<IN, OUT> implements AsyncHandler<IN, OUT> {
        private Throwable failed = null;
        private CountDownLatch latch = new CountDownLatch(1);

        private void waitForCompletion() throws Exception {
            latch.await();
            if (failed != null) {
                if (failed instanceof Exception) {
                    throw (Exception) failed;
                }
                throw (Error) failed;
            }
        }

        @Override
        public void onSuccess(IN request, OUT response) {
            if (response instanceof GetNamespaceResponse) {
                System.out.println(
                        "Using namespace: " + ((GetNamespaceResponse) response).getValue());
            } else if (response instanceof CreateBucketResponse) {
                System.out.println(
                        "New bucket location: " + ((CreateBucketResponse) response).getLocation());
            } else if (response instanceof PutObjectResponse) {
                System.out.println(
                        "New object md5: " + ((PutObjectResponse) response).getOpcContentMd5());
            } else if (response instanceof GetObjectResponse) {
                System.out.println("Object md5: " + ((GetObjectResponse) response).getContentMd5());
            }
            latch.countDown();
        }

        @Override
        public void onError(IN request, Throwable error) {
            failed = error;
            latch.countDown();
        }
    }

}
