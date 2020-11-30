package com.theapache64.supergoogle

import com.theapache64.supergoogle.utils.KeyCode
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement

var prevUrl = window.location.toString()

fun main() {
    console.log("Super Google loaded!")

    window.onkeyup = {
        if (it.keyCode == KeyCode.KEY_CODE_F2) {
            if (isReviewsOpen()) {
                val keywordInput = window.prompt("Enter keywords (comma separated)")
                if (!keywordInput.isNullOrBlank()) {
                    // Got valid keyword
                    val keywords = keywordInput
                        .toLowerCase()
                        .split(",")
                        .map { keyword -> keyword.trim() }

                    filterBy(keywords)
                }
            } else {
                window.alert("Uhh ho! We can only filter location reviews")
            }
        }
    }
}

fun filterBy(keywords: List<String>) {
    // Showing progress
    showLoading("Filtering")
}

fun showLoading(message: String) {
    val dReviewSort = document.querySelector("div#reviewSort") as HTMLDivElement
    val loadingDiv = """
        <div class="dFiltering">
            <img src="spinner.gif"/>
            <p id="pFilteringText">
                $message
            </p>
        </div>
    """.trimIndent()

}

fun isReviewsOpen(): Boolean {
    return document.querySelector("div#reviewSort") != null
}
