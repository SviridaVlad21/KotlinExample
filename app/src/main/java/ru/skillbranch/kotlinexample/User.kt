package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName : String,
    private val lastName : String?,
    email : String? = null,
    rawPhone : String? = null,
    meta : Map<String, Any>? = null
) {
    val userInfo : String
    private val fullName : String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials : String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone : String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }
    private var _login : String? = null
    internal var login : String
        set(value) {
            _login = value?.toLowerCase()
        }
        get() = _login!!

    private lateinit var salt : String
    private lateinit var passwordHash : String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode : String? = null

    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")){
        println("Secondary mail constructor")
        salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        passwordHash = encrypt(password)
    }

    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ): this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")){
        println("Secondary phone constructor")
        salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(rawPhone, code)
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String?,
        saltHash: String
    ): this(firstName, lastName, email = email, rawPhone = phone, meta = mapOf("src" to "csv")){
        salt = saltHash.split(":")[0].replace("[", "")
        passwordHash = saltHash.split(":")[1]
    }


    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) {"First name must be not blank"}
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) {"Email or phone must be not blank"}

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass : String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass : String, newPass : String){
        if(checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password doesn't match the current password")
    }


    private fun encrypt(password: String): String = salt.plus(password).md5()

    private fun String.md5() : String{
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString()
        return hexString.padStart(32, '0')
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return  StringBuilder().apply {
            repeat(6){
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String, code: String) {
        println("........sending access code: $code on $phone")
    }

    fun generateNewAuthCode(){
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
    }

    companion object Factory{
        fun makeUser(
            fullName : String,
            email : String? = null,
            password : String? = null,
            phone : String? = null
        ) : User{
            val (firstName, lastName) = fullName.fullNameToPair()

            return when{
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must not null or blank")
            }
        }

        fun importUser(
            fullName : String,
            email: String?,
            saltHash: String,
            phone: String?
        ) : User{
            val (firstName, lastName) = fullName.fullNameToPair()

            return when{
                !phone.isNullOrBlank() -> User(firstName, lastName, null, phone, saltHash)
                !email.isNullOrBlank() -> User(firstName, lastName, email, null, saltHash)
                else -> throw IllegalArgumentException("Email or phone must not null or blank")
            }
        }

        private fun String.fullNameToPair() : Pair<String, String?>{
            return this.split(" ")
                .filter { it.isNotBlank() }
                .run {
                    when(size){
                        1 -> first() to null
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("Fullname must contain only first name and last name, current split result ${this@fullNameToPair}")
                    }
                }
        }
    }
}