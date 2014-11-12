package com.example.hijackpoweroff;

import android.content.Context;
import android.os.PowerManager;

public class Utils {

	public static String bytes2Hex(byte[] src) {
		if (src == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder("0x");
		for (int i = 0; i < src.length; i++) {
			sb.append(String.format("%02X", src[i] & 0xFF));
		}

		return sb.toString();
	}

	public static byte[] hex2bytes(String src) {
		byte[] dst = new byte[(src.length() - 2) / 2];
		char[] chs = src.substring(2).toCharArray();
		for (int i = 0, c = 0; i < chs.length; i += 2, c++) {
			dst[c] = (byte) (Integer.parseInt(new String(chs, i, 2), 16));
		}
		return dst;
	}

	static void stack() {
		StackTraceElement[] stackElements = new Throwable().getStackTrace();
		if (stackElements != null) {
			for (int i = 0; i < stackElements.length; i++) {
				Hooker.hooker.log("Call stack : " + stackElements[i]);
			}
		}
	}

	void test(Context context) {
	}
}
