package com.proxy.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {
	/**
	 * 默认的密码字符串组合，用来将字节转换成 16 进制表示的字符,apache校验下载的文件的正确性用的就是默认的这个组合
	 */
	protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e','f' };
	protected static MessageDigest messagedigest = null;
	static {
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsaex) {
			System.err.println(MD5Util.class.getName() + "初始化失败，MessageDigest不支持MD5Util。");
			nsaex.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		long begin = System.currentTimeMillis();

		//String str = "00 02 24 10 4E 4A 34 47 4B 42 4F 41 56 30 30 30 30 30 32 37 15 02 1A 10 35 00 00 00 00 00 05 06 EC DF 00 FA 01 00 02 00 00 00 00 00 0F 98 82 08 6B CF BB CE B2 4B 49 19 B1 21 E3 B2 09 32 DC 41 01 30 2E 33 2E 31 36 34 39 31 61 66 65 66 34 64 36 61 34 39 66 34 38 34 34 64 32 37 36 37 61 62 31 64 37 33 32 64 2D 31 2D 30 2E 33 2E 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 05 12 02 2F 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 38 33 65 34 64 33 61 65 63 65 61 34 64 64 37 31 34 39 34 64 36 34 39 33 34 36 64 63 38 31 66 37 15 02 1A 10 35 00 66 74 70 2E 62 63 6E 79 79 75 6E 2E 63 6F 6D 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 04 1A 41 67 55 73 65 72 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 45 23 57 73 63 64 78 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 41 67 46 61 72 6D 47 61 74 65 77 61 79 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 33 09 7B CC 77 C8 88 9D 59 21 B7 5B 5F B6 75 E1 00 02 24 10 4E 4A 34 47 4B 42 4F 41 56 30 30 30 30 30 32 37 15 02 1A 10 35 00 00 00 00 00 05 06 EC DF 00 FA 01 00 02 00 00 00 00 00 0F 98 82 08 6B CF BB CE B2 4B 49 19 B1 21 E3 B2 09 32 DC 41 01 30 2E 33 2E 31 36 34 39 31 61 66 65 66 34 64".replaceAll(" ", "");
		String str = "000224104e4a34474b424f4156303030303032393232303631320000000051480688000b010001010000000000f3229801312e302e30".replaceAll(" ", "");
		
		byte[] resultSignature = MD5Util.getMD5(HexConvert.fromHexString(str));
		//byte[] resultSignature = MD5Util.getMD5(HexConvert.fromHexString("000224104e4a34474b424f41563030303030323715021a0e2d0000000000029057dd00fa01000200000000000f955588b3ccc9485a6f40e4a290520454bb599401302e332e3036343931616665663464366134396634383434643237363761623164373332642d312d302e332e300000000000000000000000000000000512022f00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000653832356231643738646436666137366565396336386432616632333662343115021a0e2d006674702e62636e7979756e2e636f6d0000000000000000000000000000000000000000000000000000000000000000000000041a4167557365720000000000000000000000000000000000000000000000000000000000000000000000000000000000000000404523577363647800000000000000000000000000000000000000000000000000000000000000000000000000000000000041674661726d47617465776179000000000000000000000000000000000001"));
		
		String md5 = MD5Util.bufferToHex(resultSignature);
		System.out.println("str:" + str);
		System.out.println("len:" + str.length());
		System.out.println("md5:" + md5);
//		File big = new File("E:/xxxx.rar");

//		String md5 = getFileMD5String(big);
		//String md5 = getMD5String("TESTDATASDfasdfasfd");

//		long end = System.currentTimeMillis();
//		System.out.println("md5:" + md5 + " time:" + ((end - begin) / 1000) + "s");
//		String md52 = getMD5String("TESTDATASDfasdfasfd");
//		System.out.println("md5-1:" + md5);
//		System.out.println("md5-2:" + md52);
	}

	// 生成文件的md5码
	public static String getFileMD5String(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		FileChannel ch = in.getChannel();
		MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		messagedigest.update(byteBuffer);
		return bufferToHex(messagedigest.digest());
	}

	// 生成字符串的md5码
	public static String getMD5String(String s) {
		return getMD5String(s.getBytes());
	}

	public static String getMD5String(byte[] bytes) {
		return bufferToHex(getMD5(bytes));
	}

	public static synchronized byte[] getMD5(byte[] bytes) {
		messagedigest.update(bytes);
		return messagedigest.digest();
	}

	public static String bufferToHex(byte bytes[]) {
		return bufferToHex(bytes, 0, bytes.length);
	}

	private static String bufferToHex(byte bytes[], int m, int n) {
		StringBuffer stringbuffer = new StringBuffer(2 * n);
		int k = m + n;
		for (int l = m; l < k; l++) {
			appendHexPair(bytes[l], stringbuffer);
		}
		return stringbuffer.toString();
	}

	private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
		char c0 = hexDigits[(bt & 0xf0) >> 4];
		char c1 = hexDigits[bt & 0xf];
		stringbuffer.append(c0);
		stringbuffer.append(c1);
	}

	public static boolean checkPassword(String password, String md5PwdStr) {
		String s = getMD5String(password);
		return s.equals(md5PwdStr);
	}
	
	public static byte calculate_checksum(byte[] data)
	{
	    byte checksum = 0;
	    for (int i = 0; i < data.length; i++)
	    {
	        checksum ^= data[i];
	    }
	    return checksum;
	}
}