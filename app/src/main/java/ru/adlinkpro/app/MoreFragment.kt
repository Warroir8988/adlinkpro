package ru.adlinkpro.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class MoreFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        val token = sp.getString("token", null)
        val tvRef = view.findViewById<TextView>(R.id.tv_referral)

        view.findViewById<Button>(R.id.btn_share).setOnClickListener {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Зарабатывайте на показах рекламы с AdLinkPro: https://adlinkpro.ru/?ref=" + tvRef.text)
            }
            startActivity(Intent.createChooser(i, "Поделиться"))
        }

        view.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            sp.edit().remove("token").remove("pin").apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        view.findViewById<TextView>(R.id.row_sites).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SitesFragment())
                .addToBackStack(null)
                .commit()
        }

        val stubs = mapOf(
            R.id.row_campaigns to "Рекламные кампании",
            R.id.row_deposit to "Пополнение",
            R.id.row_withdraw to "Вывод средств",
            R.id.row_stats to "Статистика",
            R.id.row_ref to "Реферальная программа",
            R.id.row_support to "Поддержка",
            R.id.row_about to "О платформе"
        )
        for ((id, name) in stubs) {
            view.findViewById<TextView>(id).setOnClickListener {
                Toast.makeText(requireContext(), "$name — раздел появится в следующем обновлении", Toast.LENGTH_SHORT).show()
            }
        }

        thread {
            try {
                val json = Api.post("data.php", emptyMap(), token)
                if (json.optBoolean("ok")) {
                    activity?.runOnUiThread { tvRef.text = json.optString("referral_code", "—") }
                }
            } catch (_: Exception) {}
        }
    }
}
