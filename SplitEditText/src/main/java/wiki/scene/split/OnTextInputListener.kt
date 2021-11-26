package wiki.scene.split

/**
 * 文本输入监听
 */
interface OnTextInputListener {
    /**
     * Text改变监听
     * @param text
     * @param length
     */
    fun onTextInputChanged(text: String, length: Int)

    /**
     * Text输入完成
     * @param text
     */
    fun onTextInputCompleted(text: String)
}