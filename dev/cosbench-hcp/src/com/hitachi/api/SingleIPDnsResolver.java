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

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.amazonaws.DnsResolver;


/*
 * This class is used to resolve to a single predefined IP address allowing us
 * to have a dedicated AWS SDK client for a specific node within the HCP cluster
 */
public class SingleIPDnsResolver implements DnsResolver {
	
	private final InetAddress[] ip;
	
	public SingleIPDnsResolver(InetAddress address) {
		ip = new InetAddress[1];
		ip[0] = address;
	}

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		return ip;
	}

}
