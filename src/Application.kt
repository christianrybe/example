package com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import com.example.Tables.EXAMPLE_USER

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val t = EXAMPLE_USER
}
