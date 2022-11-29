package cn.wufuqi.cardbottomslideview

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.ScrollView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wukonganimation.tween.Easing
import com.wukonganimation.tween.Tween
import com.wukonganimation.tween.TweenManager
import java.lang.Error
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * 可滑动的卡片
 */
class CardBottomSlideView : FrameLayout {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )


    /**
     * 可滑动的高度
     */
    private var mHeightList = mutableListOf(500, 300)

    /**
     * 固定可到达的Y值
     */
    private var yList = mutableListOf<Float>()


    var mTween: WeakReference<Tween>? = null

    /**
     * y 值被改变监听
     */
    private val mOnMoveYChangeList =
        mutableListOf<(oldY: Float, newY: Float, topY: Float, bottomY: Float) -> Unit>()

    /**
     * 移动的节点被改变
     */
    private val mOnMoveNodeChangeList =
        mutableListOf<(lastNodePosition: Int, currNodePosition: Int) -> Unit>()


    var onTouchDownListener: (() -> Unit)? = null

    /**
     * 当前的节点
     */
    var currNodePosition = 0

    /**
     * 是否开启上滑下滑手势
     */
    var isOpenGesture = true

    /**
     * 手势移动距离
     */
    var gestureOffsetY = 40f

    /**
     * 手势间隔时间
     */
    var gestureTime = 200L

    /**
     * 松开手的比例，执行向上向下的动画
     */
    var moveRatio = 0.1

    /**
     * 手指当前每一帧的Y值
     */
    private var intervalTouchY: Float = 0f

