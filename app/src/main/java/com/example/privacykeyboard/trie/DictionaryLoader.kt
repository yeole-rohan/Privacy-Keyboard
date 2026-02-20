package com.example.privacykeyboard.trie

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

fun loadDictionaryFromAssets(context: Context): List<String> {
    val wordList = mutableListOf<String>()
    try {
        context.assets.open("dictionary.zip").use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry = zipStream.nextEntry
                Log.i("DictionaryLoader", "Loading dictionary: $entry")
                while (entry != null) {
                    if (!entry.isDirectory) {
                        BufferedReader(InputStreamReader(zipStream)).use { reader ->
                            reader.lineSequence().forEach { line ->
                                wordList.add(line.trim())
                            }
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DictionaryLoader", "Failed to load dictionary: ${e.message}", e)
    }
    return wordList
}
