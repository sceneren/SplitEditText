package wiki.scene.split

import androidx.annotation.IntDef

/**
 * 边框风格
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(BorderStyle.BOX, BorderStyle.LINE)
annotation class BorderStyle {
    companion object {
        //框
        const val BOX = 0

        //线
        const val LINE = 1
    }
}