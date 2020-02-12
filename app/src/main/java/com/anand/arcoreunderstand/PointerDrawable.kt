package com.anand.arcoreunderstand

import android.graphics.*
import android.graphics.drawable.Drawable

class PointerDrawable: Drawable() {

    private val paint: Paint = Paint()
    private var enabled = false

    override fun draw(canvas: Canvas) {
        var cx = canvas.width/2
        var cy = canvas.height/2

        if(enabled){
            paint.color = Color.GREEN
            canvas.drawCircle(cx.toFloat(), cy.toFloat(), 10F,paint)
        }else{
            paint.color = Color.GRAY
            canvas.drawText("X", cy.toFloat(), cy.toFloat(),paint)
        }
    }

    override fun setAlpha(p0: Int) {

    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(p0: ColorFilter?) {
    }

    fun isEnabled(): Boolean {
        return enabled
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

}