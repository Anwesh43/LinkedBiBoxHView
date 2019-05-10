package com.anwesh.uiprojects.biboxhview

/**
 * Created by anweshmishra on 10/05/19.
 */

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Color
import android.view.View

val nodes : Int = 5
val lines : Int = 2
val parts : Int = 2
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val foreColor : Int = Color.parseColor("#4CAF50")
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float, a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawVerticalH(j : Int, sc : Float, size : Float, paint : Paint) {
    save()
    translate(size / 2, 0f)
    rotate(90f * (1f - 2 * j) * sc.divideScale(j, lines))
    drawLine(0f, 0f, 0f, -size / 2, paint)
    restore()
}

fun Canvas.drawHorizontalH(j : Int, sc : Float, size : Float, paint : Paint) {
    save()
    translate(size, -size / 2)
    rotate(90f * (1f - 2 * j) * sc.divideScale(j, lines))
    drawLine(0f, 0f, -size / 2,  0f, paint)
    restore()
}

fun Canvas.drawBoxH(i : Int, sc1 : Float, sc2 : Float, size : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        drawVerticalH(j, sc1, size, paint)
        drawHorizontalH(j, sc2, size, paint)
    }
}

fun Canvas.drawBBHNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    paint.color = foreColor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(parts - 1)) {
        save()
        scale(1f - 2 * j, 1f)
        drawBoxH(i, sc1.divideScale(j, lines), sc2.divideScale(j, lines), size, paint)
        restore()
    }
    restore()
}

class BiBoxHView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines * parts, lines * parts)
            if (Math.abs(scale - prevScale) > 1) {
                scale  = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }
}