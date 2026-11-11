package ru.adlinkpro.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class WalletFragment : Fragment() {
    private data class Gw(val code: String, val title: String, val desc: String, val min: Double, val max: Double)
    private data class FkMethod(val key: String, val name: String, val min: Double, val max: Double, val type: String)

    private var savedToken: String? = null
    private var balanceMain = 0.0
    private var balanceAd = 0.0
    private var commissionPct = 0.0
    private var minWithdraw = 100.0
    private var depGws: List<Gw> = emptyList()
    private var wdGws: List<Gw> = emptyList()
    private var fkMethods: List<FkMethod> = emptyList()
    private var sbpBanks: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sp = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
        savedToken = sp.getString("token", null)

        view.findViewById<TextView>(R.id.tv_wallet_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val spDep = view.findViewById<Spinner>(R.id.sp_dep_gateway)
        val spWd = view.findViewById<Spinner>(R.id.sp_wd_gateway)
        val spFkMethod = view.findViewById<Spinner>(R.id.sp_fk_method)
        val spBank = view.findViewById<Spinner>(R.id.sp_bank)
        val etDepAmount = view.findViewById<EditText>(R.id.et_dep_amount)
        val etWdAmount = view.findViewById<EditText>(R.id.et_wd_amount)
        val etWdDetails = view.findViewById<EditText>(R.id.et_wd_details)
        val etBankOther = view.findViewById<EditText>(R.id.et_bank_other)
        val etWdPhone = view.findViewById<EditText>(R.id.et_wd_phone)
        val tvDepGwInfo = view.findViewById<TextView>(R.id.tv_dep_gw_info)
        val tvWdGwInfo = view.findViewById<TextView>(R.id.tv_wd_gw_info)
        val tvWdInfo = view.findViewById<TextView>(R.id.tv_wd_info)
        val tvWdSummary = view.findViewById<TextView>(R.id.tv_wd_summary)
        val tvDepStatus = view.findViewById<TextView>(R.id.tv_dep_status)
        val tvWdStatus = view.findViewById<TextView>(R.id.tv_wd_status)
        val layFkMethod = view.findViewById<LinearLayout>(R.id.lay_fk_method)
        val layFkManual = view.findViewById<LinearLayout>(R.id.lay_fk_manual)

        loadInfo(view, tvWdInfo)

        spDep.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos in depGws.indices) {
                    val g = depGws[pos]
                    tvDepGwInfo.text = g.desc + " · от " + trimNum(g.min) + " до " + trimNum(g.max) + " ₽"
                }
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        spWd.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos !in wdGws.indices) return
                val g = wdGws[pos]
                tvWdGwInfo.text = g.desc + " · от " + trimNum(g.min) + " до " + (if (g.max > 0) trimNum(g.max) + " ₽" else "без ограничений")
                updateWdFields(view, g, layFkMethod, layFkManual, etBankOther, spFkMethod, etWdDetails)
                updateSummary(tvWdSummary, etWdAmount.text.toString())
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        spFkMethod.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (pos in fkMethods.indices) {
                    etWdDetails.hint = when (fkMethods[pos].type) {
                        "sbp" -> "Например: +7 912 345-67-89"
                        "card" -> "Например: 4276 1234 5678 9012"
                        "yoomoney" -> "Например: 41001234567890"
                        "fkwallet" -> "Например: F1234567890"
                        else -> "Реквизиты получателя"
                    }
                }
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        spBank.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val isOther = pos == sbpBanks.size
                etBankOther.visibility = if (isOther) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        etWdAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateSummary(tvWdSummary, s?.toString() ?: "")
            }
        })

        view.findViewById<Button>(R.id.btn_deposit).setOnClickListener {
            val pos = spDep.selectedItemPosition
            val amount = etDepAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            if (pos !in depGws.indices) { tvDepStatus.text = "Способ оплаты не выбран"; return@setOnClickListener }
            val g = depGws[pos]
            when {
                amount <= 0 -> tvDepStatus.text = "Введите сумму"
                amount < g.min || amount > g.max -> tvDepStatus.text = "Сумма должна быть от " + trimNum(g.min) + " до " + trimNum(g.max) + " ₽"
                else -> {
                    tvDepStatus.text = "Создаём платёж…"
                    thread {
                        try {
                            val json = Api.post("wallet.php", mapOf("action" to "deposit", "gateway" to g.code, "amount" to trimNum(amount)), savedToken)
                            activity?.runOnUiThread {
                                if (json.optBoolean("ok") && json.optString("url").isNotEmpty()) {
                                    tvDepStatus.setTextColor(Color.parseColor("#10B981"))
                                    tvDepStatus.text = "Открываем страницу оплаты…"
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(json.optString("url"))))
                                    } catch (e: Exception) {
                                        tvDepStatus.setTextColor(Color.parseColor("#EF4444"))
                                        tvDepStatus.text = "Не удалось открыть браузер"
                                    }
                                } else {
                                    tvDepStatus.setTextColor(Color.parseColor("#EF4444"))
                                    tvDepStatus.text = json.optString("error", "Ошибка")
                                }
                            }
                        } catch (e: Exception) {
                            activity?.runOnUiThread {
                                tvDepStatus.setTextColor(Color.parseColor("#EF4444"))
                                tvDepStatus.text = "Нет связи с сервером"
                            }
                        }
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btn_withdraw).setOnClickListener {
            val pos = spWd.selectedItemPosition
            val amount = etWdAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            if (pos !in wdGws.indices) { tvWdStatus.text = "Способ вывода не выбран"; return@setOnClickListener }
            val g = wdGws[pos]
            val code = g.code
            var details = etWdDetails.text.toString().trim()
            var fkKey = ""
            var bank = ""
            var phone = ""

            var err: String? = null
            when (code) {
                "azvox" -> {
                    details = details.uppercase()
                    if (!Regex("^W\\d{4,12}$").matches(details)) err = "Кошелек Azvox должен иметь вид W12345"
                }
                "anypay" -> {
                    val det = details.replace(Regex("[\\s\\-]"), "")
                    if (!Regex("^(\\+?\\d{10,15}|\\d{16,19})$").matches(det)) err = "Укажите номер карты (16 цифр) или телефон СБП (+7XXXXXXXXXX)"
                    details = det
                }
                "fk_manual" -> {
                    val bi = spBank.selectedItemPosition
                    bank = if (bi == sbpBanks.size) etBankOther.text.toString().trim() else (if (bi in sbpBanks.indices) sbpBanks[bi] else "")
                    phone = etWdPhone.text.toString().trim().replace(Regex("[\\s\\-]"), "")
                    if (!Regex("^\\+?\\d{10,15}$").matches(phone)) err = "Укажите номер телефона в формате +7XXXXXXXXXX"
                    else if (bank.length < 2) err = "Выберите или укажите банк получателя"
                    details = phone
                }
                "freekassa" -> {
                    val mi = spFkMethod.selectedItemPosition
                    if (mi !in fkMethods.indices) {
                        err = "Выберите способ оплаты"
                    } else {
                        val m = fkMethods[mi]
                        fkKey = m.key
                        val det = details.replace(Regex("[\\s\\-]"), "")
                        val okDet = when (m.type) {
                            "sbp" -> Regex("^\\+?\\d{10,15}$").matches(det)
                            "card" -> Regex("^\\d{16,19}$").matches(det)
                            "yoomoney" -> Regex("^\\d{11,16}$").matches(det)
                            else -> details.length >= 5
                        }
                        if (!okDet) err = "Укажите корректные реквизиты"
                        else if (m.min > 0 && amount < m.min) err = "Минимум для «" + m.name + "»: " + trimNum(m.min) + " ₽"
                        else if (m.max > 0 && amount > m.max) err = "Максимум для «" + m.name + "»: " + trimNum(m.max) + " ₽"
                        details = if (m.type == "fkwallet" || m.type == "other") details else det
                    }
                }
                else -> if (details.length < 5) err = "Укажите корректные реквизиты"
            }

            when {
                err != null -> tvWdStatus.text = err
                amount < g.min -> tvWdStatus.text = "Минимальная сумма вывода: " + trimNum(g.min) + " ₽"
                g.max > 0 && amount > g.max -> tvWdStatus.text = "Максимальная сумма вывода: " + trimNum(g.max) + " ₽"
                amount > balanceMain -> tvWdStatus.text = "Недостаточно средств"
                else -> {
                    tvWdStatus.text = "Отправляем заявку…"
                    val params = mutableMapOf(
                        "action" to "withdraw",
                        "gateway" to code,
                        "amount" to trimNum(amount),
                        "details" to details
                    )
                    if (fkKey.isNotEmpty()) params["fk_method"] = fkKey
                    if (code == "fk_manual") {
                        params["fk_manual_bank"] = bank
                        params["fk_manual_phone"] = phone
                    }
                    thread {
                        try {
                            val json = Api.post("wallet.php", params, savedToken)
                            activity?.runOnUiThread {
                                if (json.optBoolean("ok")) {
                                    tvWdStatus.setTextColor(Color.parseColor("#10B981"))
                                    tvWdStatus.text = json.optString("message", "Заявка создана")
                                    etWdAmount.setText("")
                                    etWdDetails.setText("")
                                    etWdPhone.setText("")
                                    etBankOther.setText("")
                                    thread {
                                        try {
                                            val inf = Api.post("wallet.php", mapOf("action" to "info"), savedToken)
                                            if (inf.optBoolean("ok")) {
                                                balanceMain = inf.optDouble("balance", 0.0)
                                                balanceAd = inf.optDouble("ad_balance", 0.0)
                                                activity?.runOnUiThread { loadInfoText(view) }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    loadHistory(view)
                                } else {
                                    tvWdStatus.setTextColor(Color.parseColor("#EF4444"))
                                    tvWdStatus.text = json.optString("error", "Ошибка")
                                }
                            }
                        } catch (e: Exception) {
                            activity?.runOnUiThread {
                                tvWdStatus.setTextColor(Color.parseColor("#EF4444"))
                                tvWdStatus.text = "Нет связи с сервером"
                            }
                        }
                    }
                }
            }
        }

        loadHistory(view)
    }

    private fun loadInfo(view: View, tvWdInfo: TextView) {
        thread {
            var err = ""
            try {
                val json = Api.post("wallet.php", mapOf("action" to "info"), savedToken)
                if (json.optBoolean("ok")) {
                    balanceMain = json.optDouble("balance", 0.0)
                    balanceAd = json.optDouble("ad_balance", 0.0)
                    commissionPct = json.optDouble("commission_percent", 0.0)
                    minWithdraw = json.optDouble("min_withdraw", 100.0)
                    depGws = parseGws(json.optJSONArray("deposit_gws"))
                    wdGws = parseGws(json.optJSONArray("withdraw_gws"))
                    fkMethods = mutableListOf<FkMethod>().also { list ->
                        val arr = json.optJSONArray("fk_methods")
                        for (i in 0 until (arr?.length() ?: 0)) {
                            val o = arr!!.getJSONObject(i)
                            list.add(FkMethod(o.optString("key"), o.optString("name"), o.optDouble("min", 0.0), o.optDouble("max", 0.0), o.optString("type")))
                        }
                    }
                    sbpBanks = mutableListOf<String>().also { list ->
                        val arr = json.optJSONArray("sbp_banks")
                        for (i in 0 until (arr?.length() ?: 0)) list.add(arr!!.getString(i))
                    }
                    activity?.runOnUiThread {
                        loadInfoText(view)
                        if (depGws.isNotEmpty()) {
                            val sp = view.findViewById<Spinner>(R.id.sp_dep_gateway)
                            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, depGws.map { it.title })
                            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            sp.adapter = adapter
                        } else {
                            view.findViewById<TextView>(R.id.tv_dep_gw_info).text = "Пополнение временно недоступно."
                        }
                        if (wdGws.isNotEmpty()) {
                            val sp = view.findViewById<Spinner>(R.id.sp_wd_gateway)
                            val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, wdGws.map { it.title })
                            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                            sp.adapter = adapter
                        } else {
                            view.findViewById<TextView>(R.id.tv_wd_gw_info).text = "Способы вывода временно недоступны."
                        }
                        val spFk = view.findViewById<Spinner>(R.id.sp_fk_method)
                        val fkAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, fkMethods.map { it.name + (if (it.min > 0) " (мин. " + trimNum(it.min) + " ₽)" else "") })
                        fkAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                        spFk.adapter = fkAdapter
                        val spBank = view.findViewById<Spinner>(R.id.sp_bank)
                        val bankNames = sbpBanks + "Другой банк (вписать свой)"
                        val bankAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, bankNames)
                        bankAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                        spBank.adapter = bankAdapter
                        tvWdInfo.text = "Комиссия вывода: " + trimNum(commissionPct) + "% · Минимум: " + trimNum(minWithdraw) + " ₽ · Доступно: " + trimNum(balanceMain) + " ₽"
                        view.findViewById<TextView>(R.id.tv_wd_status).text = ""
                    }
                } else err = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                err = "Нет связи с сервером"
            }
            if (err.isNotEmpty()) {
                activity?.runOnUiThread {
                    tvWdInfo.text = err
                }
            }
        }
    }

    private fun parseGws(arr: org.json.JSONArray?): List<Gw> {
        val list = mutableListOf<Gw>()
        for (i in 0 until (arr?.length() ?: 0)) {
            val o = arr!!.getJSONObject(i)
            list.add(Gw(o.optString("code"), o.optString("title"), o.optString("desc"), o.optDouble("min", 0.0), o.optDouble("max", 0.0)))
        }
        return list
    }

    private fun loadInfoText(view: View) {
        view.findViewById<TextView>(R.id.tv_wallet_balances).text =
            "Основной баланс: " + trimNum(balanceMain) + " ₽\nРекламный баланс: " + trimNum(balanceAd) + " ₽ (тратится только на рекламу)"
        view.findViewById<TextView>(R.id.tv_wd_info).text =
            "Комиссия вывода: " + trimNum(commissionPct) + "% · Минимум: " + trimNum(minWithdraw) + " ₽ · Доступно: " + trimNum(balanceMain) + " ₽"
    }

    private fun updateWdFields(
        view: View, g: Gw,
        layFkMethod: LinearLayout, layFkManual: LinearLayout,
        etBankOther: EditText, spFkMethod: Spinner, etWdDetails: EditText
    ) {
        when (g.code) {
            "azvox" -> {
                etWdDetails.visibility = View.VISIBLE
                etWdDetails.hint = "W12345"
                layFkMethod.visibility = View.GONE
                layFkManual.visibility = View.GONE
            }
            "anypay" -> {
                etWdDetails.visibility = View.VISIBLE
                etWdDetails.hint = "4276 1234 5678 9012 или +7 912 345-67-89"
                layFkMethod.visibility = View.GONE
                layFkManual.visibility = View.GONE
            }
            "freekassa" -> {
                etWdDetails.visibility = View.VISIBLE
                layFkMethod.visibility = if (fkMethods.isEmpty()) View.GONE else View.VISIBLE
                layFkManual.visibility = View.GONE
                if (fkMethods.isEmpty()) etWdDetails.hint = "Список способов недоступен — сервис FreeKassa не отвечает"
            }
            "fk_manual" -> {
                etWdDetails.visibility = View.GONE
                layFkMethod.visibility = View.GONE
                layFkManual.visibility = View.VISIBLE
            }
            else -> {
                etWdDetails.visibility = View.VISIBLE
                etWdDetails.hint = "Номер карты, СБП по телефону, ЮMoney и т.д."
                layFkMethod.visibility = View.GONE
                layFkManual.visibility = View.GONE
            }
        }
    }

    private fun updateSummary(tv: TextView, raw: String) {
        val amount = raw.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (amount <= 0) {
            tv.visibility = View.GONE
            return
        }
        val commission = amount * commissionPct / 100.0
        val toPay = amount - commission
        tv.text = "Сумма заявки: " + trimNum(amount) + " ₽\nКомиссия (" + trimNum(commissionPct) + "%): -" + trimNum(commission) + " ₽\nК получению: " + trimNum(toPay) + " ₽"
        tv.visibility = View.VISIBLE
    }

    private fun loadHistory(view: View) {
        val wdList = view.findViewById<LinearLayout>(R.id.wd_list)
        val depList = view.findViewById<LinearLayout>(R.id.dep_list)
        thread {
            var wds: List<org.json.JSONObject> = emptyList()
            var deps: List<org.json.JSONObject> = emptyList()
            var err = ""
            try {
                val json = Api.post("wallet.php", mapOf("action" to "history"), savedToken)
                if (json.optBoolean("ok")) {
                    wds = jsonList(json.optJSONArray("withdrawals"))
                    deps = jsonList(json.optJSONArray("deposits"))
                } else err = json.optString("error", "Ошибка")
            } catch (e: Exception) {
                err = "Нет связи с сервером"
            }
            activity?.runOnUiThread {
                wdList.removeAllViews()
                depList.removeAllViews()
                if (err.isNotEmpty()) {
                    wdList.addView(label(err, Color.parseColor("#EF4444")))
                    return@runOnUiThread
                }
                if (wds.isEmpty()) {
                    wdList.addView(label("Заявок пока нет.", Color.parseColor("#64748B")))
                } else {
                    for (w in wds) {
                        val box = cardBox()
                        val gwName = when (w.optString("gateway")) {
                            "azvox" -> "Azvox"
                            "freekassa" -> "FreeKassa"
                            "anypay" -> "AnyPay"
                            "fk_manual" -> "FreeKassa · СБП"
                            "manual" -> "Реквизиты"
                            else -> w.optString("gateway")
                        }
                        val status = wdStatus(w.optString("status"))
                        box.addView(line("#" + w.optInt("id") + " · " + w.optString("date") + " · " + gwName, "#1E293B", true))
                        box.addView(line("Сумма: " + w.optString("amount") + " ₽ · К получению: " + w.optString("to_pay") + " ₽", "#64748B", false))
                        if (w.optString("details").isNotEmpty()) box.addView(line(w.optString("details"), "#64748B", false))
                        box.addView(line(status.first, status.second, false))
                        wdList.addView(box)
                    }
                }
                if (deps.isEmpty()) {
                    depList.addView(label("Пополнений пока нет.", Color.parseColor("#64748B")))
                } else {
                    for (d in deps) {
                        val box = cardBox()
                        val status = depStatus(d.optString("status"))
                        box.addView(line("#" + d.optInt("id") + " · " + d.optString("date") + " · " + d.optString("gateway"), "#1E293B", true))
                        box.addView(line("Сумма: " + d.optString("amount") + " ₽", "#64748B", false))
                        box.addView(line(status.first, status.second, false))
                        depList.addView(box)
                    }
                }
            }
        }
    }

    private fun jsonList(arr: org.json.JSONArray?): List<org.json.JSONObject> {
        val list = mutableListOf<org.json.JSONObject>()
        for (i in 0 until (arr?.length() ?: 0)) list.add(arr!!.getJSONObject(i))
        return list
    }

    private fun wdStatus(s: String): Pair<String, String> = when (s) {
        "pending" -> "⏳ В обработке" to "#F59E0B"
        "paid", "done" -> "✅ Выплачено" to "#10B981"
        "rejected", "failed" -> "❌ Отклонено" to "#EF4444"
        else -> s to "#64748B"
    }

    private fun depStatus(s: String): Pair<String, String> = when (s) {
        "new", "pending" -> "⏳ Ожидает" to "#F59E0B"
        "paid", "success" -> "✅ Зачислено" to "#10B981"
        "fail", "refunded" -> "❌ " + (if (s == "fail") "Ошибка" else "Возврат") to "#EF4444"
        else -> s to "#64748B"
    }

    private fun cardBox(): LinearLayout {
        val box = LinearLayout(requireContext())
        box.orientation = LinearLayout.VERTICAL
        box.background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 40f }
        box.setPadding(40, 36, 40, 36)
        box.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 20 }
        return box
    }

    private fun line(text: String, color: String, bold: Boolean): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = if (bold) 15f else 13f
        tv.setTextColor(Color.parseColor(color))
        if (bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
        return tv
    }

    private fun label(text: String, color: Int): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(color)
        return tv
    }

    private fun trimNum(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.2f", v)
    }
}
