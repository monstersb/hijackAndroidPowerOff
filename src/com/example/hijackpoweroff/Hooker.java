package com.example.hijackpoweroff;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Hooker implements IXposedHookLoadPackage {

	Map<String, Object> map = new HashMap<String, Object>();
	Boolean hooked = false;
	static Hooker hooker;
	static Object windowManagerFuncs;
	static Context context;
	static Handler handler;
	static Object phoneWindowManager;
	static boolean hijacked = false;

	public void log(Object str) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		XposedBridge.log("关机劫持[" + df.format(new Date()) + "]:  "
				+ str.toString());
	}

	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		hooker = this;
		pretest(lpparam);
		if (!lpparam.processName.equals("android") || hooked) {
			return;
		}
		hooked = true;
		log("Package : " + lpparam.packageName);
		hook(lpparam);
	}

	void pretest(final LoadPackageParam lpparam) {
	}

	void hook(final LoadPackageParam lpparam) {
		try {
			XposedHelpers.findAndHookMethod(XposedHelpers.findClass(
					"com.android.internal.policy.impl.PhoneWindowManager",
					lpparam.classLoader), "interceptKeyBeforeQueueing",
					KeyEvent.class, int.class, boolean.class,
					Callbacks.cb_interceptKeyBeforeQueueing);
			XposedBridge.hookAllMethods(XposedHelpers.findClass(
					"com.android.internal.policy.impl.PhoneWindowManager",
					lpparam.classLoader), "init", Callbacks.cb_init);
			XposedBridge.hookAllMethods(XposedHelpers.findClass(
					"com.android.server.power.PowerManagerService",
					lpparam.classLoader), "wakeUpInternal",
					Callbacks.cb_wakeUpInternal);
		} catch (Exception e) {
			log(e.getLocalizedMessage());
		}
		log("挂钩成功");
	}

}