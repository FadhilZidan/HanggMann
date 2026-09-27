package com.example.hangmanapp

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // State Permainan (Data Model)
    private lateinit var kataRahasia: String
    private val hurufBenar = mutableSetOf<Char>()
    private val hurufSalah = mutableSetOf<Char>()
    private var maksNyawa = 6
    private var skor = 0
    private var isGameEnded = false
    private var isMenang = false

    // View References
    private lateinit var tvNyawa: TextView
    private lateinit var tvSkor: TextView
    private lateinit var tvKata: TextView
    private lateinit var tvRiwayat: TextView
    private lateinit var etTebak: EditText
    private lateinit var btnTebak: Button
    private lateinit var gridKeyboard: GridLayout

    private lateinit var overlayMulai: View
    private lateinit var btnMulai: Button

    private lateinit var overlayKesulitan: View
    private lateinit var btnMudah: Button
    private lateinit var btnSedang: Button
    private lateinit var btnSulit: Button

    private lateinit var overlayHasil: View
    private lateinit var tvHasil: TextView
    private lateinit var tvKataAkhir: TextView
    private lateinit var btnMainLagi: Button

    private lateinit var bagianTubuh: List<ImageView>

    // Tombol Keyboard A-Z
    private val tombolHurufMap = mutableMapOf<Char, Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inisialisasiView()
        setupListeners()

        savedInstanceState?.let { pulihkanState(it) }
    }

    private fun inisialisasiView() {
        tvNyawa = findViewById(R.id.tvNyawa)
        tvSkor = findViewById(R.id.tvSkor)
        tvKata = findViewById(R.id.tvKata)
        tvRiwayat = findViewById(R.id.tvRiwayat)
        etTebak = findViewById(R.id.etTebak)
        btnTebak = findViewById(R.id.btnTebak)
        gridKeyboard = findViewById(R.id.gridKeyboard)

        overlayMulai = findViewById(R.id.overlayMulai)
        btnMulai = findViewById(R.id.btnMulai)

        overlayKesulitan = findViewById(R.id.overlayKesulitan)
        btnMudah = findViewById(R.id.btnMudah)
        btnSedang = findViewById(R.id.btnSedang)
        btnSulit = findViewById(R.id.btnSulit)

        overlayHasil = findViewById(R.id.overlayHasil)
        tvHasil = findViewById(R.id.tvHasil)
        tvKataAkhir = findViewById(R.id.tvKataAkhir)
        btnMainLagi = findViewById(R.id.btnMainLagi)

        bagianTubuh = listOf(
            findViewById(R.id.imgKepala),
            findViewById(R.id.imgBadan),
            findViewById(R.id.imgTanganKiri),
            findViewById(R.id.imgTanganKanan),
            findViewById(R.id.imgKakiKiri),
            findViewById(R.id.imgKakiKanan),
        )

        buatTombolKeyboard()
    }

    private fun buatTombolKeyboard() {
        gridKeyboard.removeAllViews()
        tombolHurufMap.clear()

        for (char in 'A'..'Z') {
            val button = Button(this).apply {
                text = char.toString()
                textSize = 15f
                isAllCaps = true
                background = ContextCompat.getDrawable(context, R.drawable.bg_key_default)
                setTextColor(ContextCompat.getColor(context, R.color.text_main))
                setPadding(0, 0, 0, 0)
                setOnClickListener { tebakHuruf(char) }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = (48 * resources.displayMetrics.density).toInt()
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            button.layoutParams = params

            tombolHurufMap[char] = button
            gridKeyboard.addView(button)
        }
    }

    private fun setupListeners() {
        btnMulai.setOnClickListener {
            overlayMulai.visibility = View.GONE
            overlayKesulitan.visibility = View.VISIBLE
        }

        btnMudah.setOnClickListener {
            maksNyawa = 8
            overlayKesulitan.visibility = View.GONE
            mulaiPermainan()
        }

        btnSedang.setOnClickListener {
            maksNyawa = 6
            overlayKesulitan.visibility = View.GONE
            mulaiPermainan()
        }

        btnSulit.setOnClickListener {
            maksNyawa = 4
            overlayKesulitan.visibility = View.GONE
            mulaiPermainan()
        }

        btnMainLagi.setOnClickListener {
            overlayHasil.visibility = View.GONE
            overlayKesulitan.visibility = View.VISIBLE
        }

        btnTebak.setOnClickListener { prosesTebakan() }

        etTebak.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                prosesTebakan()
                true
            } else {
                false
            }
        }
    }

    private fun mulaiPermainan() {
        val daftarKata = resources.getStringArray(R.array.daftar_kata)
        kataRahasia = daftarKata.random()

        hurufBenar.clear()
        hurufSalah.clear()
        isGameEnded = false
        isMenang = false

        etTebak.isEnabled = true
        etTebak.setText("")

        tombolHurufMap.values.forEach { button ->
            button.isEnabled = true
            button.background = ContextCompat.getDrawable(this, R.drawable.bg_key_default)
            button.setTextColor(ContextCompat.getColor(this, R.color.text_main))
        }

        overlayMulai.visibility = View.GONE
        overlayKesulitan.visibility = View.GONE
        overlayHasil.visibility = View.GONE

        // Reset tampilan gambar gantungan
        bagianTubuh.forEach { imageView ->
            imageView.animate().cancel()
            imageView.visibility = View.INVISIBLE
            imageView.alpha = 0f
        }

        gambarPapan()
    }

    private fun gambarPapan() {
        perbaruiGantungan(hurufSalah.size)

        val maskedText = kataRahasia.map { char ->
            if (hurufBenar.contains(char)) char else '_'
        }.joinToString(" ")
        tvKata.text = maskedText

        val sisaNyawa = (maksNyawa - hurufSalah.size).coerceAtLeast(0)
        tvNyawa.text = getString(R.string.nyawa_format, sisaNyawa)
        tvSkor.text = getString(R.string.skor_format, skor)

        val riwayat = if (hurufSalah.isEmpty()) "-" else hurufSalah.joinToString(", ")
        tvRiwayat.text = getString(R.string.riwayat_salah_format, riwayat)
    }

    private fun perbaruiGantungan(jumlahSalah: Int) {
        val visiblePartsCount = if (maksNyawa > 0) {
            ((jumlahSalah.toFloat() / maksNyawa.toFloat()) * bagianTubuh.size).toInt().coerceAtMost(bagianTubuh.size)
        } else {
            0
        }

        bagianTubuh.forEachIndexed { i, imageView ->
            if (i < visiblePartsCount) {
                if (imageView.visibility != View.VISIBLE) {
                    imageView.visibility = View.VISIBLE
                    imageView.alpha = 0f
                    imageView.animate().alpha(1f).setDuration(300).start()
                }
            } else {
                imageView.animate().cancel()
                imageView.visibility = View.INVISIBLE
                imageView.alpha = 0f
            }
        }
    }

    private fun tebakHuruf(char: Char) {
        if (isGameEnded || hurufBenar.contains(char) || hurufSalah.contains(char)) {
            return
        }

        val isCorrect = kataRahasia.contains(char)
        val button = tombolHurufMap[char]

        if (isCorrect) {
            hurufBenar.add(char)
            button?.apply {
                isEnabled = false
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_key_correct)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.key_correct_text))
            }
        } else {
            hurufSalah.add(char)
            button?.apply {
                isEnabled = false
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_key_wrong)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.key_wrong_text))
            }
        }

        gambarPapan()
        periksaAkhirPermainan()
    }

    private fun prosesTebakan() {
        val input = etTebak.text.toString().trim().uppercase()

        if ((input.length != 1) || (input[0] < 'A') || (input[0] > 'Z')) {
            Toast.makeText(this, getString(R.string.pesan_salah_input), Toast.LENGTH_SHORT).show()
            return
        }

        val char = input[0]

        if (hurufBenar.contains(char) || hurufSalah.contains(char)) {
            Toast.makeText(this, getString(R.string.pesan_sudah_ditebak, char), Toast.LENGTH_SHORT).show()
            etTebak.setText("")
            return
        }

        etTebak.setText("")
        tebakHuruf(char)
    }

    private fun periksaAkhirPermainan() {
        val menang = ::kataRahasia.isInitialized && kataRahasia.isNotEmpty() && kataRahasia.all { hurufBenar.contains(it) }
        val kalah = hurufSalah.size >= maksNyawa

        if (menang) {
            tampilkanHasil(menang = true)
        } else if (kalah) {
            tampilkanHasil(menang = false)
        }
    }

    private fun tampilkanHasil(menang: Boolean) {
        if (isGameEnded) return
        isGameEnded = true
        isMenang = menang

        etTebak.isEnabled = false
        tombolHurufMap.values.forEach { it.isEnabled = false }

        if (menang) {
            skor += 10
            tvHasil.text = getString(R.string.menang_title)
            tvHasil.setTextColor(ContextCompat.getColor(this, R.color.accent))
        } else {
            tvHasil.text = getString(R.string.kalah_title)
            tvHasil.setTextColor(ContextCompat.getColor(this, R.color.danger))
        }

        tvSkor.text = getString(R.string.skor_format, skor)
        tvKataAkhir.text = getString(R.string.kata_akhir_format, kataRahasia)

        overlayHasil.alpha = 0f
        overlayHasil.visibility = View.VISIBLE
        overlayHasil.animate().alpha(1f).setDuration(300).start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::kataRahasia.isInitialized) {
            outState.putString(KEY_KATA_RAHASIA, kataRahasia)
        }
        outState.putCharArray(KEY_HURUF_BENAR, hurufBenar.toCharArray())
        outState.putCharArray(KEY_HURUF_SALAH, hurufSalah.toCharArray())
        outState.putInt(KEY_SKOR, skor)
        outState.putInt(KEY_MAKS_NYAWA, maksNyawa)
        outState.putBoolean(KEY_IS_GAME_ENDED, isGameEnded)
        outState.putBoolean(KEY_IS_MENANG, isMenang)
        outState.putInt(KEY_OVERLAY_MULAI_VIS, overlayMulai.visibility)
        outState.putInt(KEY_OVERLAY_KESULTANAN_VIS, overlayKesulitan.visibility)
        outState.putInt(KEY_OVERLAY_HASIL_VIS, overlayHasil.visibility)
    }

    private fun pulihkanState(savedInstanceState: Bundle) {
        kataRahasia = savedInstanceState.getString(KEY_KATA_RAHASIA, "") ?: ""
        skor = savedInstanceState.getInt(KEY_SKOR, 0)
        maksNyawa = savedInstanceState.getInt(KEY_MAKS_NYAWA, 6)
        isGameEnded = savedInstanceState.getBoolean(KEY_IS_GAME_ENDED, false)
        isMenang = savedInstanceState.getBoolean(KEY_IS_MENANG, false)

        savedInstanceState.getCharArray(KEY_HURUF_BENAR)?.forEach { hurufBenar.add(it) }
        savedInstanceState.getCharArray(KEY_HURUF_SALAH)?.forEach { hurufSalah.add(it) }

        overlayMulai.visibility = savedInstanceState.getInt(KEY_OVERLAY_MULAI_VIS, View.VISIBLE)
        overlayKesulitan.visibility = savedInstanceState.getInt(KEY_OVERLAY_KESULTANAN_VIS, View.GONE)
        overlayHasil.visibility = savedInstanceState.getInt(KEY_OVERLAY_HASIL_VIS, View.GONE)

        if (kataRahasia.isNotEmpty()) {
            gambarPapan()

            tombolHurufMap.forEach { (char, button) ->
                when {
                    hurufBenar.contains(char) -> {
                        button.isEnabled = false
                        button.background = ContextCompat.getDrawable(this, R.drawable.bg_key_correct)
                        button.setTextColor(ContextCompat.getColor(this, R.color.key_correct_text))
                    }
                    hurufSalah.contains(char) -> {
                        button.isEnabled = false
                        button.background = ContextCompat.getDrawable(this, R.drawable.bg_key_wrong)
                        button.setTextColor(ContextCompat.getColor(this, R.color.key_wrong_text))
                    }
                    else -> {
                        button.isEnabled = !isGameEnded
                        button.background = ContextCompat.getDrawable(this, R.drawable.bg_key_default)
                        button.setTextColor(ContextCompat.getColor(this, R.color.text_main))
                    }
                }
            }

            if (isGameEnded) {
                etTebak.isEnabled = false
                if (isMenang) {
                    tvHasil.text = getString(R.string.menang_title)
                    tvHasil.setTextColor(ContextCompat.getColor(this, R.color.accent))
                } else {
                    tvHasil.text = getString(R.string.kalah_title)
                    tvHasil.setTextColor(ContextCompat.getColor(this, R.color.danger))
                }
                tvKataAkhir.text = getString(R.string.kata_akhir_format, kataRahasia)
            }
        }
    }

    companion object {
        private const val KEY_KATA_RAHASIA = "key_kata_rahasia"
        private const val KEY_HURUF_BENAR = "key_huruf_benar"
        private const val KEY_HURUF_SALAH = "key_huruf_salah"
        private const val KEY_SKOR = "key_skor"
        private const val KEY_MAKS_NYAWA = "key_maks_nyawa"
        private const val KEY_IS_GAME_ENDED = "key_is_game_ended"
        private const val KEY_IS_MENANG = "key_is_menang"
        private const val KEY_OVERLAY_MULAI_VIS = "key_overlay_mulai_vis"
        private const val KEY_OVERLAY_KESULTANAN_VIS = "key_overlay_kesulitan_vis"
        private const val KEY_OVERLAY_HASIL_VIS = "key_overlay_hasil_vis"
    }
}