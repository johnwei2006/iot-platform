package com.proxy.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    public static byte[] hashKeyWithSHA256(byte[] key) throws NoSuchAlgorithmException {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(key);
        byte[] newKey = new byte[key.length];
        
        if(hash.length >= newKey.length) {
        	System.arraycopy(hash, hash.length - newKey.length, newKey, 0, newKey.length);
        }
        else {
        	System.arraycopy(hash, 0, newKey, 0, hash.length);
        }
        
        return newKey;
    }	
}
