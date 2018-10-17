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
import com.amazonaws.SDKGlobalConfiguration;
import com.intel.cosbench.api.storage.NoneStorage;
import com.intel.cosbench.api.storage.StorageException;
import com.intel.cosbench.config.Config;
import com.intel.cosbench.log.Logger;

import java.io.InputStream;

public class HcpS3Storage extends NoneStorage {

    private static final String ENDPOINT_KEY = "endpoint";
    private static final String ENDPOINT_DEFAULT = null;
    private static final String USERNAME_KEY = "username";
    private static final String USERNAME_DEFAULT = null;
    private static final String PASSWORD_KEY = "password";
    private static final String PASSWORD_DEFAULT = null;
    private static final String PROTOCOL_KEY = "protocol";
    private static final String PROTOCOL_DEFAULT = "https";
    private static final String MAX_ERROR_RETRY_KEY = "max_error_retry";
    private static final String MAX_ERROR_RETRY_DEFAULT = "0";
    private static final String MAX_CONNECTIONS_KEY = "max_connections";
    private static final String MAX_CONNECTIONS_DEFAULT = "100";
    private static final String CONNECTION_TIMEOUT_KEY = "connection_timeout";
    private static final String CONNECTION_TIMEOUT_DEFAULT = "600000"; //10 minutes in milliseconds
    private static final String SOCKET_TIMEOUT_KEY = "socket_timeout";
    private static final String SOCKET_TIMEOUT_DEFAULT = "300000"; //5 minutes in milliseconds
    private static final String SIGNER_VERSION_KEY = "signer_version";
    private static final HcpS3AuthType DEFAULT_AUTH_TYPE = HcpS3AuthType.V4;
    private static final String SIGNER_VERSION_DEFAULT = "hcp";
    private static final String OPTIMIZE_DIR_KEY = "optimize_dir";
    private static final String OPTIMIZE_DIR_DEFAULT = "true";
    private static final String DISABLE_CERT_CHECK_KEY = "disable_cert_check";
    private static final String DISABLE_CERT_CHECK_DEFAULT = "true";
    private static final String USE_EXPECT_CONTINUE_KEY = "use_expect_continue";
    private static final String USE_EXPECT_CONTINUE_DEFAULT = "true";


    private HcpS3Client client;

    @Override
    public void init(Config config, Logger logger) {
        super.init(config, logger);
        String hostname = config.get(ENDPOINT_KEY, ENDPOINT_DEFAULT);
        String username = config.get(USERNAME_KEY, USERNAME_DEFAULT);
        String password = config.get(PASSWORD_KEY, PASSWORD_DEFAULT);
        if (hostname == null) {
        	throw new StorageException("hostname key must be specified in config");
        }
        String signerOverrideStr = config.get(SIGNER_VERSION_KEY, SIGNER_VERSION_DEFAULT);
        HcpS3AuthType authType = HcpS3AuthType.fromString(signerOverrideStr, DEFAULT_AUTH_TYPE);
        logger.info("authType: " + authType);
        if (authType != HcpS3AuthType.ANON) {        	
        	if (username == null) {
        		throw new StorageException("username key must be specified in config");
        	}
        	if (password == null ) {
        		throw new StorageException("password key must be specified in config");
        	}
        }
        String protocolStr = config.get(PROTOCOL_KEY, PROTOCOL_DEFAULT);
        HcpS3Client.Protocol protocol = HcpS3Client.Protocol.fromString(protocolStr);
        logger.info("Protocol: " + protocol);
        String maxErrorRetryStr = config.get(MAX_ERROR_RETRY_KEY, MAX_ERROR_RETRY_DEFAULT);
        int maxErrorRetry = Integer.valueOf(maxErrorRetryStr);
        logger.info("max_error_retry: " + maxErrorRetry);
        String maxConnectionStr = config.get(MAX_CONNECTIONS_KEY, MAX_CONNECTIONS_DEFAULT);
        int maxConnections = Integer.valueOf(maxConnectionStr);
        logger.info("max_connections: " + maxConnections);
        String connectionTimeoutStr = config.get(CONNECTION_TIMEOUT_KEY, CONNECTION_TIMEOUT_DEFAULT);
        int connectionTimeout = Integer.valueOf(connectionTimeoutStr);
        logger.info("connection_timeout: " + connectionTimeout);
        String socketTimeoutStr = config.get(SOCKET_TIMEOUT_KEY, SOCKET_TIMEOUT_DEFAULT);
        int socketTimeout = Integer.valueOf(socketTimeoutStr);
        logger.info("socket_timeout: " + socketTimeout);
        String optimizeDirStr = config.get(OPTIMIZE_DIR_KEY, OPTIMIZE_DIR_DEFAULT);
        boolean optimizeDir = Boolean.parseBoolean(optimizeDirStr);
        logger.info("optimizeDir: " + optimizeDirStr);
        String disableCertStr = config.get(DISABLE_CERT_CHECK_KEY, DISABLE_CERT_CHECK_DEFAULT);
        boolean disableCert = Boolean.parseBoolean(disableCertStr);
        logger.info("disable_cert_check: " + disableCert);
        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, Boolean.toString(disableCert));
        String useExpectContinueString = config.get(USE_EXPECT_CONTINUE_KEY, USE_EXPECT_CONTINUE_DEFAULT);
        boolean useExpectContinue = Boolean.parseBoolean(useExpectContinueString);
        logger.info("use_expect_continue: " + useExpectContinue);
        
        
        ClientConfiguration clientConfig = new ClientConfiguration().withMaxErrorRetry(maxErrorRetry)
                .withMaxConnections(maxConnections)
                .withConnectionTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout)
                .withSignerOverride(authType.getSignerType())
                .withUseExpectContinue(useExpectContinue);
        

        client = HcpS3Client.create(hostname, username, password,
                clientConfig, protocol, logger, optimizeDir);
    }

    @Override
    public void dispose() {
        client = null;
    }


    @Override
    public void deleteObject(String container, String object, Config config) {
    	super.deleteObject(container, object, config);
    	try {
    		client.deleteObject(container, object);
    	} catch (Exception e) {
    		throw new StorageException(e);
    	}
        
    }

    @Override
    public void deleteContainer(String container, Config config) {
    	super.deleteContainer(container, config);
    	try {
    		client.deleteBucket(container);
    	} catch (Exception e) {
    		throw new  StorageException(e);
    	}
        
    }

    @Override
    public void createObject(String container, String object, InputStream data, long length, Config config) {
    	super.createObject(container, object, data, length, config);
    	try {    		
    		client.putObject(container, object, data, length);
    	} catch (Exception e) {
    		throw new StorageException(e);
    	}
    }

    @Override
    public void createContainer(String container, Config config) {
    	super.createContainer(container, config);
        try {
        	client.createBucket(container);
        } catch (Exception e) {
        	throw new StorageException(e);
        }

    }


    @Override
    public InputStream getObject(String container, String object, Config config) {
    	super.getObject(container, object, config);
        try {
        	return client.getObject(container, object);        	
        } catch (Exception e) {
        	throw new StorageException(e); 
        }
    }
}
