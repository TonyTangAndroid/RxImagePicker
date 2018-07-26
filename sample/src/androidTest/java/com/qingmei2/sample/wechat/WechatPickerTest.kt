package com.qingmei2.sample.wechat

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.MediaStore
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents.intending
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.matcher.IntentMatchers.*
import android.support.test.espresso.intent.rule.IntentsTestRule
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.filters.LargeTest
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import com.qingmei2.rximagepicker_extension.ui.BasePreviewActivity
import com.qingmei2.sample.R
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class WechatPickerTest {

    private val successActivityResult: Instrumentation.ActivityResult =
            with(Intent()) {
                putExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE, EXTRA_BUNDLE)
                putExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, EXTRA_RESULT_APPLY)

                Instrumentation.ActivityResult(Activity.RESULT_OK, this)
            }

    @Rule
    @JvmField
    var systemActivityTestRule = IntentsTestRule<WechatActivity>(WechatActivity::class.java)
    @Rule
    @JvmField
    var grantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Test
    fun testPickCamera() {

        intending(hasAction(MediaStore.ACTION_IMAGE_CAPTURE)).respondWith(successActivityResult)

        onView(withId(R.id.fab_pick_camera)).perform(click())

        onView(withId(R.id.iv_picked_image)).check(matches(isDisplayed()))
    }

    @Test
    fun testPickGallery() {
        intending(allOf(
                toPackage("com.qingmei2.rximagepicker_extension_wechat"),
                hasComponent(".ui.WechatImagePickerActivity")
        )).respondWith(successActivityResult)

        onView(withId(R.id.iv_picked_image)).check(matches(isDisplayed()))

        onView(withId(R.id.fab_pick_gallery)).perform(click())

        onView(withId(R.id.iv_picked_image)).check(ViewAssertions.doesNotExist())
    }

    companion object Mock {
        private const val EXTRA_BUNDLE = "123"
        private const val EXTRA_RESULT_APPLY = "456"
    }
}