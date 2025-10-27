package com.dd3boh.outertune.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Dispatcher dédié pour les opérations liées au lecteur audio
 */
val playerCoroutine: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()