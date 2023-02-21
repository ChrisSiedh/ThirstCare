package com.example.thirstcure

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.thirstcure.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity(){

    private val mFirebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private lateinit var binding: ActivityLoginBinding

    private var email = ""
    private var password = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Button für den Login
        binding.btnLogin.setOnClickListener{
            email = binding.etEmail.text.toString()
            password = binding.etPassword.text.toString()

            if (validateForm(email,password)){
                logIn(email, password)
            }
        }

        //Button für die Registrieren
        binding.btnRegister.setOnClickListener{
            email = binding.etEmail.text.toString()
            password = binding.etPassword.text.toString()

            if (validateForm(email,password)){
                register(email, password)
            }
        }

        //Firebase Authentifizierung
        mAuthListener = FirebaseAuth.AuthStateListener {
            val user = mFirebaseAuth.currentUser

            //E-Mail Verifikation
            user?.sendEmailVerification()?.addOnCompleteListener {
                if (it.isSuccessful) {
                    FirebaseAuth.getInstance().signOut()
                    toast(getString(R.string.msg_verify))
                } else {
                    toast(it.exception!!.message.toString())
                }
            }
        }
    }


    //Menü
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    //Menüitem auswählen
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         when (item.itemId) {
            R.id.item_reset -> {
                //Passwort zurücksetzten
                sendResetPw()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    //Funktion zur Überprüfung der Eingaben
    private fun validateForm(email: String, password: String): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.et_email_error)
            valid = false
        }
        if ((password.isEmpty() || password.length <=6)) {
            binding.etPassword.error = getString(R.string.et_passwort_error)
            valid = false
        }
        return valid
    }


    //Funktion zum einloggen
    private fun logIn(email: String, password: String) {
        mFirebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (mFirebaseAuth.currentUser!!.isEmailVerified) {
                        toast(getString(R.string.msg_login_succsess))
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        //Starte die MainActivity wenn der Login erfolgreich war
                        startActivity(intent)
                    } else {
                        toast(getString(R.string.msg_reminder_verify))
                    }
                } else {
                    toast(it.exception!!.message.toString())
                }
            }
    }


    //Funktion zum registrieren
    private fun register(email: String, password: String) {
        mFirebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mAuthListener.onAuthStateChanged(mFirebaseAuth)
                } else {
                    toast(task.exception!!.message.toString())
                }
            }
    }


    private fun sendResetPw() {
        //AlertDialog erstellen
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout:View = inflater.inflate(R.layout.edit_text_layout, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.et_pwreset)
        editText.hint = getString(R.string.et_email_hint)

        with(builder){
            setTitle(R.string.dialog_pwreset_title)
            setPositiveButton(R.string.dialog_pos){ _, _ ->
                //Passwort zurücksetzten wenn das Textfeld nicht leer ist
                val mail = editText.text.toString().trim()
                if (mail.isEmpty()) {
                    toast(getString(R.string.msg_pwreset_fill_out))

                } else {
                    sendMail(mail)
                }
            }
            setNegativeButton(R.string.dialog_neg){ _, _ ->

            }
            setView(dialogLayout)
            show()
        }
    }

    //Funktion zum senden der E-Mail für die Zurücksetztung
    private fun sendMail(mail: String) {
        mFirebaseAuth.sendPasswordResetEmail(mail)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    toast(getString(R.string.msg_pwreset_success))
                } else {
                    toast(it.exception!!.message.toString())
                }
            }
    }


    //Funktion zum zeigen von Toasts
    private fun toast(msg: String) {
        Toast.makeText(applicationContext, msg , Toast.LENGTH_LONG).show()
    }
}