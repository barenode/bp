package org.barenode;

import junit.framework.TestCase;

public class FooTestCase extends TestCase {

	
	public void test() throws Exception {
		
		int a = 1;
		int b = a++;
		assertEquals(1, b);
	}
}
