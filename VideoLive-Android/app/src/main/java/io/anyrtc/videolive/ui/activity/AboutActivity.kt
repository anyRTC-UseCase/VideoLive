package io.anyrtc.videolive.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.anyrtc.videolive.R
import io.anyrtc.videolive.databinding.ActivityAboutBinding
import io.anyrtc.videolive.databinding.ActivityHostBinding

class AboutActivity : BaseActivity() {

    private val binding by lazy { ActivityAboutBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}