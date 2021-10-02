package com.example.myworkmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.work.*
import com.example.myworkmanager.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var workManager: WorkManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var periodicWorkRequest: PeriodicWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workManager = WorkManager.getInstance(this)
        binding.btnOneTimeTask.setOnClickListener(this)
        binding.btnPeriodicTask.setOnClickListener(this)
        binding.btnCancelTask.setOnClickListener(this)

    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.btnOneTimeTask -> StartOneTimeTask()   // untuk menjalankan task sekali saja
            R.id.btnPeriodicTask -> StartPeriodicTask() // untuk menjalankan task secara periodic
            R.id.btnCancelTask -> CancelPeriodicTask()
        }
    }

    private fun StartPeriodicTask() {
        binding.textStatus.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Kode di atas digunakan untuk menjalankan PeriodicWorkRequest dengan interval 15 menit (minimal).
        // kita bisa mengaturnya dengan mengganti parameter kedua dan ketiga
        periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        workManager.enqueue(periodicWorkRequest)
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity, { workInfo ->
                val status = workInfo.state.name
                binding.textStatus.append("\n" + status)
                binding.btnCancelTask.isEnabled = false
                if (workInfo.state == WorkInfo.State.ENQUEUED) {
                    binding.btnCancelTask.isEnabled = true
                }
            })
    }

    /*
    Kode di atas digunakan untuk membatalkan task berdasarkan id request. Selain menggunakan id,
    Anda juga bisa menambahkan tag pada request. Kelebihan dari penggunaan tag yaitu Anda bisa
    membatalkan task lebih dari satu task sekaligus
     */
    private fun CancelPeriodicTask() {
        workManager.cancelWorkById(periodicWorkRequest.id)
    }

    private fun StartOneTimeTask() {
        binding.textStatus.text = (getString(R.string.status))

        /*
        Fungsi di bawah digunakan untuk membuat one-time request. Saat membuat request, kita bisa
        menambahkan data untuk dikirimkan dengan membuat object Data yang berisi data key-value,
        key yang dipakai di sini yaitu MyWorker.EXTRA_CITY. Setelah itu dikirimkan melalui setInputData
         */

        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding.editCity.text.toString())
            .build()

        // mengatur task ketika koneksi internet terhubung
        // Constraint digunakan untuk memberikan syarat kapan task ini dieksekusi
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(data) // send data
            .setConstraints(constraints)
            .build()

        workManager.enqueue(oneTimeWorkRequest)

        //WorkInfo digunakan untuk mengetahui status task yang dieksekusi
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this@MainActivity, { workInfo ->
                val status = workInfo.state.name
                binding.textStatus.append("\n" + status)
            })
        /*
        Anda dapat membaca status secara live dengan menggunakan getWorkInfoByIdLiveData. kita juga
        bisa memberikan aksi pada state tertentu dengan mengambil data state dan membandingkannya
        dengan konstanta yang bisa didapat di WorkInfo.State. Misalnya, pada kode di atas kita
        mengatur tombol Cancel task aktif jika task dalam state ENQUEUED
         */
    }


}