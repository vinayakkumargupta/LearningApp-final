package com.univ.learningapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ThreeButtonsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_three_buttons)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.widget.Button>(R.id.buttonRed).setOnClickListener {
            Toast.makeText(this, "Red pressed", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.widget.Button>(R.id.buttonGreen).setOnClickListener {
            Toast.makeText(this, "Green pressed", Toast.LENGTH_SHORT).show()
        }
        findViewById<android.widget.Button>(R.id.buttonBlue).setOnClickListener {
            Toast.makeText(this, "Blue pressed", Toast.LENGTH_SHORT).show()
        }
    }
}