//    /**
//     * 最大的顶部Y值
//     */
//    private var maxTopY: Float = 0f

    /**
     * 手指是否按下
     */
    private var isTouch: Boolean = false

    /**
     * dispatchTouchEvent 方法
     * 是否返回的value
     */
    private var returnDispatchTouchValue = true


    /**
     * 当前手指移动时顶部的Y值
     */
    private var currMoveTopY = 0f


    /**
     * 当前手指移动时底部的Y值
     */
    private var currMoveBottomY = 0f


    /**
     * 松开手时执行动画的总时间
     */
    var time = 500L

    /**
     * move时的缓动函数
     */
    var moveEasing = Easing.outQuad()


    /**
     * 手指按下时的Y值
     */
    private var dispatchTouchStartY: Float = 0f

    /**
     * 按下时拿到的可滚动的view
     */
    private var mTouchScrollView: View? = null

    /**
     * scrollView 是否可滚动
     */
    private var isTouchScrollView = false

    /**
     * 是否执行了move拖拽
     */
    private var isMove = false


    /**
     * 按下的开始时间
     */
    private var startTouchTime = 0L
    private var startDispatchTouchEventY = 0f


    private var maxNoResponseMoveY = 20f
    private var currNoResponseMoveY = 0f


    /**
     * 根据角标获取可滑动的节点  mHeightList
     */
    fun getIndexSlideNodeHeight(position: Int): Int {
        return mHeightList[position]
    }

    /**
     * 根据角标获取可滑动的节点  yList
     */
    fun getIndexSlideNodeY(position: Int): Float {
        return yList[position]
    }

    /**
     * 设置可滑动的节点
     */
    fun setSlideHeightList(vararg heights: Int) {
        setSlideHeightList(heights.toTypedArray().toMutableList())
    }

    /**
     * 设置可滑动的节点
     */
    fun setSlideHeightList(hList: List<Int>) {
        if (hList.isEmpty()) {
            throw Error("CardSlideView  setSlideHeightList 可滑动的高度节点为空")
        }
        val list = hList.sortedByDescending { t -> t }
        if (list[list.size - 1] < 0) {
            throw Error("CardSlideView  setSlideHeightList 可滑动的高度节点不可为0")
        }
        layoutParams.height = list[0]
        layoutParams = layoutParams
        mHeightList.clear()
        mHeightList.addAll(list)
        yList.clear()
        mHeightList.forEach { _ ->
            yList.add(0f)
        }
        post {
            mHeightList[0] = measuredHeight
            yList[0] = ((parent as View).measuredHeight - mHeightList[0]).toFloat()
            y = yList[0]
            for (i in 1.until(mHeightList.size)) {
                yList[i] = yList[0] + (mHeightList[0] - mHeightList[i])
            }
            moveNodeChange()
        }
    }


    /**
     * 添加改变监听
     */
    fun addOnMoveYChangeListener(listener: (oldY: Float, newY: Float, topY: Float, bottomY: Float) -> Unit) {
        mOnMoveYChangeList.add(listener)
    }

    /**
     * 添加节点改变监听
     */
    fun addOnMoveNodeChangeList(listener: (lastNodePosition: Int, currNodePosition: Int) -> Unit) {
        mOnMoveNodeChangeList.add(listener)
    }


    private fun moveYChange(oldY: Float, newY: Float) {
        val topY = getListStartValue(newY)
        val bottomY = getListEndValue(newY)
        mOnMoveYChangeList.forEach { it.invoke(oldY, newY, topY, bottomY) }
    }


    private fun moveNodeChange() {
        for (i in yList.indices) {
            if (y == yList[i]) {
                if (currNodePosition == i) {
                    return
                }
                mOnMoveNodeChangeList.forEach { it.invoke(currNodePosition, i) }
                currNodePosition = i
                break
            }
        }
    }

    /**
     * 动画移动Y值
     */
    private fun animationY(t: Long = time, moveY: Float) {
        mTween?.get()?.stop()?.remove()
        var oldY = y
        mTween = WeakReference(TweenManager.builder(this)
            .to(mutableMapOf("y" to moveY))
            .setExpire(true)
            .time(t)
            .easing(moveEasing)
            .on(TweenManager.EVENT_UPDATE) {
                moveYChange(oldY, y)
                moveNodeChange()
                oldY = y
            }
            .on(TweenManager.EVENT_END) {
                moveYChange(oldY, y)
                moveNodeChange()
                oldY = y
            }
            .start()
        )
    }


    /**
     * 没有动画直接移动
     * 移动到那个位置
     */
    fun movePosition(position: Int) {
        post {
            val newY = yList[position]
            val oldY = y
            y = newY
            moveYChange(oldY, newY)
            moveNodeChange()
        }
    }

    /**
     * 执行动画移动到指定节点
     * @param t 执行时间毫秒数
     */
    fun animationPosition(t: Long = time, position: Int) {
        post {
            animationY(t, yList[position])
        }
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchDownListener?.invoke()
                startTouchTime = SystemClock.uptimeMillis()
                mTween?.get()?.stop()?.remove()
                intervalTouchY = ev.rawY
                startDispatchTouchEventY = ev.rawY
                dispatchTouchStartY = y
                isTouch = true
                isMove = false
                returnDispatchTouchValue = true
                mTouchScrollView = getTouchScrollView(ev)
                if (mTouchScrollView != null) {
                    returnDispatchTouchValue = super.dispatchTouchEvent(ev)
                }
                isTouchScrollView = false
                currNoResponseMoveY = 0f
            }
            MotionEvent.ACTION_UP -> {

                if (!(y <= currMoveTopY || y >= currMoveBottomY) && !isTouchScrollView) {
                    val moveOffsetY = y - dispatchTouchStartY
                    val moveRatioY = abs(currMoveTopY - currMoveTopY) * moveRatio
                    if (isOpenGesture && abs(moveOffsetY) < moveRatioY) {
                        gestureUp(ev)
                    } else {
                        moveUp(ev)
                    }
                } else {
                    returnDispatchTouchValue = if (abs(ev.rawY - startDispatchTouchEventY) > 5
                        && (!isTouchScrollView || (isTouchScrollView && isShowTopRecyclerView(
                            mTouchScrollView
                        )))
                        && !isHorizontalRecyclerView(mTouchScrollView)
                    ) {
                        true
                    } else {
                        if (mTouchScrollView == null) {
                            ev.action = MotionEvent.ACTION_DOWN
                            super.dispatchTouchEvent(ev)
                        }
                        ev.action = MotionEvent.ACTION_UP
                        super.dispatchTouchEvent(ev)
                    }
                }
                moveNodeChange()
                mTouchScrollView = null
                isTouchScrollView = false
                isTouch = false
            }
            MotionEvent.ACTION_MOVE -> {
                currNoResponseMoveY += abs(intervalTouchY - ev.rawY)
                if (currNoResponseMoveY < maxNoResponseMoveY) {
//                    intervalTouchY = ev.rawY
                    return true
                }

                if (isTouch) {
                    if (isMove) move(ev)
                    else if (mTouchScrollView != null && (isTouchScrollView || isTouchScrollView(ev))) {
                        isTouchScrollView = true
                        returnDispatchTouchValue = super.dispatchTouchEvent(ev)
                    } else {
                        move(ev)
                    }
                } else {
                    returnDispatchTouchValue = super.dispatchTouchEvent(ev)
                }
            }
        }
        return returnDispatchTouchValue
    }

    private fun isHorizontalRecyclerView(v: View?): Boolean {
        if (v is RecyclerView) {
            if (v.layoutManager is LinearLayoutManager) {
                if ((v.layoutManager as LinearLayoutManager).orientation == LinearLayoutManager.HORIZONTAL) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 判断是否完全显示最顶部的 item
     */
    private fun isShowTopRecyclerView(v: View?): Boolean {
        if (v == null) {
            return false
        }
        if (v is RecyclerView) {
            if (v.childCount == 0) return true
            var topPosition = -1
            if (v.layoutManager is LinearLayoutManager) {
                topPosition =
                    (v.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            } else if (v.layoutManager is GridLayoutManager) {
                topPosition =
                    (v.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            }
            return topPosition == 0
        } else {
            return mTouchScrollView!!.scrollY == 0
        }

    }

    /**
     * 是否点击ScrollView
     */
    private fun isTouchScrollView(ev: MotionEvent): Boolean {
        if (mTouchScrollView == null) return false
        if (y != yList[0]) return false
        val moveY = ev.rawY - intervalTouchY
        if (moveY > 0 && isShowTopRecyclerView(mTouchScrollView)) return false
        return true
    }

    /**
     * 移动
     */
    private fun move(ev: MotionEvent) {
//        val minBottomY = maxTopY + (maxHeight - currMinHeight)
        val endTouchY = ev.rawY
        val finalY = y + endTouchY - intervalTouchY
        val oldY = y
        val topY = getListStartValue(finalY)
        val bottomY = getListEndValue(finalY)
        y = if (finalY <= topY) {
            topY
        } else if (finalY >= bottomY) {
            bottomY
        } else {
            finalY
        }
        currMoveTopY = topY
        currMoveBottomY = bottomY
        moveYChange(oldY, y)
        moveNodeChange()
        isMove = true
        intervalTouchY = endTouchY
    }

    private fun getListStartValue(value: Float): Float {
        for (i in (yList.size - 1) downTo 0) {
            if (yList[i] < value) {

                return yList[i]
            }
        }
        return yList[0]
    }

    private fun getListEndValue(value: Float): Float {
        yList.forEach {
            if (it > value) {
                return it
            }
        }
        return yList[yList.size - 1]
    }

    /**
     * move时手指松开
     */
    private fun moveUp(ev: MotionEvent) {
//        val minBottomY = maxTopY + (maxHeight - currMinHeight)


        val moveOffsetY = y - dispatchTouchStartY
        val moveRatioY = abs(currMoveBottomY - currMoveTopY) * moveRatio
        val moveY = if (moveOffsetY < 0) {
            val isMoveRatioArrive = abs(moveOffsetY.toDouble()) >= moveRatioY
            //往上滑
            if (isMoveRatioArrive || (!isMoveRatioArrive && (y - currMoveTopY < moveRatioY))) currMoveTopY else currMoveBottomY
        } else {
            if ((moveOffsetY.toDouble()) <= moveRatioY) currMoveTopY else currMoveBottomY
        }
        val moveTime = time * (abs(moveY - y) / (currMoveBottomY - currMoveTopY))
        animationY(moveTime.toLong(), moveY)
    }

    /**
     * 手势抬起
     */
    private fun gestureUp(ev: MotionEvent) {
//        val minBottomY = maxTopY + (maxHeight - currMinHeight)
        val moveOffsetY = y - dispatchTouchStartY
        val endTouchTime = SystemClock.uptimeMillis()
        if (endTouchTime - startTouchTime > gestureTime || abs(moveOffsetY) < gestureOffsetY) {
            moveUp(ev)
            return
        }
        val moveY = if (moveOffsetY < 0) currMoveTopY else currMoveBottomY
        val moveTime = time * (abs(moveY - y) / (currMoveBottomY - currMoveTopY))
        animationY(moveTime.toLong(), moveY)
    }


    /**
     * 获取能滑动的view
     */
    private fun getTouchScrollView(ev: MotionEvent): View? {
        val viewGroupList = mutableListOf<View>()
        getChildrenScrollViews(viewGroupList, this)
        viewGroupList.forEach {
            val r = Rect()
            it.getGlobalVisibleRect(r)
            if (r.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                return it
            }
        }
        return null
    }

    /**
     * 获取子view能滑动的节点
     */
    private fun getChildrenScrollViews(list: MutableList<View>, view: View) {
        if (view is ViewGroup) {
            view.children.forEach {
                if (!it.isVisible) {
                    return@forEach
                }
                if (it is ScrollView) {
                    list.add(it)
                } else if (it is ListView) {
                    list.add(it)
                } else if (it is RecyclerView) {
                    list.add(it)
                } else if (it is ViewGroup) {
                    getChildrenScrollViews(list, it)
                }
            }
        }

    }


    /**
     * 当布局发生改变时，并且正在滑动，取消调用  mTouchScrollView的内容
     */
    override fun requestLayout() {
        super.requestLayout()
        //当布局发生变化时
        if (mTouchScrollView == null) {
            return
        }
        val listScrollViews = mutableListOf<View>()
        getChildrenScrollViews(listScrollViews, this)
        var isRemoveScrollView = true
        listScrollViews.forEach {
            if (it == mTouchScrollView) {
                isRemoveScrollView = false
            }
        }
        if (isRemoveScrollView) {
            mTouchScrollView = null
            isTouchScrollView = false
        }
    }
}