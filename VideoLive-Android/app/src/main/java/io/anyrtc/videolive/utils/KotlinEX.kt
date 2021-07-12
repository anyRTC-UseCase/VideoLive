package io.anyrtc.videolive.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.anyrtc.videolive.App
import io.anyrtc.videolive.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

fun Activity.toast(str: String) {
    Toast.makeText(this,str, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(str: String) {
    Toast.makeText(activity,str, Toast.LENGTH_SHORT).show()
}
fun Float.dp():Int{
    return (this * App.app.resources.displayMetrics.density).roundToInt()
}

fun Float.dp2px():Int{
    return (0.5f + this * App.app.resources.displayMetrics.density).roundToInt()
}


inline fun <T> Boolean.ternary(trueValue: T, falseValue: T): T {
    return if (this) {
        trueValue
    } else {
        falseValue
    }
}

fun launch(
    block: suspend (CoroutineScope) -> Unit,
    error_: ((e: Throwable) -> Unit)? = null,
    context: CoroutineContext = Dispatchers.Main
) = GlobalScope.launch(context + CoroutineExceptionHandler { _, e ->
    error_?.let { it(e) }
}) {
    try {
        block(this)
    } catch (e: Exception) {
        e.printStackTrace()
        error_?.let { it(e) }
    }
}


/**
 * 默认主线程的协程
 * 添加生命周期管理
 */
fun launchWithLife(
    life: LifecycleOwner?,
    block: suspend (CoroutineScope) -> Unit,
    error_: ((e: Throwable) -> Unit)? = null,
    context: CoroutineContext = Dispatchers.Main
) {
    val job = launch(block, error_, context)

    life?.lifecycle?.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (life.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                job.cancel("the lifecycleOwner(${life.javaClass.simpleName}) has been destroyed")
                return;
            }
        }
    })
}

fun<T> Activity.go(clazz: Class<T>){
    startActivity(Intent().apply {
        setClass(this@go,clazz)
    })
}

fun<T> Activity.goAndFinish(clazz: Class<T>){
    startActivity(Intent().apply {
        setClass(this@goAndFinish,clazz)
        finish()
    })
}

fun View.gone(){
    this.visibility = View.GONE
}

fun View.show(){
    this.visibility = View.VISIBLE
}

fun FragmentActivity.replaceFragment(replaceFragment: Fragment, id: Int = R.id.frameLayout) {
    val tag = replaceFragment::class.java.name
    var tempFragment = supportFragmentManager.findFragmentByTag(tag)
    val transaction = supportFragmentManager.beginTransaction()
    if (tempFragment == null) {
        try {
            tempFragment = replaceFragment.apply {

            }
            transaction
                .add(id, tempFragment, tag)
                .setMaxLifecycle(tempFragment, Lifecycle.State.RESUMED)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    val fragments = supportFragmentManager.fragments

    for (i in fragments.indices) {
        val fragment = fragments[i]
        if (fragment.tag == tag) {
            transaction
                .show(fragment)
        } else {
            transaction
                .hide(fragment)
        }
    }
    transaction.commitAllowingStateLoss()
}


fun getRandomName():String{
    return Constans.firstName.shuffled().take(1).first()+Constans.secondName.shuffled().take(1).first()
}

internal fun View.throttleClick(
        interval: Long = 500,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        block: View.() -> Unit
) {
    setOnClickListener(ThrottleClickListener(interval, unit, block))
}


internal class ThrottleClickListener(
        private val interval: Long = 500,
        private val unit: TimeUnit = TimeUnit.MILLISECONDS,
        private var block: View.() -> Unit
) : View.OnClickListener {

    private var lastTime: Long = 0

    override fun onClick(v: View) {

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTime > unit.toMillis(interval)) {
            lastTime = currentTime
            block(v)
        }

    }
}



