```
    一个可以从底部滑动的卡片，可以自定义多个滑动节点
```

#### ![演示](https://github.com/wufuqi123/CardBottomSlideView/raw/master/assets/gif/1.gif)



#### 基础功能
1. 添加依赖

    请在 build.gradle 下添加依赖。

    ``` 
        implementation 'cn.wufuqi:CardBottomSlideView:1.0.1'
    ```

2. 设置jdk8或更高版本

    因为本sdk使用了jdk8才能使用的 Lambda 表达式，所以要在 build.gradle 下面配置jdk8或以上版本。

    ``` 
    android {
        ....

        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
        
    }
    ```


3. xml 使用
    ```
        <cn.wufuqi.cardbottomslideview.CardBottomSlideView
            android:id="@+id/cbsv"
            android:layout_width="match_parent"
            android:layout_height="match_parent"/>
    ```


4. 设置CardBottomSlideView可滑动高度

    可滑动节点高度填写 2 - 无穷大 。这里为了显示写3个高度

    ```
        cbsv.setSlideHeightList(
            cbsvParent.measuredHeight-dip2px(150),
            dip2px(400),
            dip2px(100)
        )
    ```

5. 设置移动到第几个角标

    默认移动到最顶部的节点，也就是  SlideHeightList 第0个角标

    ```
        //移动到第2个角标
        cbsv.movePosition(2)
    ```

6. 使用动画

    ```
        cbsv.animationPosition(time = 500L, position = 2)
    ```

7. Y值改变监听

    ```
        cbsv.addOnMoveYChangeListener {oldY, newY, topY, bottomY ->
            // oldY 上一次移动前的Y值
            // newY 当前的Y值
            // topY 当前的相对顶部Y值节点，注意：不是最顶部节点，是相对节点
            // bottomY 当前的相对底部Y值节点，注意：不是最底部节点，是相对节点
        }
    ```

8. node节点改变监听，就是 position 改变监听

    ```
        cbsv.addOnMoveNodeChangeList { lastNodePosition, currNodePosition ->
            // lastNodePosition 上一个Position
            // currNodePosition 当前Position
        }
    ```