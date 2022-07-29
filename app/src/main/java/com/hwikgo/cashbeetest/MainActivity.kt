package com.hwikgo.cashbeetest

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.ebcard.cashbee.cardservice.hce.CashbeeHceInterface
import com.ebcard.cashbee.cardservice.hce.ICashbeeApplication
import com.ebcard.cashbee.cardservice.hce.common.CashBeeListener
import com.ebcard.cashbee.cardservice.hce.common.OnHceServiceListener
import com.ebcard.cashbee.cardservice.hce.impl.CashbeeHce
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    lateinit var mCashbeeHCE: CashbeeHceInterface
    var mNetworkIO: Executor = Executors.newFixedThreadPool(3)
    lateinit var hceApplication: HceApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mCashbeeHCE = CashbeeHce()
        hceApplication = HceApplication()

        hceApplication.initialize(this, "1000103935", "02", 2, true)

    }

}