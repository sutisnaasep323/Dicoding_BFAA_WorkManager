package com.example.myworkmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.text.DecimalFormat

class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        private val TAG = MyWorker::class.java.simpleName
        const val APP_ID = "YOUR_KEY"
        const val EXTRA_CITY = "city"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "dicoding channel"
    }

    private var resultStatus: Result? = null

    /*
    Metode doWork adalah metode yang akan dipanggil ketika WorkManager berjalan. Kode di dalamnya
    akan dijalankan di background thread secara otomatis. Metode ini juga mengembalikan nilai berupa
    Result yang berfungsi untuk mengetahui status WorkManager yang berjalan. seperti Result.success(),
    Result.failure(), dan Result.retry()
     */
    override fun doWork(): Result {
        val dataCity = inputData.getString(EXTRA_CITY) // getData dari key EXTRA_CITY (tipe data dan key yang dimasukkan harus sama persis)
        return getCurrentWeather(dataCity)
    }

    // LoopJ adalah salah library untuk menangani koneksi ke API.  Selain LoopJ ada Retrofit, Volley, FastAndroidNetworking dsb
    private fun getCurrentWeather(city: String?): Result {
        Log.d(TAG, "getCurrentWeather: Mulai.....")
        Looper.prepare()
        val client = SyncHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$APP_ID"
        Log.d(TAG, "getCurrentWeather: $url")
        client.post(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header?>?,
                responseBody: ByteArray
            ) {
                val result = String(responseBody)
                Log.d(TAG, result)
                try {
                    // diganti dengan Moshi
//                    val responseObject = JSONObject(result)
//                    val currentWeather: String =
//                        responseObject.getJSONArray("weather").getJSONObject(0).getString("main")
//                    val description: String =
//                        responseObject.getJSONArray("weather").getJSONObject(0)
//                            .getString("description")
//                    val tempInKelvin = responseObject.getJSONObject("main").getDouble("temp")

                    val moshi = Moshi.Builder()
                        .addLast(KotlinJsonAdapterFactory())
                        .build()
                    /*
                    Apabila response JSON dimulai dengan kurung siku / [ ] yang berarti ia berupa
                    JSON Array. Maka pada moshi.adapter argumen yang dimasukkan adalah Array<Response>
                     */
                    val jsonAdapter = moshi.adapter(Response::class.java) //pastikan kita memberikan class yang sesuai dengan hasil JSON yang diberikan
                    val response = jsonAdapter.fromJson(result)

                    response?.let {
                        val currentWeather = it.weatherList[0].main
                        val description = it.weatherList[0].description
                        val tempInKelvin = it.main.temperature //batas moshi

                        val tempInCelsius = tempInKelvin - 273
                        val temperature: String = DecimalFormat("##.##").format(tempInCelsius)
                        val title = "Current Weather in $city"
                        val message = "$currentWeather, $description with $temperature celsius"
                        showNotification(title, message)
                    }
                    Log.d(TAG, "onSuccess: Selesai.....")
                    resultStatus =
                        Result.success() // Dengan ini, kita bisa menentukan kembalian dari proses ini apakah berhasil atau gagal dengan menggunakan kode tersebut
                } catch (e: Exception) {
                    showNotification("Get Current Weather Not Success", e.message)
                    Log.d(TAG, "onSuccess: Gagal.....")
                    resultStatus = Result.failure() // Dengan ini, kita bisa menentukan kembalian dari proses ini apakah berhasil atau gagal dengan menggunakan kode tersebut
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header?>?,
                responseBody: ByteArray?,
                error: Throwable
            ) {
                Log.d(TAG, "onFailure: Gagal.....")
                // ketika proses gagal, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                showNotification("Get Current Weather Failed", error.message)
                resultStatus = Result.failure() // Dengan begitu, kita bisa menentukan kembalian dari proses ini apakah berhasil atau gagal dengan menggunakan kode tersebut
            }
        })
        return resultStatus as Result
    }
    private fun showNotification(title: String, description: String?) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }
}
