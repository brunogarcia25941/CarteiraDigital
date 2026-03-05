package com.brunogarcia.carteira

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform