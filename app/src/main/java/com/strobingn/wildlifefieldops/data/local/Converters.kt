package com.strobingn.wildlife.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strobingn.wildlife.data.model.InvoiceLineItem

/**
 * Room converters for collection types that cannot be stored directly.
 *
 * Room 2.6+ supports enum fields natively, so enum converters are intentionally
 * omitted. Keeping enum imports here caused KSP to fail whenever any model enum
 * was moved, renamed, or temporarily malformed.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value.orEmpty())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(value, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromInvoiceLineItemList(value: List<InvoiceLineItem>?): String =
        gson.toJson(value.orEmpty())

    @TypeConverter
    fun toInvoiceLineItemList(value: String?): List<InvoiceLineItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<InvoiceLineItem>>() {}.type
            gson.fromJson<List<InvoiceLineItem>>(value, type).orEmpty()
        }.getOrDefault(emptyList())
    }
}
