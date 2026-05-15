package com.example.niord.api



fun cpfPlainToFormatted(cpf: String): String{
    //Formatting for API
    //In: 11122233344; out: 111.222.333-44
    val cpfString = StringBuilder(cpf)
    cpfString.insert(3, '.')
    cpfString.insert(7, '.')
    cpfString.insert(11, '-')
    return cpfString.toString()
}