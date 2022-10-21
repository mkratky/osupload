# osupload
REST API service for uploading files to OCI Object Storage bucket implemented as Spring Boot application. 
Synchronous, Asynchronous and Multipart uploads using OCI Java SDK are demonstrated.

# Steps to build and run

## Step 1: Build and run the application
You can build and test the application in the OCI Cloud Shell (in that case start the Cloud Shell).
Otherwise on your machine
  #### Run following commands in the shell:
    java -version # JDK 11 is required. 
    git clone https://github.com/mkratky/osupload.git
    cd osupload
    mvn clean install
    java -jar target/osupload-0.0.1-SNAPSHOT.jar <your object storage bucket compartment ocid> <bucket name> # if the bucket name doesn't exist it will be created

#### When the application starts you should see output like this:
    Using namespace: <your tenancy namespace>
    Checking if bucket exists
    Using existing Bucket Name : <bucket name>

## Step 2: Upload file
  #### Run following commands in the shell:
    curl -F file=@"yourfilename" localhost:8080/upload # for synchronous upload
    curl -F file=@"yourfilename" localhost:8080/uploada # for asynchronous upload
    curl -F file=@"yourfilename" localhost:8080/uploadm # for multipart upload
