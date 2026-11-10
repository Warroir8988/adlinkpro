package ru.adlinkpro.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val token = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE).getString("token", null) ?: return
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance)
        val tvHold = view.findViewById<TextView>(R.id.tv_hold)
        val tvInfo = view.findViewById<TextView>(R.id.tv_info)
        thread {
            try {
                val json = Api.post("data.php", emptyMap(), token)
                if (json.optBoolean("ok")) {
                    activity?.runOnUiThread {
                        tvBalance.text = json.optString("balance", "—") + " ₽"
                        tvHold.text = "В холде: " + json.optString("hold", "—") + " ₽"
                        tvInfo.text = json.optString("welcome", "Добро пожаловать в AdLinkPro")
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread { tvInfo.text = "Нет связи с сервером" }
            }
        }
    }
}
