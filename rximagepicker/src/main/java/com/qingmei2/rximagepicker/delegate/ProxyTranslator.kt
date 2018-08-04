package com.qingmei2.rximagepicker.delegate

import android.app.Activity
import android.content.Context
import android.support.v4.app.FragmentActivity

import com.qingmei2.rximagepicker.entity.sources.Camera
import com.qingmei2.rximagepicker.entity.sources.Gallery
import com.qingmei2.rximagepicker.entity.sources.SourcesFrom
import com.qingmei2.rximagepicker.providers.ConfigProvider
import com.qingmei2.rximagepicker.core.ImagePickerProjector
import com.qingmei2.rximagepicker.providers.RuntimeProvider
import com.qingmei2.rximagepicker.ui.ActivityPickerViewController
import com.qingmei2.rximagepicker.ui.ICustomPickerConfiguration
import com.qingmei2.rximagepicker.ui.ICustomPickerView
import java.lang.NullPointerException

import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * [ProxyTranslator] is used to handle the annotation configuration above the method in the user's
 * custom interface, after completed parsing configuration, the configrations will be stored in an object
 * [ConfigProvider].
 */
class ProxyTranslator {

    fun processMethodForStaticConfig(method: Method): ConfigProvider {
        val sourcesFrom = streamSourcesFrom(method)
        val asFragment = asFragment(method, sourcesFrom)
        return ConfigProvider(
                getComponentClass(method, sourcesFrom),
                asFragment,
                sourcesFrom,
                if (asFragment) containerViewId(method, sourcesFrom) else -1)
    }

    fun processMethodForRuntimeParams(method: Method, args: Array<Any>?): RuntimeProvider {
        val fragmentActivity = getObjectFromMethodParam(method, Context::class.java, args).let {
            transformContextToFragmentActivity(it)
        }
        val runtimeConfiguration = getObjectFromMethodParam(method, ICustomPickerConfiguration::class.java, args)

        val sourcesFrom = streamSourcesFrom(method)
        val componentClass = getComponentClass(method, sourcesFrom)
        val asFragment = asFragment(method, sourcesFrom)

        return if (asFragment &&
                ICustomPickerView::class.java.isAssignableFrom(componentClass.java)) {
            RuntimeProvider(fragmentActivity, componentClass.java.newInstance() as ICustomPickerView, runtimeConfiguration)
        } else if (!asFragment &&
                Activity::class.java.isAssignableFrom(componentClass.java)) {
            RuntimeProvider(fragmentActivity, ActivityPickerViewController.instance, runtimeConfiguration)
        } else
            throw IllegalArgumentException("Configration Conflict! The ui component as Activity: ${!asFragment}," +
                    " the Class type is: ${componentClass.simpleName}")
    }

    private fun asFragment(method: Method, sourcesFrom: SourcesFrom): Boolean {
        return when (sourcesFrom) {
            SourcesFrom.CAMERA -> {
                method.getAnnotation(Camera::class.java).openAsFragment
            }
            SourcesFrom.GALLERY -> {
                method.getAnnotation(Gallery::class.java).openAsFragment
            }
        }
    }

    private fun getComponentClass(method: Method, sourcesFrom: SourcesFrom): KClass<*> {
        return when (sourcesFrom) {
            SourcesFrom.CAMERA -> {
                method.getAnnotation(Camera::class.java).componentClazz
            }
            SourcesFrom.GALLERY -> {
                method.getAnnotation(Gallery::class.java).componentClazz
            }
        }
    }

    /**
     * Handle the annotation configuration of image source. This configuration is user-defined annotation
     * above the method [Camera] or [Gallery].
     *
     *
     * By default, if not image source annotation was configured, you will get an [IllegalArgumentException].
     *
     * @return [SourcesFrom.CAMERA] or [SourcesFrom.GALLERY]
     */
    private fun streamSourcesFrom(method: Method): SourcesFrom {
        val camera = method.getAnnotation(Camera::class.java) != null
        val gallery = method.getAnnotation(Gallery::class.java) != null
        return if (camera && !gallery) {
            SourcesFrom.CAMERA
        } else if (gallery && !camera) {
            SourcesFrom.GALLERY
        } else if (!camera) {
            throw IllegalArgumentException("Did you forget to add the @Galley or the @Camera annotation?")
        } else {
            throw IllegalArgumentException("You should not add two conflicting annotation to this method: @Galley and @Camera.")
        }
    }

    private fun containerViewId(method: Method, sourcesFrom: SourcesFrom): Int {
        return when (sourcesFrom) {
            SourcesFrom.CAMERA -> {
                method.getAnnotation(Camera::class.java).containerViewId
            }
            SourcesFrom.GALLERY -> {
                method.getAnnotation(Gallery::class.java).containerViewId
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getObjectFromMethodParam(method: Method,
                                             expectedClass: Class<T>,
                                             objectsMethod: Array<Any>?): T {
        var countSameObjectsType = 0
        var expectedObject: T? = null

        if (objectsMethod == null)
            throw NullPointerException(method.name
                    + " requires the Context as argument at least.")

        for (objectParam in objectsMethod) {
            if (expectedClass.isAssignableFrom(objectParam.javaClass)) {
                expectedObject = objectParam as T
                countSameObjectsType++
            }
        }

        if (countSameObjectsType > 1) {
            throw IllegalArgumentException(method.name
                    + " requires just one instance of type: ${expectedClass.simpleName}, but $countSameObjectsType.")
        }
        return expectedObject ?: throw NullPointerException(method.name
                + " requires just one instance of type: ${expectedClass.simpleName}, but none.")
    }

    private fun transformContextToFragmentActivity(context: Context): FragmentActivity {
        return context as? FragmentActivity
                ?: throw IllegalArgumentException("the context should be FragmentActivity")
    }
}
