package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.pages.ReaderPage
import org.wordpress.android.support.BaseTest
import org.wordpress.android.support.ComposeEspressoLink

@HiltAndroidTest
class ReaderTests : BaseTest() {
    @Before
    fun setUp() {
        ComposeEspressoLink().unregister()
        logoutIfNecessary()
        wpLogin()
        ReaderPage().go()
    }

    @Test
    fun e2eNavigateThroughPosts() {
        ReaderPage()
            .tapFollowingTab()
            .openBlogOrPost(TITLE_COACHING_POST)
            .verifyPostDisplayed(TITLE_COACHING_POST)
            .slideToPreviousPost()
            .verifyPostDisplayed(TITLE_COMPETITION_POST)
            .slideToNextPost()
            .verifyPostDisplayed(TITLE_COACHING_POST)
            .goBackToReader()
    }

    @Test
    fun e2eLikePost() {
        ReaderPage()
            .tapFollowingTab()
            .openBlogOrPost(TITLE_COACHING_POST)
            .likePost()
            .verifyPostLiked()
            .unlikePost()
            .verifyPostNotLiked()
            .goBackToReader()
    }

    @Test
    fun e2eBookmarkPost() {
        ReaderPage()
            .tapFollowingTab()
            .openBlogOrPost(TITLE_LONGREADS_BLOG)
            .openMoreMenu()
            .bookmarkPost()
            .verifyPostBookmarked()
            .openMoreMenu()
            .removeBookmarkPost()
            .openMoreMenu()
            .verifyPostNotBookmarked()
            .goBackToReader()
    }

    companion object {
        private const val TITLE_LONGREADS_BLOG = "Longreads"
        private const val TITLE_COACHING_POST = "Let's check out the coaching team!"
        private const val TITLE_COMPETITION_POST = "Let's focus on the competition."
    }
}
