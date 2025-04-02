// a che serviva?
package com.gentestrana.utils

import androidx.compose.runtime.*
import java.util.Locale

//@Composable
//fun rememberTranslation(text: String): String {
//    var translatedText by remember { mutableStateOf(text) }
//    val deviceLanguage = Locale.getDefault().language
//    LaunchedEffect(text, deviceLanguage) {
//        if (deviceLanguage != "en") {
//            TranslationHelper.translateTextWithDetection(
//                text = text,
//                targetLanguageCode = deviceLanguage,
//                onSuccess = { translatedText = it },
//                onFailure = { translatedText = text }
//            )
//        } else {
//            translatedText = text
//        }
//    }
//    return translatedText
//}
