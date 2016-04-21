package name.caiyao.cracktencent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.Toast
import name.caiyao.cracktencent.utils.Utils

class MainActivity : AppCompatActivity() {

    final val REQUEST_FOR_READ_PHONE_STATE = 0
    final val REQUEST_FOR_WRITE_EXTERNAL_STORAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (Build.VERSION.SDK_INT >= 23) {
            val checkReadPhoneStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            if (checkReadPhoneStatePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_FOR_READ_PHONE_STATE)
            } else {
                Thread() {
                    run {
                        Utils.init(this)
                    }
                }.start()
            }
            val checkWriteExtenelPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (checkWriteExtenelPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_FOR_WRITE_EXTERNAL_STORAGE)
            } else {
                Thread() {
                    run {
                        Utils.copyDatabase(this)
                    }
                }.start()
            }
        } else {
            Thread() {
                run {
                    Utils.init(this)
                    Utils.copyDatabase(this)
                }
            }.start()

        }

        val fab = findViewById(R.id.fab) as FloatingActionButton
        fab.setOnClickListener { view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_FOR_READ_PHONE_STATE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Thread() {
                    run {
                        Utils.init(this)
                    }
                }.start()
            } else {
                Toast.makeText(this, "读取IMEI权限被禁止！", Toast.LENGTH_SHORT).show()
            }
            REQUEST_FOR_WRITE_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Thread() {
                    run {
                        Utils.copyDatabase(this)
                    }
                }.start()
            } else {
                Toast.makeText(this, "写文件权限被禁止！", Toast.LENGTH_SHORT).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
