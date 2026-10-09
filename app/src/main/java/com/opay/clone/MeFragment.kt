package com.opay.clone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class MeFragment : Fragment() {

    private var hidden = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_me, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshMe(view)

        view.findViewById<ImageView>(R.id.btnMeEye).setOnClickListener {
            hidden = !hidden
            val bal = AppPrefs.getBalance(requireContext())
            view.findViewById<TextView>(R.id.tvMeBalance).text = formatBalance(bal, hidden)
            view.findViewById<ImageView>(R.id.btnMeEye).setImageResource(
                if (hidden) R.drawable.opay_me_eye_hide else R.drawable.opay_me_eye_show
            )
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshMe(it) }
    }

    private fun refreshMe(view: View) {
        val name = AppPrefs.getName(requireContext()).trim()
        val first = name.split(" ").firstOrNull()?.takeIf { it.isNotEmpty() } ?: "there"
        view.findViewById<TextView>(R.id.tvMeGreeting).text = "Hi, $first"
        val bal = AppPrefs.getBalance(requireContext())
        view.findViewById<TextView>(R.id.tvMeBalance).text = formatBalance(bal, hidden)
    }

    private fun formatBalance(raw: String, hide: Boolean): String {
        if (hide) return "₦ ****"
        val d = raw.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        return "₦" + String.format("%,.2f", d)
    }
}

