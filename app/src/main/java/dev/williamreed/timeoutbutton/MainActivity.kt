package dev.williamreed.timeoutbutton

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var timeoutDisposable: Disposable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeout.start()
        timeoutDisposable = timeout.timeoutOccurred
            .subscribe { Toast.makeText(this, "Timeout occurred", Toast.LENGTH_LONG).show() }

        reset.setOnClickListener { timeout.start() }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutDisposable?.dispose()
    }
}
