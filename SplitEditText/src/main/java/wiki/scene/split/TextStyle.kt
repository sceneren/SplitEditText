package wiki.scene.split

import androidx.annotation.IntDef

/**
 * 文本风格
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(TextStyle.PLAIN_TEXT, TextStyle.CIPHER_TEXT)
annotation class TextStyle {
    companion object {
        //明文
        const val PLAIN_TEXT = 0

        //秘文
        const val CIPHER_TEXT = 1
    }
}