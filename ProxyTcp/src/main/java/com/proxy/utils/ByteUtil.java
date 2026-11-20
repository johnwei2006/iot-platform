package com.proxy.utils;

public class ByteUtil {
	public static void main(String[] args) {
		int lat = 127;
		byte[] b = ByteUtil.short2Byte2Array(lat);
		System.out.println(HexConvert.toHexString(b));
		System.out.println((byte) lat);
		short v = ByteUtil.byte2Array2Short(b);
		System.out.println(v);
		// System.out.println(ByteUtil.byte8Array2Long(HexConvert.fromHexString("0731097a45000000")));
	}

	public static short byte2Array2Short(byte[] b) {
		return (short) (b[1] & 0xFF | (b[0] & 0xFF) << 8);
	}

	public static short byte2BigArray2Short(byte[] b) {
		return (short) (b[0] & 0xFF | (b[1] & 0xFF) << 8);
	}
	
	public static byte[] short2Byte2Array(int a) {
		return new byte[] { (byte) ((byte) (a >> 8) & 0xFF), (byte) (a & 0xFF) };
	}

	public static byte[] short2BigByte2Array(int a) {
		return new byte[] { (byte) (a & 0xFF),  (byte) ((byte) (a >> 8) & 0xFF) };
	}
	
	public static int byte4Array2Int(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

	public static int byte4BigArray2Int(byte[] b) {
		return b[0] & 0xFF | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
	}

	public static byte[] int2Byte4Array(int a) {
		return new byte[] { (byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF),
				(byte) (a & 0xFF) };
	}
	public static byte[] int2BigByte4Array(int a) {
		return new byte[] { (byte) ((a) & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) ((a >> 16) & 0xFF),
				(byte) (a >> 24 & 0xFF) };
	}
	public static long byte5Array2Long(byte[] b) {
		return ((long) b[0] << 32 & 0xFF00000000L) | ((long) b[1] << 24 & 0xFF000000L) | ((long) b[2] << 16 & 0xFF0000L)
				| ((long) b[3] << 8 & 0xFF00L) | ((long) b[4] & 0xFFL);
	}

	public static byte[] long2Byte5Array(long a) {
		byte[] b = new byte[5];
		b[0] = (byte) ((a >> 32) & 0xFF);
		b[1] = (byte) ((a >> 24) & 0xFF);
		b[2] = (byte) ((a >> 16) & 0xFF);
		b[3] = (byte) ((a >> 8) & 0xFF);
		b[4] = (byte) (a & 0xFF);
		return b;
	}

	public static long byte8Array2Long(byte[] b) {
		long value = 0;
		value = (((long) b[0] << 56 & 0xFF00000000000000L) | ((long) b[1] << 48 & 0xFF000000000000L)
				| ((long) b[2] << 40 & 0xFF0000000000L) | ((long) b[3] << 32 & 0xFF00000000L)
				| ((long) b[4] << 24 & 0xFF000000L) | ((long) b[5] << 16 & 0xFF0000L) | ((long) b[6] << 8 & 0xFF00L)
				| ((long) b[7] & 0xFFL));
		return value;
	}

	public static byte[] long2Byte8Array(long a) {
		byte[] c = new byte[8];
		c[0] = (byte) ((a >> 56) & 0xFF);
		c[1] = (byte) ((a >> 48) & 0xFF);
		c[2] = (byte) ((a >> 40) & 0xFF);
		c[3] = (byte) ((a >> 32) & 0xFF);
		c[4] = (byte) ((a >> 24) & 0xFF);
		c[5] = (byte) ((a >> 16) & 0xFF);
		c[6] = (byte) ((a >> 8) & 0xFF);
		c[7] = (byte) (a & 0xFF);
		return c;
	}
	public static int specificValuePositon(byte[] source, byte[] specificValue) {
		if(source == null || specificValue == null)
			return -1;
		if(specificValue.length > source.length)
			return -1;
		int matchedPosition = -1;
		for(int i = 0; i < source.length; i++) {
			if(source[i] == specificValue[0]) {
				for(int j = 1; j < specificValue.length; j++) {
					if(source[i + j] != specificValue[j]) {
						break;
					}
				}
				matchedPosition = i;
				break;
			}
		}
		return matchedPosition;
	}	
}
