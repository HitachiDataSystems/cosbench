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
