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

    override fun onResume() {
        super.onResume()
        // Обновляем счётчик при возвращении на главный экран — например, после просмотра уведомлений
        loadNotifCount()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null)
        val tvInfo = view.findViewById<TextView>(R.id.tv_info)
        val tvBalance = view.findViewById<TextView>(R.id.tv_balance)
        val tvHold = view.findViewById<TextView>(R.id.tv_hold)

        tvBalance.text = "— ₽"
        tvHold.text = "В холде: — ₽"

        if (token == null) return

        // Тап по колокольчику — открываем уведомления
        view.findViewById<View>(R.id.bell_container).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }

        loadNotifCount()

        thread {
            try {
                val json = Api.post("data.php", emptyMap(), token)
                if (json.optBoolean("ok")) {
                    activity?.runOnUiThread {
                        tvInfo.text = "Вы вошли как " + json.optString("welcome", "").removePrefix("Добро пожаловать, ")
                        tvBalance.text = json.optString("balance", "0.00") + " ₽"
                        val adv = json.optString("ad_balance", "0.00")
                        val hold = json.optString("hold", "0.00")
                        tvHold.text = "Рекламный баланс: $adv ₽  •  В холде: $hold ₽"
                    }
                } else {
                    activity?.runOnUiThread {
                        tvInfo.text = "Не удалось загрузить данные: " + json.optString("error", "")
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    tvInfo.text = "Нет связи с сервером"
                }
            }
        }
    }

    private fun loadNotifCount() {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null) ?: return
        val badge = view?.findViewById<TextView>(R.id.bell_badge) ?: return

        thread {
            try {
                val json = Api.post("notifications.php", mapOf("action" to "count"), token)
                if (!json.optBoolean("ok")) return@thread
                val unread = json.optInt("unread", 0)
                activity?.runOnUiThread {
                    if (unread > 0) {
                        badge.visibility = View.VISIBLE
                        badge.text = if (unread > 99) "99+" else unread.toString()
                    } else {
                        badge.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
