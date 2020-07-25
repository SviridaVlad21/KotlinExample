package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }

    fun registerUser(
        fullName : String,
        email : String,
        password : String
    ) : User{
        if(map.contains(email.toLowerCase())) throw IllegalArgumentException("A user with this email already exists")
        else return User.makeUser(fullName,email=email, password = password)
            .also { user -> map[user.login] = user }
    }

    fun loginUser(login : String, password: String) : String?{
        return map[login.normalizeLogin()]?.run {
            if(checkPassword(password)) this.userInfo
            else null
        }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String) : User {
        if(map.contains(rawPhone.normalizePhone())) throw  IllegalArgumentException("A user with this phone already exists")

        if(!rawPhone.validatePhone()){
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }

        return User.makeUser(fullName,null,null, phone = rawPhone).also { map[it.login] = it }
    }

    fun requestAccessCode(login: String) : Unit{
        map[login.normalizeLogin()]?.generateNewAuthCode()
    }



    private fun String.normalizePhone() = replace("[^+\\d]".toRegex(), "")

    private fun String.normalizeLogin() = if(contains("@")) trim() else normalizePhone()

    private fun String.validatePhone() = normalizePhone().matches("[+]\\d{11}".toRegex())
}