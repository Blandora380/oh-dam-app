package org.myapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val textWelcome = findViewById<TextView>(R.id.textWelcome)
        val btnBack = findViewById<Button>(R.id.btnBack)

        // Ambil data yang dikirim dari MainActivity
        val namaKiriman = intent.getStringExtra("nama_user")

        if (!namaKiriman.isNullOrBlank()) {
            textWelcome.text = "Selamat datang, $namaKiriman!"
        }

        btnBack.setOnClickListener {
            finish() // Tutup activity ini, balik ke MainActivity
        }
    }
}