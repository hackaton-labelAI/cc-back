package cc.services.analyzer

import java.util.concurrent.Callable

interface SimpleCache<K, V> {
    fun get(key: K, compute: Callable<V>): V
    fun put(key: K, value: V)
}
