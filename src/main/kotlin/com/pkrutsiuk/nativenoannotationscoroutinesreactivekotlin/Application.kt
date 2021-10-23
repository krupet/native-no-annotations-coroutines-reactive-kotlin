package com.pkrutsiuk.nativenoannotationscoroutinesreactivekotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NativeNoAnnotationsCoroutinesReactiveKotlinApplication

fun main(args: Array<String>) {
	runApplication<NativeNoAnnotationsCoroutinesReactiveKotlinApplication>(*args)
}
