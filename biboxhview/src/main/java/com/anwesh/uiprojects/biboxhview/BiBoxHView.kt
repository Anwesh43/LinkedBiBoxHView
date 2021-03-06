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
val delay : Long = 20

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
    drawVerticalH(0, 0f, size, paint)
    drawHorizontalH(0, 0f, size, paint)
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
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
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
                    Thread.sleep(delay)
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

    data class BBHNode(var i : Int, val state : State = State()) {

        private var next : BBHNode? = null
        private var prev : BBHNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = BBHNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawBBHNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BBHNode {
            var curr : BBHNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BiBoxH(var i : Int) {

        private val root : BBHNode = BBHNode(0)
        private var curr : BBHNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BiBoxHView) {

        private val animator : Animator = Animator(view)
        private val bbh : BiBoxH = BiBoxH(0)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            bbh.draw(canvas, paint)
            animator.animate {
                bbh.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            bbh.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : BiBoxHView {
            val view : BiBoxHView = BiBoxHView(activity)
            activity.setContentView(view)
            return view
        }
    }
}