package com.example.myworkmanager

import com.squareup.moshi.Json

/*
Kuncinya adalah apabila ia berupa JSON Array (ditandai dengan kurung siku/[ ]), variabelnya berupa List.
Jika ia berupa JSON Object (ditandai dengan kurung kurawal/{ }), bentuknya adalah Object dari Class.

Pastikan juga bahwa nama variabel yang digunakan sama dengan key yang ada pada API. Apabila Anda ingin menggunakan nama variabel yang berbeda, Anda bisa menggunakan anotasi @Json seperti contoh di atas
 */

data class Response(
    val id: Int,
    val name: String,
    @Json(name = "weather")
    val weatherList: List<Weather>,
    val main: Main,
)

data class Weather(
    val main: String,
    val description: String
)

data class Main(
    @Json(name = "temp")
    val temperature: Double
)
