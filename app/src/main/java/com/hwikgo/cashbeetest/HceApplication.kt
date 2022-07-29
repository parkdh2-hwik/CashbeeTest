package com.hwikgo.cashbeetest

import android.app.Application
import android.content.Context
import android.util.Log
import com.ebcard.cashbee.cardservice.hce.CashbeeHceInterface
import com.ebcard.cashbee.cardservice.hce.ICashbeeApplication
import com.ebcard.cashbee.cardservice.hce.common.CashBeeListener
import com.ebcard.cashbee.cardservice.hce.common.CashbeeResultCode
import com.ebcard.cashbee.cardservice.hce.common.OnHceServiceListener
import com.ebcard.cashbee.cardservice.hce.impl.CashbeeHce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class HceApplication : Application(), ICashbeeApplication, CashBeeListener,
    OnHceServiceListener {

    lateinit var mInstance: HceApplication
    lateinit var mCashbeeHCE: CashbeeHceInterface
    var mNetworkIO: Executor = Executors.newFixedThreadPool(3)
    var affiliatesKey = "1000103935"
    var payMethod = "02"
    var serverType = 2 //0 = 운영서버, 1 = 검증서버, 2 = 개발서버
    var showLog = true // true = 로그 출력, false = 로그 출력안함
    lateinit var context: Context
    lateinit var sharedPreferences: SharedPreferences
    var mbrId = "25"


    override fun onCreate() {
        super.onCreate()

        sharedPreferences = SharedPreferences(this)
        mInstance = this

        context = applicationContext

//        initialize()

//        Log.e("TEST","서버 상태 조회 : " + mCashbeeHCE.requestServerStatus())
    }

    /**
     * @param context
     * @param affiliatesKey [가맹점 코드]
     * @param payMethod [결제수단 코드]
     * @param serverType [0 = 운영, 1 = 검증, 2 = 개발]
     * @param showLog [로그 활성화 여부]
     */
    override fun initialize() {
        mCashbeeHCE = CashbeeHce()
        mCashbeeHCE.setOnCashbeeListener(this)
        mCashbeeHCE.setOnHceServiceListener(this)


        mCashbeeHCE.initialize(context, affiliatesKey, payMethod, serverType, true)

    }

    fun initialize(
        context: Context,
        affiliatesKey: String,
        payMethod: String,
        serverType: Int,
        isLogView: Boolean
    ) {
        this.affiliatesKey = affiliatesKey
        this.payMethod = payMethod
        this.serverType = serverType
        this.showLog = isLogView
        this.context = context

        initialize()
    }

    override fun onNotify(p0: String?, p1: Int, p2: Int) {
        //p0 = 상태메시지 (ex: 충전, 지불..), p1 = 잔액, p2 = 거래금액
        Log.e("TEST", "상태메시지 : " + p0)
    }

    //서비스 접속 종료
    override fun onTerminate() {
        super.onTerminate()
        if (mCashbeeHCE != null) {
            mCashbeeHCE.terminate()
        }
    }

    override fun getCashbeeManager(): CashbeeHceInterface {
        return mCashbeeHCE
    }

    override fun getNFCManager(): CashbeeHceInterface? {
        return null
    }

    override fun getNetworkIO(): Executor {
        return mNetworkIO
    }

    /**
     * @param requestCode API 요청코드
     * @param responseCode API 상태코드
     *  - S_SUCCESS : 데이터값(json or null)
     *  - S_FAIL : ETC_CODE(캐시비 에러 코드)
     * @param responseMessage API 결과 데이터(json or null)
     */
    override fun onResult(requestCode: Int, responseCode: Int, responseMessage: String?) {
        //API Result 확인
        //todo 네트워크 오류인 경우에는 requestCode가 M_CODE_NETWORK_STATUS_RESULT로 응답이 오기때문에 예외처리 필요함.

        Log.e(
            "TEST",
            "REQUEST_CODE : " + requestCode + ", RESPONSE_CODE : " + responseCode + ", RESPONSE_MESSAGE : " + responseMessage
        )

        when (requestCode) {
            CashbeeResultCode.M_CODE_INIT_STATUS -> {
                if (responseCode == CashbeeResultCode.S_SUCCESS) {
                    if (mCashbeeHCE.isIssuedStatus) {
                        //캐시비 발급 상태 요청(getIssuedStatus)
                    } else {
                        //회원여부조회 요청(simpleUserInfoReg)
                        CoroutineScope(IO).launch {
                            mCashbeeHCE.simpleUserInfoReg(mbrId)
                        }

                    }
                }
            }

            CashbeeResultCode.M_CODE_SIMPLE_USER_INFO_REG_RESULT -> {
                if (responseCode == CashbeeResultCode.S_SUCCESS) {
                    mCashbeeHCE.getIssuedStatus()
                }
            }

            CashbeeResultCode.M_CODE_USIM_ISSUE_STATUS -> {
                Log.e("TEST", "상태 조회 : " + responseCode)
                when (responseCode) {
                    CashbeeResultCode.S_CODE_USIM_ISSUE_STATUS_1ST -> {
                        //미발급 상태(카드 발급요청 호출 필요)
                        CoroutineScope(IO).launch {
                            mCashbeeHCE.registerPerso()
                        }
                    }
                    CashbeeResultCode.S_CODE_USIM_ISSUE_STATUS_2ST -> {
                        //발급 완료 상태(유저정보 요청 필요)
                        CoroutineScope(IO).launch {
                            mCashbeeHCE.getUserInfoLookup()
                        }
                    }
                    CashbeeResultCode.S_CODE_USIM_ISSUE_STATUS_1ST -> {
                        //앱 재설치 된 상태(전체 동기화 요청 필요)
                        CoroutineScope(IO).launch {
                            mCashbeeHCE.membersInfoSyncAll(mbrId)
                        }
                    }
                    CashbeeResultCode.S_CODE_USIM_ISSUE_STATUS_1ST -> {
                        //상태 호출 실패
                    }
                    CashbeeResultCode.S_CODE_USIM_ISSUE_STATUS_1ST -> {
                        //약관 미동의 상태(약관 동의 진행)
                        //responseMessage 메시지
                        //=>신규동의필요여부 : Y -> setPersonalProvision() (신규약관동의 진행), N -> setPersonalProvisionSelection() (변경약관동의 진행)
                        //=>약관코드 : 동의필요한 필수약관코드 ex)"01,02,03"
                    }
                    CashbeeResultCode.S_CODE_USIM_ISSUE_STATUS_1ST -> {
                        //회원탈퇴 상태(회원탈퇴철회 요청 필요)
                        //responsemessage 메시지
                        //=>leaveReqDtti -> 탈퇴신청일시(YYYYMMDDHHMMSS), leaveReqMchtNm -> 탈퇴신청제휴사명
                    }

                }
            }
        }

//        if (requestCode == 1000) {
//
//            Log.e("TEST", "초기화 : " + responseCode)
////            Log.e("TEST", "발급 내역 조회 : " + mCashbeeHCE.isIssuedStatus)
//        }
//        if (requestCode == 1000 && responseCode == 0) {
//            Log.e("TEST", "발급 내역 조회 : " + mCashbeeHCE.isIssuedStatus)
////            init()
////    GlobalScope.launch(Dispatchers.Main) {
////        mCashbeeHCE.requestServerStatus()
////    }
//            CoroutineScope(IO).launch {
//                mCashbeeHCE.requestServerStatus()
//            }
//
//
//
//        } else if (requestCode == 1301 && responseCode == 0) {
//            CoroutineScope(IO).launch {
//                mCashbeeHCE.getIssuedStatus()
//            }
//            Log.e("TEST", "서버 상태 조회 : " + responseCode + ", " + responseMessage)
//            Log.e("TEST", "발급 상태 체크 : " + responseCode + ", " + responseMessage)
//        }


//        Log.e("TEST","서버 상태 조회 : " + mCashbeeHCE.requestServerStatus())
//        Log.e("TEST","캐시비 리스너3 : " + mCashbeeHCE.getIssuedStatus())

    }




}