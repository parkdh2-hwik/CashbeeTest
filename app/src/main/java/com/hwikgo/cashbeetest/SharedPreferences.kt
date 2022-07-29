package com.hwikgo.cashbeetest

import android.app.Activity
import android.content.Context

class SharedPreferences(context: Context) {

    private val _sharedPrefs: android.content.SharedPreferences
    private val _prefsEditor: android.content.SharedPreferences.Editor

    companion object {
        private val APP_SHARED_PREFS = SharedPreferences::class.java.simpleName
    }

    init {
        _sharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE)
        _prefsEditor = _sharedPrefs.edit()
    }

    // FCM TOKEN
    var fcmId: String?
        get() = _sharedPrefs.getString("FCM_ID", "")
        set(token) {
            _prefsEditor.putString("FCM_ID", token)
            _prefsEditor.commit()
        }

    //통신사 코드
    var TelecomCode: String?
        get() = _sharedPrefs.getString("Telecom_Code", "")
        set(telecom_code) {
            _prefsEditor.putString("Telecom_Code", telecom_code)
            _prefsEditor.commit()
        }

    //가맹점 번호
    var LocalMchtNo: String?
        get() = _sharedPrefs.getString("LocalMchtNo", "")
        set(LocalMchtNo) {
            _prefsEditor.putString("LocalMchtNo", LocalMchtNo)
            _prefsEditor.commit()
        }

    //선후불타입
    var PayType: String?
        get() = _sharedPrefs.getString("PayType", "")
        set(PayType) {
            _prefsEditor.putString("PayType", PayType)
            _prefsEditor.commit()
        }
}