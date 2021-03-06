package com.theapache64.supergoogle

import com.theapache64.supergoogle.utils.KeyCode
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


const val STYLE_HIGHLIGHT = "color: #fff; background-color: #383838; font-weight: 700;"


@ExperimentalTime
suspend fun main() {
    console.log("Super Google loaded!")
    window.onkeyup = {
        if (it.keyCode == KeyCode.KEY_CODE_F2) {
            if (isReviewsOpen()) {
                val keywordInput = window.prompt("Enter keywords (comma separated)")
                if (!keywordInput.isNullOrBlank()) {
                    // Got valid keyword
                    val keywords = keywordInput
                        .split(",")
                        .map { keyword -> keyword.trim() }

                    val count = window.prompt("How many reviews you want to analyse? (empty to review all)")
                        .let { maxLimitString ->
                            if (maxLimitString.isNullOrBlank()) {
                                null
                            } else {
                                try {
                                    maxLimitString.trim().toInt()
                                } catch (e: NumberFormatException) {
                                    null
                                }
                            }
                        }

                    GlobalScope.launch {
                        filterBy(keywords, count)
                    }
                } else {
                    // empty keyword show all hidden elements
                    showAllHiddenReviews()
                }
            } else {
                window.alert("Uhh ho! We can only filter location reviews")
            }
        }
    }
}

fun showAllHiddenReviews() {
    document.querySelectorAll("div#reviewSort div[data-google-review-count]")
        .asList()
        .forEach {
            for (node in it.childNodes.asList()) {
                node as HTMLDivElement
                node.style.display = "block"
            }
        }
}

val dReviewDialogList by lazy {
    document.querySelector("#gsr div.review-dialog-list") as HTMLDivElement
}

@ExperimentalTime
suspend fun filterBy(keywords: List<String>, maxLimit: Int?) {
    // Showing progress
    showLoading("Loading all reviews ...")

    val totalReviews = getTotalReviews()
    showLoading("Found $totalReviews reviews")

    var loadedReviewCount = getLoadedReviewCount()


    while (totalReviews > loadedReviewCount) {

        // Scroll to bottom
        val tookMs = measureTime {
            dReviewDialogList.scrollTop = dReviewDialogList.scrollHeight.toDouble()
            delay(500)
            loadedReviewCount = getLoadedReviewCount()

            val percentageLoaded = (loadedReviewCount / totalReviews.toFloat()) * 100
            val secondaryPercentage = if (maxLimit != null) {
                val secPerc = (loadedReviewCount / maxLimit.toFloat()) * 100
                "(${secPerc.toInt()}%)"
            } else {
                ""
            }
            showLoading("${percentageLoaded.toInt()}% reviews loaded ... $secondaryPercentage")
        }

        console.log("Took ${tookMs.inMilliseconds}ms -> $totalReviews >= $loadedReviewCount")

        if (maxLimit != null && loadedReviewCount >= maxLimit) {
            break
        }
    }

    dReviewDialogList.scrollTop = dReviewDialogList.scrollHeight.toDouble()
    delay(500)
    // All reviews are loaded
    showLoading("Initializing analysis of ${getLoadedReviewCount()} review(s)...")
    delay(1500)
    onAllReviewsLoaded(keywords)
}

suspend fun onAllReviewsLoaded(keywords: List<String>) {
    var totalReviewsShown = 0

    document.querySelectorAll("div#reviewSort div[data-google-review-count]")
        .asList()
        .forEach {

            for (node in it.childNodes.asList()) {
                node as HTMLDivElement
                val currentReviewSpan = node.querySelector("span[jscontroller]") as HTMLSpanElement
                val currentReview = currentReviewSpan.innerText.trim()

                if (currentReview.isNotBlank()) {
                    val matches = findFrom(currentReview, keywords)
                    if (matches.isNotEmpty()) {
                        // show
                        node.style.display = "block"

                        // color the matched word
                        for (word in matches) {
                            val updatedReview = currentReview.replace(
                                word,
                                "<span style=\"$STYLE_HIGHLIGHT\">$word</span>",
                                true
                            )

                            currentReviewSpan.innerHTML = updatedReview
                        }

                        totalReviewsShown++
                    } else {
                        // hide
                        node.style.display = "none"
                    }
                } else {
                    // Hide that review
                    node.style.display = "none"
                }
            }
        }

    hideLoading()
    delay(200)
    val joinedKeywords = keywords.joinToString(",")

    if (totalReviewsShown > 0) {
        window.alert("Found $totalReviewsShown match(es) for $joinedKeywords")
        dReviewDialogList.scrollTop = 0.toDouble()
    } else {
        window.alert("No match found for $joinedKeywords")
        showAllHiddenReviews()
    }
}

fun hideLoading() {
    getLoadingDiv()?.remove()
}

fun findFrom(review: String, keywords: List<String>): List<String> {
    val matches = mutableListOf<String>()
    for (keyword in keywords) {
        if (review.contains(keyword, true)) {
            matches.add(keyword)
        }
    }
    return matches
}

fun getLoadedReviewCount(): Int {
    return document
        .querySelectorAll("div#reviewSort div[data-google-review-count]")
        .asList()
        .sumBy {
            it as HTMLDivElement
            it.getAttribute("data-google-review-count")?.toInt() ?: 0
        }
}

fun getTotalReviews(): Int {
    return document.querySelector(
        "#gsr div.review-score-container > div:nth-child(1) > div > span"
    )?.let {
        it as HTMLElement
        it.innerText.replace("[^\\d]".toRegex(), "").toInt()
    } ?: 0
}

fun showLoading(message: String) {

    var loadingDiv = getLoadingDiv()
    if (loadingDiv == null) {
        loadingDiv = createLoadingDiv(message)
    }

    // Update message
    val pLoadingMessage = loadingDiv!!.querySelector("p#${ID_LOADING_MESSAGE}") as HTMLParagraphElement
    pLoadingMessage.textContent = message
}

fun getLoadingDiv(): HTMLDivElement? {
    return document.querySelector("div#${ID_LOADING_DIV}") as? HTMLDivElement
}

fun createLoadingDiv(message: String): HTMLDivElement? {
    val dReviewSort = document.querySelector("div#reviewSort") as HTMLDivElement
    val loadingDiv = getLoadingDivHtml(message)
    dReviewSort.innerHTML += loadingDiv
    return getLoadingDiv()
}

const val ID_LOADING_DIV = "dFiltering"
const val ID_LOADING_MESSAGE = "pLoadingMessage"

fun getLoadingDivHtml(message: String) = """
       <div id="$ID_LOADING_DIV" style="
             position: absolute;
             top: 0px;
             left: 0px;
             background-color: #ffffffa8;
             width: 100%;
             height: 100%;
             align-items: center;
             justify-content: center;
             display: flex;
             flex-flow: column;
             z-index: 9999;
         ">
             <img src="https://raw.githubusercontent.com/theapache64/super-google/master/spinner.gif" style="
        ">
             <p id="$ID_LOADING_MESSAGE" style="
            font-size: 27px;
            color: #2b2b2b;
            margin-top: 0px;
        ">$message</p>
         </div>
    """.trimIndent()

fun isReviewsOpen(): Boolean {
    return document.querySelector("div#reviewSort") != null
}
