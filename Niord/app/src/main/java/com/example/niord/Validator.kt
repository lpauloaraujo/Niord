package com.example.niord

import android.util.Patterns

sealed class PasswordResult {
    object Valid : PasswordResult()
    data class Invalid(val errors: List<String>) : PasswordResult()
}

fun validatePassword(password: String): PasswordResult {
    val errors = mutableListOf<String>()

    if (password.length < 8) errors.add("Ao menos 8 caracteres")
    if (password.none { it.isUpperCase() }) errors.add("Uma letra maiúscula")
    if (password.none { it.isLowerCase() }) errors.add("Uma letra minúscula")
    if (password.none { it.isDigit() }) errors.add("Um número")
    if (password.none { !it.isLetterOrDigit() }) errors.add("Um caractere especial(#/%$@)")
    println(errors.size)
    if (errors.isEmpty()){
        return PasswordResult.Valid
    }else {
        errors.add(0, "É necessário:")
        return PasswordResult.Invalid(errors)
    }
}

fun validatePone(phone: String): Boolean{
    return phone.isNotEmpty() && Patterns.PHONE.matcher(phone).matches()
}
fun validateEmail(email: String): Boolean {
    return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

fun validatePlate(plate: String): Boolean{

    return verifyPlate(plate)|| verifyOldPlate(plate)
}

fun verifyPlate(plate: String): Boolean{
    if(plate.length != 7){
        return false
    }
    //ABC 1 D 23
    //Check letter
    for(i in 0..<3){
        if(!plate[i].isLetter()) return false
    }
    if(!plate[4].isLetter()) return false

    //Check digit
    for(i in 5..6){
        if(!plate[i].isDigit()) return false
    }
    if(!plate[3].isDigit()) return false

    return true
}

fun verifyOldPlate(unformattedPlate: String): Boolean{
    if(unformattedPlate.length != 8){
        return false
    }
    val splitted = unformattedPlate.split("-")
    val plate = splitted.joinToString("")
    //ABC 1234
    for(i in 0..<3){
        if(!plate[i].isLetter()) return false
    }
    for(i in 3..<7){
        if(!plate[i].isDigit()) return false
    }

    return true
}

fun validateCpf(cpf: String): Boolean{
    if(cpf.length != 11) return false

    var isRepeating = true
    for(i in 1..<cpf.length){
        if (cpf[i] != cpf[i-1]){
            isRepeating = false
            break
        }
    }
    if(isRepeating){
        return false
    }

    var s = 0
    var c = 10
    for(i in 0..<cpf.length-2){
        s += cpf[i].digitToInt() * c
        c -= 1
    }
    s *= 10
    var firstDigit= s%11
    if (firstDigit== 10){
        firstDigit = 0
    }

    if(cpf[cpf.length-2] != firstDigit.digitToChar()){
        return false
    }

    s = 0
    c = 11
    for(i in 0..<cpf.length-1){
        s += cpf[i].digitToInt() * c
        c -= 1
    }
    s *= 10
    var secondDigit = s%11
    if(secondDigit == 10){
        secondDigit = 0
    }

    if(cpf[cpf.length-1] != secondDigit.digitToChar()){
        return false
    }

    return true
}

