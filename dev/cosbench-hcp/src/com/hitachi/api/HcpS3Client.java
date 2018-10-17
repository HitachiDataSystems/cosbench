/** 
 
Copyright 2018 Hitachi Vantara, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
*/ 
package com.hitachi.api;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.intel.cosbench.api.storage.StorageException;
import com.intel.cosbench.log.Logger;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

public class HcpS3Client {

    public enum Protocol {
        HTTP("http", 80),
        HTTPS("https", 443);


        private final String protocol;
        private final int port;

        Protocol(String protocol, int port) {
            this.protocol = protocol;
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public int getPort() {
            return port;
        }

        public static Protocol fromString(String val) {
            String lowerCaseVal = val.toLowerCase();
            for (Protocol p : Protocol.values()) {
                if (p.getProtocol().equals(lowerCaseVal)) {
                    return p;
                }
            }
            throw new StorageException("Unknown protocol: " + val);
        }
    }

    private final String accessKey;
    private final String secretKey;
    private final S3ClientOptions clientOptions;
    private final ClientConfiguration config;
    private final Protocol protocol;;
    private final boolean optimizeDirStructure;
    private final Object nextClientLock = new Object();
    
    private final List<AmazonS3> s3Clients;
    
    private int nextClientIndex = 0;


    private HcpS3Client(String hostname, String accessKey, String secretKey,
                        S3ClientOptions clientOptions, ClientConfiguration config, 
                        Protocol protocol, Logger log, boolean optimizeDirStructure) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.clientOptions = clientOptions;
        this.config = config;
        this.protocol = protocol;
        this.optimizeDirStructure = optimizeDirStructure;
        try {        	
        	s3Clients = createEndpointConnections(hostname);
        } catch (Exception e) {
        	throw new StorageException(e);
        }

    }

    public static HcpS3Client create(String endpoint, String username, String password,
                                     ClientConfiguration config, Protocol protocol, Logger log, boolean optimizeDirStructure) {
    	String nameEncoded = DatatypeConverter.printBase64Binary(username.getBytes());
        String secretKeyHash = getMD5Hash(password);
        S3ClientOptions options = S3ClientOptions.builder().setPathStyleAccess(true).build();
        return new HcpS3Client(endpoint, nameEncoded, secretKeyHash, options, config, protocol, log, optimizeDirStructure);
    }

    public void putObject(String namespace, String path, InputStream content, long length) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(length);
        PutObjectRequest request = new PutObjectRequest(namespace, translatePath(path), content, metadata);
        getNextClient().putObject(request);
    }

    public InputStream getObject(String namespace, String path) {
        GetObjectRequest request = new GetObjectRequest(namespace, translatePath(path));
        S3Object object = getNextClient().getObject(request);
        return object.getObjectContent();
    }

    public void deleteObject(String namespace, String path) {
        DeleteObjectRequest request = new DeleteObjectRequest(namespace, translatePath(path));
        getNextClient().deleteObject(request);
    }

    public void createBucket(String namespace) {
        if (!getNextClient().doesBucketExist(namespace)) {
        	getNextClient().createBucket(namespace);
        }
    }

    public void deleteBucket(String namespace) {
        if (getNextClient().doesBucketExist(namespace)) {
            getNextClient().deleteBucket(namespace);
        }
    }
    
    private AmazonS3 getNextClient() {
    	AmazonS3 nextClient;
    	synchronized (nextClientLock) {
    	nextClient = s3Clients.get(nextClientIndex);
    	nextClientIndex = (nextClientIndex + 1) % s3Clients.size();
    	}
    	return nextClient;
    }



    private List<AmazonS3> createEndpointConnections(String host) throws UnknownHostException, IllegalArgumentException {
    	List<AmazonS3> clients = new ArrayList<AmazonS3>();
        String endpointString = protocol.getProtocol() + "://" + host + ":" + protocol.getPort();

        HcpS3AuthType authType = HcpS3AuthType.fromSignerOverride(config.getSignerOverride());
        
        // If we are using HCP native auth instead of S3 we must connect via the hs3 endpoint
        if (authType.useHS3Endpoint()) {
        	endpointString = endpointString + "/hs3";
        }
        
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        for (InetAddress address : InetAddress.getAllByName(host)) {
        	config.setDnsResolver(new SingleIPDnsResolver(address));
        	AmazonS3 s3 = new AmazonS3Client(credentials, config);
        	s3.setRegion(Region.getRegion(Regions.US_EAST_1));
        	s3.setEndpoint(endpointString);
        	s3.setS3ClientOptions(clientOptions);
        	clients.add(s3);
        }
        return clients;
    }

    
    // If we are optimizing the directory structure we prepend directories based on the hash of the object name 
    private String translatePath(String path) {
    	if (optimizeDirStructure) {
    		String hashedPath = getMD5Hash(path);
    		int hashedPathLength = hashedPath.length();
    		String newPathString = hashedPath.substring(0, 2) + "/" + 
    		hashedPath.substring(hashedPathLength - 2, hashedPathLength) + "/" + path;
    		return newPathString;
    	} else { 
    		return path;
    	}
    }


    private static final String MESSAGE_DIGEST_ALGORITHM = "MD5";
    private static final String MESSAGE_DIGEST_STRING_CHARSET = "UTF-8";

    private static String getMD5Hash(String str) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unable to construct MessageDigest because "
                    + MESSAGE_DIGEST_ALGORITHM + " is not a valid algorithm.");
        }

        byte[] md5PassHashArray;
        try {
            md5PassHashArray = messageDigest.digest(str.getBytes(MESSAGE_DIGEST_STRING_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unable to compute message digest because "
                    + MESSAGE_DIGEST_STRING_CHARSET + " is not a valid encoding.");
        }

        String secretKey;
        secretKey = (new HexBinaryAdapter()).marshal(md5PassHashArray);
        return secretKey.toLowerCase();
    }

}
