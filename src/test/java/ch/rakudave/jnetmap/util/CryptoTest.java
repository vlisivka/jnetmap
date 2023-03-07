
/**
 * 
 * @author sebehuber
 *
 */

package ch.rakudave.jnetmap.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CryptoTest {

	String BadPassword = "securitypasswordwhichiswaytoolong";
	String password = "securit";
	String plainText ="Zeitung";
	String encryptedBase64 = "NLRAmdDoB7I=";

	
	@Test(expected=Exception.class)
	public void convertTest() throws Exception{
		Crypto.convert(BadPassword);	
	}
	
	@Test
	public void decryptTest(){
		assertEquals(Crypto.decrypt(encryptedBase64, password),plainText);
	}
	
	@Test
	public void encryptTest(){
		assertEquals(Crypto.encrypt(plainText, password),encryptedBase64);
	}
	
}
