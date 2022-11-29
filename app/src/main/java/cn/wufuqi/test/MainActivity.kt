package cn.wufuqi.test

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import cn.wufuqi.cardbottomslideview.CardBottomSlideView
import java.lang.Math.abs

class MainActivity : AppCompatActivity() {

    var btnBottomY = 0f
    var btnCenterY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cbsv = findViewById<CardBottomSlideView>(R.id.cbsv)
        val btn1 = findViewById<AppCompatTextView>(R.id.btn_1)
        val btn2 = findViewById<AppCompatTextView>(R.id.btn_2)
        val btn3 = findViewById<AppCompatTextView>(R.id.btn_3)
        val btn4 = findViewById<AppCompatTextView>(R.id.btn_4)
        val btn5 = findViewById<AppCompatTextView>(R.id.btn_5)
        val cbsvParent = (cbsv.parent as View)

        cbsvParent.post {
            // 设置 3个可滑动的高
            cbsv.setSlideHeightList(
                cbsvParent.measuredHeight-dip2px(150),
                dip2px(400),
                dip2px(100)
            )
            //移动到第几个角标
            cbsv.movePosition(2)


            cbsv.post {
                btnBottomY = cbsv.getIndexSlideNodeY(2) - btn1.measuredHeight - dip2px(20)
                btnCenterY = cbsv.getIndexSlideNodeY(1) - btn1.measuredHeight - dip2px(20)
                btn1.y = btnBottomY
                btn2.y = btnBottomY
                btn3.y = btnBottomY
                btn4.y = btnCenterY
                btn5.y = btnCenterY
            }
        }


        cbsv.addOnMoveYChangeListener {oldY, newY, topY, bottomY ->
            if (topY == cbsv.getIndexSlideNodeY(0) &&
                bottomY == cbsv.getIndexSlideNodeY(1)
            ) {
                val moveY = btnCenterY - abs(newY - bottomY)
                btn3.y = moveY
                btn4.y = moveY
                btn5.y = moveY
            } else if (topY == cbsv.getIndexSlideNodeY(1) &&
                bottomY == cbsv.getIndexSlideNodeY(2)
            ){
                val moveY = btnBottomY - abs(newY - bottomY)
                btn1.y = moveY
                btn2.y = moveY
                btn3.y = moveY
            }

        }


        findViewById<AppCompatButton>(R.id.btn_top).setOnClickListener {
            cbsv.animationPosition(position = 0)
        }

        findViewById<AppCompatButton>(R.id.btn_center).setOnClickListener {
            cbsv.animationPosition(position = 1)
        }

        findViewById<AppCompatButton>(R.id.btn_bottom).setOnClickListener {
            cbsv.animationPosition(position = 2)
        }


        findViewById<View>(R.id.v_1).setOnClickListener {
            Log.e("-----------","---------------")
        }
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    fun dip2px(dpValue: Int): Int {
        val scale: Float = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}