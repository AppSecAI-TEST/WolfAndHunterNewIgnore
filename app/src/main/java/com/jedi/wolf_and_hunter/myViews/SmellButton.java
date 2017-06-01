package com.jedi.wolf_and_hunter.myViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.jedi.wolf_and_hunter.R;
import com.jedi.wolf_and_hunter.activities.GameBaseAreaActivity;
import com.jedi.wolf_and_hunter.myObj.MyVirtualWindow;
import com.jedi.wolf_and_hunter.myViews.characters.BaseCharacterView;

import java.util.Date;

/**
 * Created by Administrator on 2017/5/10.
 */

public class SmellButton extends View {
    static Bitmap attackBitmap;
    public int buttonSize;
    public Paint normalPaint;
    public TextPaint redTextPaint;
    public TextPaint blackTextPaint;
    public int baselineY;
    public int bitmapLeft;
    public int bitmapTop;
    private long lastTouchTime;
    boolean isTouchingInside = true;
    private int lastTouchX;
    private int lastTouchY;
    public BaseCharacterView bindingCharacter;

    public SmellButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public SmellButton(Context context) {
        super(context);
        init();
    }

    public void reCreateBitmap() {
        if (GameBaseAreaActivity.myCharacter != null) {
            if (GameBaseAreaActivity.myCharacter.characterType == BaseCharacterView.CHARACTER_TYPE_HUNTER) {
                attackBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fire);
            } else if (GameBaseAreaActivity.myCharacter.characterType == BaseCharacterView.CHARACTER_TYPE_WOLF) {
                attackBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.wolf_attack);
            }
            Matrix matrix = new Matrix();
            matrix.postScale((float) (buttonSize * 0.8) / attackBitmap.getWidth(), (float) (buttonSize * 0.8) / attackBitmap.getHeight());
            attackBitmap = Bitmap.createBitmap(attackBitmap, 0, 0, attackBitmap.getWidth(), attackBitmap.getHeight(), matrix, true);

        }
    }


    public void init() {

        int windowWidth = MyVirtualWindow.getWindowWidth(getContext());
        int windowHeight = MyVirtualWindow.getWindowHeight(getContext());
        buttonSize = (int) (windowWidth / 8);

        normalPaint = new Paint();

        normalPaint.setColor(Color.WHITE);
        normalPaint.setStyle(Paint.Style.FILL);
        normalPaint.setAntiAlias(true);


        redTextPaint = new TextPaint();
        redTextPaint.setColor(Color.RED);
        redTextPaint.setTextSize(buttonSize / 2);
        redTextPaint.setTextAlign(Paint.Align.CENTER);

        blackTextPaint = new TextPaint();
        blackTextPaint.setColor(Color.BLACK);
        blackTextPaint.setTextSize(buttonSize / 2);
        blackTextPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetricsInt fontMetrics = redTextPaint.getFontMetricsInt();
        baselineY = (buttonSize - fontMetrics.bottom - fontMetrics.top) / 2;

        if (attackBitmap == null) {

            attackBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.fire);
            Matrix matrix = new Matrix();
            matrix.postScale((float) (buttonSize * 0.8) / attackBitmap.getWidth(), (float) (buttonSize * 0.8) / attackBitmap.getHeight());
            attackBitmap = Bitmap.createBitmap(attackBitmap, 0, 0, attackBitmap.getWidth(), attackBitmap.getHeight(), matrix, true);
        }
        bitmapLeft = (buttonSize - attackBitmap.getWidth()) / 2;
        bitmapTop = (buttonSize - attackBitmap.getHeight()) / 2;

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //获取到手指处的横坐标和纵坐标
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                lastTouchTime = new Date().getTime();
                if (GameBaseAreaActivity.myPlayerInfo.characterType == BaseCharacterView.CHARACTER_TYPE_WOLF) {
                    bindingCharacter.isStay = true;
                }
                isTouchingInside = true;

                break;


            case MotionEvent.ACTION_UP:
                lastTouchTime = 0;
                if (GameBaseAreaActivity.myPlayerInfo.characterType == BaseCharacterView.CHARACTER_TYPE_WOLF) {
                    bindingCharacter.isStay = false;
                }
                if (lastTouchX > 0 && lastTouchX < getWidth() && lastTouchY > 0 && lastTouchY < getHeight())
                    bindingCharacter.attack();
                break;

            case MotionEvent.ACTION_MOVE:
                lastTouchX = x;
                lastTouchY = y;
                if (new Date().getTime() - lastTouchTime > 800) {
                    if (bindingCharacter.attackCount<bindingCharacter.maxAttackCount&&GameBaseAreaActivity.myPlayerInfo.characterType == BaseCharacterView.CHARACTER_TYPE_HUNTER) {
                        bindingCharacter.reloadAttackCount();
                    }
                }

        }

        return true;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int realSize = buttonSize;
        int width = realSize;
        int height = realSize;
        setMeasuredDimension(width, height);
    }

    public int measureDimension(int defaultSize, int measureSpec) {
        int result;

        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = defaultSize;   //UNSPECIFIED
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(attackBitmap, bitmapLeft, bitmapTop, null);
        if (bindingCharacter == null) {
            bindingCharacter = GameBaseAreaActivity.myCharacter;
            invalidate();
            return;
        }

        if (bindingCharacter.attackCount < bindingCharacter.maxAttackCount && bindingCharacter.nowReloadingCount != 0) {
            float percent = (float)bindingCharacter.nowReloadingCount/BaseCharacterView.reloadAttackTotalCount;
            float sweepAngle = 360 * percent;
            if (sweepAngle > 360)
                sweepAngle = 359;
            if (sweepAngle > 0)
                Log.i("", "");
            canvas.drawArc(new RectF(0, 0, buttonSize, buttonSize), 0, sweepAngle, true, normalPaint);
        }
        if (bindingCharacter.attackCount == 0)
            canvas.drawText(new Integer(bindingCharacter.attackCount).toString(), buttonSize / 2, baselineY, redTextPaint);
        else
            canvas.drawText(new Integer(bindingCharacter.attackCount).toString(), buttonSize / 2, baselineY, blackTextPaint);
        invalidate();
    }
}