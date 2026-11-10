package ru.adlinkpro.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class NewsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvList = view.findViewById<TextView>(R.id.tv_news)
        val token = requireContext().getSharedPreferences("app", MODE_PRIVATE).getString("token", null)
        thread {
            try {
                val json = Api.post("data.php", emptyMap(), token)
                if (json.optBoolean("ok")) {
                    val arr = json.optJSONArray("notifications")
                    val sb = StringBuilder()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val n = arr.getJSONObject(i)
                            sb.append("• ").append(n.optString("title", "")).append("\n")
                            sb.append(n.optString("text", "")).append("\n\n")
                        }
                    }
                    activity?.runOnUiThread {
                        tvList.text = if (sb.isBlank()) "Пока новостей нет" else sb.toString().trim()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread { tvList.text = "Нет связи с сервером" }
            }
        }
    }
}
