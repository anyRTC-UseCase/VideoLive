package io.anyrtc.videolive.ui.activity

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.gyf.immersionbar.ImmersionBar
import com.kongzue.dialog.util.BaseDialog
import com.kongzue.dialog.v3.CustomDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.databinding.ActivityMainBinding
import io.anyrtc.videolive.ui.fragment.HomeFragment
import io.anyrtc.videolive.ui.fragment.MineFragment
import io.anyrtc.videolive.utils.replaceFragment

class MainActivity : BaseActivity() {

    private  val binding by lazy {  ActivityMainBinding.inflate(layoutInflater)}
    private  val fragments by lazy {
        arrayListOf(HomeFragment(), MineFragment())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        replaceFragment(fragments[0])
        binding.bottomView.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId){
                R.id.rb_home->{
                    replaceFragment(fragments[0])
                }
                R.id.rb_mine->{
                    replaceFragment(fragments[1])
                }
            }
        }

    }


}