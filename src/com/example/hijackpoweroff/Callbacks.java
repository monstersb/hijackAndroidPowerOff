package com.example.hijackpoweroff;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Callbacks {

	static XC_MethodHook cb_wakeUpInternal = new XC_MethodHook() {

		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {
			Hooker.hooker.log("wakeUpInternal屏幕 " + param.args[0]);
			if (Hooker.hijacked == true) {
				param.setResult(null);
			}
		}
	};
	static XC_MethodHook cb_interceptKeyBeforeQueueing = new XC_MethodHook() {

		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {
			Hooker.hooker.log("按键" + param.args[0] + " | " + param.args[1]
					+ " | " + param.args[2]);
			if (Hooker.hijacked == true) {
				param.setResult(null);
			}
		}
	};

	static XC_MethodHook cb_init = new XC_MethodHook() {

		@Override
		protected void afterHookedMethod(MethodHookParam param)
				throws Throwable {
			Hooker.phoneWindowManager = param.thisObject;
			Hooker.hooker.log("PhoneWindow初始化!");
			Hooker.windowManagerFuncs = XposedHelpers.getObjectField(
					param.thisObject, "mWindowManagerFuncs");
			Hooker.context = (Context) param.args[0];
			XposedBridge.hookAllMethods(Hooker.windowManagerFuncs.getClass(),
					"shutdown", Callbacks.cb_shutdown);
		}
	};

	static XC_MethodHook cb_shutdown = new XC_MethodHook() {

		@Override
		protected void beforeHookedMethod(MethodHookParam param)
				throws Throwable {
			Hooker.hooker.log("关机！ " + param.args[0].toString());
			Hooker.hijacked = true;
			showShutdownDialog();
			param.setResult(null);
		}

		void showShutdownDialog() {

			ProgressDialog pd = new ProgressDialog(Hooker.context);
			pd.setTitle("关机");
			pd.setMessage("已劫持关机");
			pd.setIndeterminate(true);
			pd.setCancelable(false);
			pd.getWindow().setType(
					WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
			pd.show();
			Hooker.handler = new Handler();
			Hooker.handler.postDelayed(new myCancelShutdownDialog(pd), 5000);
		}
	};
}

class myCancelShutdownDialog implements Runnable {
	ProgressDialog pd;

	myCancelShutdownDialog(ProgressDialog pd) {
		this.pd = pd;
	}

	@Override
	public void run() {
		pd.setCancelable(true);
		pd.cancel();
		offVolume();
		goToSleep();
		listenCall();
	}

	void offVolume() {
		AudioManager mAudioManager = (AudioManager) Hooker.context
				.getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
		mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0);
	}

	void goToSleep() {
		PowerManager pm = (PowerManager) Hooker.context
				.getSystemService(Context.POWER_SERVICE);
		pm.goToSleep(SystemClock.uptimeMillis());
	}

	void listenCall() {
		BroadcastReceiverMgr mBroadcastReceiver = new BroadcastReceiverMgr();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		intentFilter.setPriority(Integer.MAX_VALUE);
		Hooker.context.registerReceiver(mBroadcastReceiver, intentFilter);
	}
}

class BroadcastReceiverMgr extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		if (arg1.getAction()
				.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
			TelephonyManager telephony = (TelephonyManager) Hooker.context
					.getSystemService(Context.TELEPHONY_SERVICE);
			switch (telephony.getCallState()) {
			case TelephonyManager.CALL_STATE_RINGING:
				Method getITelephonyMethod;
				try {
					getITelephonyMethod = telephony.getClass()
							.getDeclaredMethod("getITelephony");
					getITelephonyMethod.setAccessible(true);
					Object iTelephony = getITelephonyMethod.invoke(telephony);
					iTelephony.getClass().getMethod("answerRingingCall")
							.invoke(iTelephony);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Hooker.hooker.log("调用接听电话失败" + e);
					return;
				}
				break;
			}
		}
	}
}