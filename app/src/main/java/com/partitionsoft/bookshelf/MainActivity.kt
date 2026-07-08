package com.partitionsoft.bookshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.partitionsoft.bookshelf.domain.repository.BookRepository
import com.partitionsoft.bookshelf.domain.subscription.usecase.RefreshSubscriptionPlansUseCase
import com.partitionsoft.bookshelf.ui.BooksApp
import com.partitionsoft.bookshelf.ui.theme.BookShelfTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bookRepository: BookRepository

    @Inject
    lateinit var refreshSubscriptionPlans: RefreshSubscriptionPlansUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        var keepSplashOnScreen = true
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            withTimeoutOrNull(HOME_PRELOAD_TIMEOUT_MS) {
                bookRepository.preloadHomeFeed()
            }
            keepSplashOnScreen = false
        }

        setContent {
            BookShelfTheme {
                BooksApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSubscriptionPlans()
    }

    private companion object {
        private const val HOME_PRELOAD_TIMEOUT_MS = 2_500L
    }
}
