package com.jedi.wolf_and_hunter.myViews.rocker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.jedi.wolf_and_hunter.myViews.characters.BaseCharacterView;
import com.jedi.wolf_and_hunter.utils.MyMathsUtils;
import com.jedi.wolf_and_hunter.utils.ViewUtils;

/**
 * Created by Administrator on 2017/3/29.
 */

public class LeftRocker extends JRocker {


    public LeftRocker(Context context, AttributeSet attrs) {

        super(context, attrs);
//        actionButtonLeft=(padRadius+rockerRadius)*2-actionButtonsWidth;
//        actionButtonTop=0;

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        params.gravity = Gravity.TOP | Gravity.LEFT;
        setLayoutParams(params);
    }


/*
下面这个onTouchEvent为推摇杆模式，由于手感问题，暂时废弃
 */
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        //获取到手指处的横坐标和纵坐标
//        int x = (int) event.getX();
//        int y = (int) event.getY();
//        switch(event.getAction())
//        {
//
//            case MotionEvent.ACTION_DOWN:
//
////                if(MyMathsUtils.isInRECT(actionButtonLeft,actionButtonTop
////                        ,actionButtonLeft+actionButtonsWidth,actionButtonTop+actionButtonsWidth
////                        ,new Point(x,y))){
////                    readyToFire=true;
////                }
////                else
//                    if(MyMathsUtils.isInCircle(rockerCircleCenter,rockerRadius,new Point(x,y))) {
//                    isHoldingRocker = true;
//                    startCenterX=x;
//                    startCenterY=y;
//                }
//                break;
//            case MotionEvent.ACTION_UP:
////                if(readyToFire){
////                    GameBaseAreaActivity.myCharacter.judgeAttack();
////                    readyToFire=false;
////                }
//                isHoldingRocker=false;
//                distance=0;
//                rockerCircleCenter.set(padCircleCenter.x,padCircleCenter.y);
//                synchronized (bindingCharacter) {
//                    bindingCharacter.needMove = false;
//                    bindingCharacter.offX = 0;
//                    bindingCharacter.offY = 0;
//                    startCenterX = padCircleCenter.x;
//                    startCenterY = padCircleCenter.y;
//                    invalidate();
//                    break;
//                }
//            case MotionEvent.ACTION_MOVE:
//                if(isHoldingRocker==false) {
//                    break;
//                }
//                int relateX=x-startCenterX;
//                int relateY=y-startCenterY;
//                Point newPosition=new Point(padCircleCenter.x+relateX,padCircleCenter.y+relateY);
//                rockerCircleCenter= new ViewUtils().revisePointInCircleViewMovement(padCircleCenter,padRadius,newPosition);
//                distance= MyMathsUtils.getDistance(rockerCircleCenter,padCircleCenter);
//                synchronized (bindingCharacter) {
//                    bindingCharacter.offX = rockerCircleCenter.x - padCircleCenter.x;
//                    bindingCharacter.needMove = true;
//                    bindingCharacter.offY = rockerCircleCenter.y - padCircleCenter.y;
//                    bindingCharacter.needMove = true;
//                }
//                invalidate();
//
//        }
//
//        return true;
//    }


