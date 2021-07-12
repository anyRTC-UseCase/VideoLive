package io.anyrtc.videolive.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import io.anyrtc.videolive.R
import io.anyrtc.videolive.sdk.RtcManager

abstract class BaseActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImmersionBar.with(this).fitsSystemWindows(true).statusBarColor(R.color.white).statusBarDarkFont(
            true
        ).navigationBarColor(R.color.white, 0.2f).navigationBarDarkIcon(true).init()

    }
}