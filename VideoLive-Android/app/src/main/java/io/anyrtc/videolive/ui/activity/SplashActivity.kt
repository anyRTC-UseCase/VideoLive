package io.anyrtc.videolive.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.anyrtc.videolive.R
import io.anyrtc.videolive.utils.Interval
import io.anyrtc.videolive.utils.go
import io.anyrtc.videolive.utils.goAndFinish
import io.anyrtc.videolive.utils.toast
import java.util.concurrent.TimeUnit

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        XXPermissions.with(this).permission(Permission.CAMERA,Permission.RECORD_AUDIO,Permission.WRITE_EXTERNAL_STORAGE)
            .request { permissions, all ->
                if (all){
                    Interval(0, 1, TimeUnit.SECONDS, 1).life(this).finish {
                        goAndFinish(MainActivity::class.java)
                    }.start()
                }else{
                    toast("请开启权限")
                    finish()
                }
            }

    }
}