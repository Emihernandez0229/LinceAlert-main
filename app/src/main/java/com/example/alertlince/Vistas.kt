package com.example.alertlince

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class Vistas : AppCompatActivity() {

    private val firstFragment = FirstFragment()
    private val secondFragment = SecondFragment()
    private val thirdFragment = ThirdFragment()
    private val fourthFragment = FourthFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homescroll)

        val navigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Cargar solo si es la primera vez
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_container, firstFragment, "first")
                .add(R.id.frame_container, secondFragment, "second").hide(secondFragment)
                .add(R.id.frame_container, thirdFragment, "third").hide(thirdFragment)
                .add(R.id.frame_container, fourthFragment, "fourth").hide(fourthFragment)
                .commit()
        }

        navigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.firstFragment -> switchFragment(firstFragment)
                R.id.secondFragment -> switchFragment(secondFragment)
                R.id.thirdFragment -> switchFragment(thirdFragment)
                R.id.fourthFragment -> switchFragment(fourthFragment)
            }
            true
        }
    }

    private fun switchFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        // Ocultar todos los fragmentos
        supportFragmentManager.fragments.forEach {
            transaction.hide(it)
        }

        // Mostrar el fragmento seleccionado
        transaction.show(fragment)
        transaction.commit()
    }
}