    public void reactUsingTouchPadMode(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        int offX=0;
        int offY=0;
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

//                if(MyMathsUtils.isInRECT(actionButtonLeft,actionButtonTop
//                        ,actionButtonLeft+actionButtonsWidth,actionButtonTop+actionButtonsWidth
//                        ,new Point(x,y))){
//                    readyToFire=true;
//                }
//                else
                if (MyMathsUtils.isInCircle(rockerCircleCenter, rockerRadius+padRadius, new Point(x, y))) {
                    isHoldingRocker = true;
                    int relateX = x - padCircleCenter.x;
                    int relateY = y - padCircleCenter.y;
                    Point newPosition = new Point(padCircleCenter.x + relateX, padCircleCenter.y + relateY);
                    rockerCircleCenter = new ViewUtils().revisePointInCircleViewMovement(padCircleCenter, padRadius, newPosition);
                    distance = MyMathsUtils.getDistance(rockerCircleCenter, padCircleCenter);
                    synchronized (bindingCharacter) {
                        bindingCharacter.offX = rockerCircleCenter.x - padCircleCenter.x;
                        bindingCharacter.needMove = true;
                        bindingCharacter.offY = rockerCircleCenter.y - padCircleCenter.y;
                        bindingCharacter.needMove = true;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
//                if(readyToFire){
//                    GameBaseAreaActivity.myCharacter.judgeAttack();
//                    readyToFire=false;
//                }
                isHoldingRocker = false;
                distance = 0;
                rockerCircleCenter.set(padCircleCenter.x, padCircleCenter.y);
                synchronized (bindingCharacter) {
                    bindingCharacter.needMove = false;
                    bindingCharacter.offX = 0;
                    bindingCharacter.offY = 0;
                    invalidate();
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                if (isHoldingRocker == false) {
                    break;
                }
                int relateX = x - padCircleCenter.x;
                int relateY = y - padCircleCenter.y;
                Point newPosition = new Point(padCircleCenter.x + relateX, padCircleCenter.y + relateY);
                rockerCircleCenter = new ViewUtils().revisePointInCircleViewMovement(padCircleCenter, padRadius, newPosition);
                distance = MyMathsUtils.getDistance(rockerCircleCenter, padCircleCenter);
                synchronized (bindingCharacter) {
                    offX=rockerCircleCenter.x - padCircleCenter.x;
                    offY=rockerCircleCenter.y - padCircleCenter.y;
                    bindingCharacter.offX = offX;
                    bindingCharacter.needMove = true;
                    bindingCharacter.offY = offY;
                    bindingCharacter.needMove = true;
                    double offDistance = Math.sqrt(offX * offX + offY * offY);
                    int nowMoveSpeed = bindingCharacter.nowSpeed;
                    bindingCharacter.runOrWalk = BaseCharacterView.MOVINT_TYPE_RUN;
                    if (offDistance < JRocker.padRadius * 3 / 4) {
                        bindingCharacter.runOrWalk = BaseCharacterView.MOVINT_TYPE_WALK;
                    }
                }
                invalidate();

        }
    }

    public void reactUsingRockerMode(MotionEvent event) {
        //获取到手指处的横坐标和纵坐标
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

//                if(MyMathsUtils.isInRECT(actionButtonLeft,actionButtonTop
//                        ,actionButtonLeft+actionButtonsWidth,actionButtonTop+actionButtonsWidth
//                        ,new Point(x,y))){
//                    readyToFire=true;
//                }
//                else
                if (MyMathsUtils.isInCircle(rockerCircleCenter, rockerRadius, new Point(x, y))) {
                    isHoldingRocker = true;
                    startCenterX = x;
                    startCenterY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
//                if(readyToFire){
//                    GameBaseAreaActivity.myCharacter.judgeAttack();
//                    readyToFire=false;
//                }
                isHoldingRocker = false;
                distance = 0;
                rockerCircleCenter.set(padCircleCenter.x, padCircleCenter.y);
                synchronized (bindingCharacter) {
                    bindingCharacter.needMove = false;
                    bindingCharacter.offX = 0;
                    bindingCharacter.offY = 0;
                    startCenterX = padCircleCenter.x;
                    startCenterY = padCircleCenter.y;
                    invalidate();
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                if (isHoldingRocker == false) {
                    break;
                }
                int relateX = x - startCenterX;
                int relateY = y - startCenterY;
                Point newPosition = new Point(padCircleCenter.x + relateX, padCircleCenter.y + relateY);
                rockerCircleCenter = new ViewUtils().revisePointInCircleViewMovement(padCircleCenter, padRadius, newPosition);
                distance = MyMathsUtils.getDistance(rockerCircleCenter, padCircleCenter);
                synchronized (bindingCharacter) {
                    bindingCharacter.offX = rockerCircleCenter.x - padCircleCenter.x;
                    bindingCharacter.needMove = true;
                    bindingCharacter.offY = rockerCircleCenter.y - padCircleCenter.y;
                    bindingCharacter.needMove = true;
                }
                invalidate();

        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //reactUsingRockerMode(event);
        reactUsingTouchPadMode(event);
        return true;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        canvas.drawBitmap(fireBitmap,actionButtonLeft,actionButtonTop,null);
//        if(bindingCharacter.attackCount<bindingCharacter.maxAttackCount&&bindingCharacter.reloadAttackStartTime!=0){
//            float sweepAngle=360*((new Date().getTime()-bindingCharacter.reloadAttackStartTime)/bindingCharacter.reloadAttackNeedTime);
//            if (sweepAngle>360)
//                sweepAngle=359;
//            canvas.drawArc(new RectF(actionButtonLeft,actionButtonTop,actionButtonLeft+fireBitmap.getWidth(),actionButtonTop+fireBitmap.getHeight()),0,sweepAngle,true,normalPaint);
//        }
//        canvas.drawText(new Integer(bindingCharacter.attackCount).toString(),actionButtonLeft+5,baselineY,normalPaint);
    }

}
