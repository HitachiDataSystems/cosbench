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
package com.intel.cosbench.driver.generator;

import java.io.IOException;

import org.apache.commons.io.input.NullInputStream;

/*
 * The NullInputStream class does not follow the InputStream contract for read methods which should only throw IOException
 * If the stream cannot be read. The NullInputStream also throws an IOException when the "EOF" is reached, SafeReadNullInputStream
 * will instead catch this exception and return -1. The need for this class is that the AWS SDK will sometimes attempt to read past
 * EOF expecting a -1 to be returned multiple times (we hit this along the v4 authentication write path). Since we are generating 
 * data in NullInputStream, an IOException will not be thrown unless NullInputStream explicitly throws it which happens during the EOF code path.
 */
public class SafeReadNullInputStream extends NullInputStream {

	public SafeReadNullInputStream(long size) {
		super(size);
	}
	
	@Override
	public int read() throws IOException {
		try {
			return super.read();
		} catch (IOException e) {
			return -1;
		}
	}
	
	@Override
	public int read(byte[] bytes) throws IOException {
		try {
			return super.read(bytes);
		} catch (IOException e) {
			return -1;
		}
	}
	
	@Override
	public int read(byte[] bytes, int offset, int length) throws IOException {
		try {
			return super.read(bytes, offset, length);
		} catch (IOException e) {
			return -1;
		}
	}
}