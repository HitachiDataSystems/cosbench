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

import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AbstractAWSSigner;
import com.amazonaws.auth.SignerFactory;

public enum HcpS3AuthType {
	
    V2("S3SignerType", false),
    V4("AWSS3V4SignerType", false),
    ANON("HcpHs3AnonymousSigner", true),
    HCP("HcpHs3Signer", true),
    UNKNOWN("Uknown", true);
    
    public static final String SIGNER_V2 = "v2";
    public static final String SIGNER_V4 = "v4";
    public static final String SIGNER_ANON = "anon";
    public static final String SIGNER_HCP = "hcp";


    private final String signerType;
    private final boolean useHS3Endpoint;
    
    HcpS3AuthType(String signerType, boolean useHS3Endpoint) {
        this.signerType = signerType;
        this.useHS3Endpoint = useHS3Endpoint;
    }

    public String getSignerType() {
        return this.signerType;
    }
    
    public boolean useHS3Endpoint() {
    	return this.useHS3Endpoint;
    }
    
    
    static {
		SignerFactory.registerSigner(HCP.getSignerType(), HcpHs3Signer.class);
		SignerFactory.registerSigner(ANON.getSignerType(), HcpHs3AnonymousSigner.class);
	}
    
    
    public static HcpS3AuthType fromString(String val, HcpS3AuthType defaultVal) {
        if (val.equalsIgnoreCase(SIGNER_V2)) {
            return V2;
        }
        if (val.equalsIgnoreCase(SIGNER_V4)) {
            return V4;
        }
        if (val.equalsIgnoreCase(SIGNER_HCP)) {
        	return HCP;
        }
        if (val.equalsIgnoreCase(SIGNER_ANON)) {
        	return ANON;
        }
        return defaultVal;
    }
    
    

    public static HcpS3AuthType fromSignerOverride(String val) {
        if (val.equalsIgnoreCase(V2.getSignerType())) {
            return V2;
        }
        if (val.equalsIgnoreCase(V4.getSignerType())) {
            return V4;
        }
        if (val.equalsIgnoreCase(ANON.getSignerType())) {
            return ANON;
        }
        if (val.equalsIgnoreCase(HCP.getSignerType())) {
            return HCP;
        }
        return UNKNOWN;
    }
    
    
    
    // Custom Signers
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    public static class HcpHs3AnonymousSigner extends AbstractAWSSigner {
    	
    	private static final String HCP_ALL_USERS = "HCP all_users";
    	
		@Override
		public void sign(SignableRequest<?> request, AWSCredentials credentials) {
			request.addHeader(AUTHORIZATION_HEADER, HCP_ALL_USERS);
			
		}

		@Override
		protected void addSessionCredentials(SignableRequest<?> request,
				AWSSessionCredentials credentials) {
			throw new UnsupportedOperationException("addSessionCredentials is not implemented for HCP Signers");
			
		}
    	
    }
    
    public static class HcpHs3Signer extends AbstractAWSSigner {
    	
    	private static final String HCP_AUTH_FORMAT = "HCP %s:%s";
    	
    	@Override
    	public void sign(SignableRequest<?> request, AWSCredentials credentials) {
    		request.addHeader(AUTHORIZATION_HEADER, 
    				String.format(HCP_AUTH_FORMAT, 
    						credentials.getAWSAccessKeyId(), 
    						credentials.getAWSSecretKey()));
    	}
    	
    	@Override
    	public void addSessionCredentials(SignableRequest<?> request, 
    			AWSSessionCredentials credentials) {
    		throw new UnsupportedOperationException("addSessionCredentials is not implemented for HCP Signers");
    	}
    }

}