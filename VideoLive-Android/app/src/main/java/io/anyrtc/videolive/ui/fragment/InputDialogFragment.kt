package io.anyrtc.videolive.ui.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.anyrtc.videolive.R
import io.anyrtc.videolive.utils.launch
import io.anyrtc.videolive.utils.toast
import kotlinx.coroutines.delay

class InputDialogFragment : DialogFragment() {

    private lateinit var inputListener: (String) -> Unit

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val window = dialog?.window
        val view = layoutInflater.inflate(
            R.layout.dialog_input,
            window?.findViewById(android.R.id.content),
            false
        )
        window?.let {
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            it.setGravity(Gravity.BOTTOM)
            it.setDimAmount(0f)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etContent = view.findViewById<EditText>(R.id.input)
        etContent.run {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        launch({
            delay(100)
            val inputManager: InputMethodManager =
                etContent.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(etContent, InputMethodManager.SHOW_IMPLICIT)
        })
        view.findViewById<TextView>(R.id.send).setOnClickListener {
            if (etContent.text.toString().isEmpty()) {
                toast("请输入要发送的内容")
                return@setOnClickListener
            } else {
                inputListener.invoke(etContent.text.toString())
                etContent.text.clear()
                dismiss()
            }
        }
    }

    fun show(manager: FragmentManager, callback: (String) -> Unit) {
        this.inputListener = callback
        show(manager, "input")
    }
}