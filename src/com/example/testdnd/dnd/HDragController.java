/*
 * @(#)DragController.java $version 2011. 11. 18.
 *
 * Copyright 2011 NAVER JAPAN. All rights Reserved. 
 * NAVER JAPAN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.example.testdnd.dnd;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.example.testdnd.VisibleChildDetectableHorizontalScrollView;

/**
 * 이 클래스는 ViewGroup내에서 사용자가 드래그하는 뷰와 연관된 모든 터치 이벤트를 처리한다.
 * 드래그가 시작되면, 특별한 뷰인 DragView가 만들어 지고, 이 뷰는 사용자가 드래그를 끝낼 때까지 화면 주위로 움직일 수 있다.
 * 사용자와의 피드백 부분을 보면, 드래그가 시작될 때 이 객체는 장치를 진동시키고, DropTarget과 DragSource 인터페이스에서 정의된 메소드를 통해 다른 객체들과 상호작용할 수 있다.
 * 
 * 또한 드래그되고 드롭될 때 알림을 받고 싶은 객체를 위해서 DragListener 인터페이스를 지원한다.
 * 
 * 드래그&드롭 관련 기능은 아래의 오픈소스를 기반으로 만들어졌다.
 * http://blahti.wordpress.com/2010/12/20/moving-views-in-android-part-1/
 * 
 * 위 소스에서 다음의 기능들이 추가 또는 변경되었다.
 * - 드래그하는 동안 auto-scrolling 기능(BY_TOUCH_MOVE, BY_TIMER)
 * - DragSource를 드래그해서 특정 DropTarget에 놓으면 item의 순서를 바꾸게
 * - 그 외 DropTarget 추가 방식 변경 및 전반적으로 소스 정리 및 수정 
 *
 * @author 박성현
 */

public class HDragController {
	private static final String TAG = "DragAndDrop";

	/** Indicates the drag is a move.  */
	public static final int DRAG_ACTION_MOVE = 0;

	/** Indicates the drag is a copy.  */
	public static final int DRAG_ACTION_COPY = 1;

	public static enum AutoScrollingType {
		BY_TOUCH_MOVE, // 터치 move 방식으로. 계속 움직여야 함.
		BY_TIMER // 타이머 방식, 터치한 상태로 안 움직여도 스크롤 됨.
	}

	private static final int VIBRATE_DURATION = 35;

	private static final boolean PROFILE_DRAWING_DURING_DRAG = false;

	private Context mContext;
	private Vibrator mVibrator;

	private AutoScrollingType autoScrollingType = AutoScrollingType.BY_TOUCH_MOVE;

	private boolean isAutoScrollable;

	// temporaries to avoid gc thrash
	private Rect mRectTemp = new Rect();
	private final int[] mCoordinatesTemp = new int[2];

	/** Whether or not we're dragging. */
	public boolean mDragging;

	/** X coordinate of the down event. */
	private float mMotionDownX;

	/** Y coordinate of the down event. */
	private float mMotionDownY;

	/** Info about the screen for clamping. */
	private DisplayMetrics mDisplayMetrics = new DisplayMetrics();

	/** Original view that is being dragged.  */
	private View mOriginator;

	/** X offset from the upper-left corner of the cell to where we touched.  */
	private float mTouchOffsetX;

	/** Y offset from the upper-left corner of the cell to where we touched.  */
	private float mTouchOffsetY;

	/** Where the drag originated */
	private DragSource mDragSource;

	/** The data associated with the object being dragged. 뷰가 재활용되기 때문에 여기서는 드래그를 시작한 position을 저장해 놓는 용도로 사용 */
	public Object mDragInfo;

	/** The view that moves around while you drag.  */
	private DragView mDragView;

	/** Who can receive drop events */
	private Map<Integer, DropTarget> mDropTargets = new HashMap<Integer, DropTarget>();

	private DragListener mListener;

	/** The window token used as the parent for the DragView. */
	private IBinder mWindowToken;

	private View mMoveTarget;

	private DropTarget mLastDropTarget;

	private InputMethodManager mInputMethodManager;

	private VisibleChildDetectableHorizontalScrollView adapterView;

	/** ListView의 길이를 몇 등분으로 나눌지. 이 등분으로 상단과 하단 경계선을 정한다. */
	private final int divider = 4;

	private int mWidth;
	private int mHeight;

	private int mRightBound;

	private int mLeftBound;

	private int scrollSpeed;

	private Thread timer;

	/** 타이머가 계속 실행되어야 하는지 여부 */
	private boolean shouldRun;

	private final int timerInterval = 50;

	/**
	 * Interface to receive notifications when a drag starts or stops
	 */
	interface DragListener {
		public void onDragStart(DragSource source, Object info, int dragAction);

		public void onDragEnd();
	}

	/**
	 * 생성자
	 * 
	 * @param context
	 * @param adapterView ListView 외의 객체를 주면 자동 스크롤 기능을 쓸 수 없다.
	 * ListView 객체를 주면 setAutoScrollable() 메소드를 사용해 자동 스크롤 기능을 쓸지 안 쓸지 정할 수 있다. 기본은 자동스크롤 가능이다.
	 */
	public HDragController(Context context, VisibleChildDetectableHorizontalScrollView adapterView) {
		mContext = context;
		mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

		this.adapterView = adapterView;

		setAutoScrollable(true);
	}

	/**
	 * 드래그를 시작한다.
	 * 이것은 드래그되는 뷰의 비트맵을 만든다. 당신이 보고 있는 움직이는 것이 비트맵이다.
	 * 실제 뷰는 onDrop 이벤트를 통해 위치를 재조정할 수 있다. 
	 * 
	 * @param v The view that is being dragged
	 * @param source An object representing where the drag originated
	 * @param dragInfo The data associated with the object that is being dragged
	 * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
	 *        {@link #DRAG_ACTION_COPY}
	 */
	public void startDrag(View v, DragSource source, Object dragInfo, int dragAction) {
		mOriginator = v;

		Bitmap b = getViewBitmap(v);

		if (b == null) {
			Log.e(TAG, "can't make bitmap of originator.");

			return;
		}

		int[] loc = mCoordinatesTemp;
		v.getLocationOnScreen(loc);
		int screenX = loc[0];
		int screenY = loc[1];

		startDrag(b, screenX, screenY, 0, 0, b.getWidth(), b.getHeight(),
			source, dragInfo, dragAction);

		b.recycle();

		if (dragAction == DRAG_ACTION_MOVE) {
			v.setVisibility(View.INVISIBLE);

			((View)v.getParent()).setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Starts a drag.
	 * 
	 * @param b The bitmap to display as the drag image.  It will be re-scaled to the
	 *          enlarged size.
	 * @param screenX The x position on screen of the left-top of the bitmap.
	 * @param screenY The y position on screen of the left-top of the bitmap.
	 * @param textureLeft The left edge of the region inside b to use.
	 * @param textureTop The top edge of the region inside b to use.
	 * @param textureWidth The width of the region inside b to use.
	 * @param textureHeight The height of the region inside b to use.
	 * @param source An object representing where the drag originated
	 * @param dragInfo The data associated with the object that is being dragged
	 * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
	 *        {@link #DRAG_ACTION_COPY}
	 */
	public void startDrag(Bitmap b, int screenX, int screenY,
			int textureLeft, int textureTop, int textureWidth, int textureHeight,
			DragSource source, Object dragInfo, int dragAction) {
		if (PROFILE_DRAWING_DURING_DRAG) {
			android.os.Debug.startMethodTracing("Launcher");
		}

		// Hide soft keyboard, if visible
		if (mInputMethodManager == null) {
			mInputMethodManager = (InputMethodManager)
					mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		}
		mInputMethodManager.hideSoftInputFromWindow(mWindowToken, 0);

		if (mListener != null) {
			mListener.onDragStart(source, dragInfo, dragAction);
		}

		int registrationX = ((int)mMotionDownX) - screenX;
		int registrationY = ((int)mMotionDownY) - screenY;

		mTouchOffsetX = mMotionDownX - screenX;
		mTouchOffsetY = mMotionDownY - screenY;

		mDragging = true;
		mDragSource = source;
		mDragInfo = dragInfo;

		mVibrator.vibrate(VIBRATE_DURATION);

		DragView dragView = mDragView = new DragView(mContext, b, registrationX, registrationY,
			textureLeft, textureTop, textureWidth, textureHeight);
		dragView.show(mWindowToken, (int)mMotionDownX, (int)mMotionDownY);
	}

	/**
	 * Draw the view into a bitmap.
	 */
	private Bitmap getViewBitmap(View v) {
		v.clearFocus();
		v.setPressed(false);

		boolean willNotCache = v.willNotCacheDrawing();
		v.setWillNotCacheDrawing(false);

		// Reset the drawing cache background color to fully transparent
		// for the duration of this operation
		int color = v.getDrawingCacheBackgroundColor();
		v.setDrawingCacheBackgroundColor(0);

		if (color != 0) {
			v.destroyDrawingCache();
		}
		v.buildDrawingCache();
		Bitmap cacheBitmap = v.getDrawingCache();
		if (cacheBitmap == null) {
			Log.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
			return null;
		}

		Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

		// Restore the view
		v.destroyDrawingCache();
		v.setWillNotCacheDrawing(willNotCache);
		v.setDrawingCacheBackgroundColor(color);

		return bitmap;
	}

	/**
	 * Call this from a drag source view like this:
	 *
	 * <pre>
	 *  @Override
	 *  public boolean dispatchKeyEvent(KeyEvent event) {
	 *      return mDragController.dispatchKeyEvent(this, event)
	 *              || super.dispatchKeyEvent(event);
	 * </pre>
	 */
	public boolean dispatchKeyEvent(KeyEvent event) {
		return mDragging;
	}

	/**
	 * Stop dragging without dropping.
	 */
	public void cancelDrag() {
		endDrag();
	}

	/**
	 * View가 재활용되는 상황에서는, 드래그가 끝났을 때 mOriginator 값이 원래 드래그를 시작한 뷰가 아니기 때문에 mOriginator 뷰를 보이게 하는 것만으로는 부족하다.
	 * 원래 드래그를 시작한 position을 mDragInfo에 넣기 때문에 mDragInfo값에 해당하는 뷰를 보이게 만들어야 한다.
	 */
	private void endDrag() {
		Log.d(TAG, "endDrag()");

		if (mDragging) {
			mDragging = false;
			if (mOriginator != null) {
				mOriginator.setVisibility(View.VISIBLE);

				((View)mOriginator.getParent()).setVisibility(View.VISIBLE);
			}

			if (mDragInfo != null) {
				DropTarget target = mDropTargets.get(mDragInfo);
				if (target != null) {
					ImageCell cell = (ImageCell)target;

					cell.setVisibility(View.VISIBLE);

					((View)cell.getParent()).setVisibility(View.VISIBLE);
				}
			}

			if (mListener != null) {
				mListener.onDragEnd();
			}

			if (mDragView != null) {
				mDragView.remove();
				mDragView = null;
			}

			// todo
			// DragSource?
		}
	}

	/**
	 * Call this from a drag source view.
	 */
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();

		if (action == MotionEvent.ACTION_DOWN) {
			recordScreenSize();
		}

		final int screenX = clamp((int)ev.getRawX(), 0, mDisplayMetrics.widthPixels);
		final int screenY = clamp((int)ev.getRawY(), 0, mDisplayMetrics.heightPixels);

		switch (action) {
			case MotionEvent.ACTION_MOVE:
				break;

			case MotionEvent.ACTION_DOWN:
				//				if (adapterView instanceof ListView) {
				//					mWidth = adapterView.getHeight();
				//				}
				mWidth = adapterView.getWidth();
				mHeight = adapterView.getHeight();

				// Remember location of down touch
				mMotionDownX = screenX;
				mMotionDownY = screenY;
				mLastDropTarget = null;
				break;

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "onInterceptTouchEvent() ACTION_UP or ACTION_CANCEL");

				if (mDragging) {
					drop(screenX, screenY);
				}
				endDrag();
				break;
		}

		return mDragging;
	}

	/**
	 * Sets the view that should handle move events.
	 */
	void setMoveTarget(View view) {
		mMoveTarget = view;
	}

	public boolean dispatchUnhandledMove(View focused, int direction) {
		return mMoveTarget != null && mMoveTarget.dispatchUnhandledMove(focused, direction);
	}

	/**
	 * Call this from a drag source view.
	 */
	public boolean onTouchEvent(MotionEvent ev) {
		if (!mDragging) {
			return false;
		}

		final int action = ev.getAction();
		final int screenX = clamp((int)ev.getRawX(), 0, mDisplayMetrics.widthPixels);
		final int screenY = clamp((int)ev.getRawY(), 0, mDisplayMetrics.heightPixels);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				// Remember where the motion event started
				mMotionDownX = screenX;
				mMotionDownY = screenY;
				break;

			case MotionEvent.ACTION_MOVE:
				// Update the drag view.  Don't use the clamped pos here so the dragging looks
				// like it goes off screen a little, intead of bumping up against the edge.
				mDragView.move((int)ev.getRawX(), (int)ev.getRawY());

				// Drop on someone?
				final int[] coordinates = mCoordinatesTemp;
				DropTarget dropTarget = findDropTarget(screenX, screenY, coordinates);
				fireEvent(coordinates, dropTarget);
				mLastDropTarget = dropTarget;

				if (isAutoScrollable) {
					autoScroll(ev);
				}
				break;

			case MotionEvent.ACTION_UP:
				Log.d(TAG, "onTouchEvent() ACTION_UP");

				if (mDragging) {
					drop(screenX, screenY);
				}
				endDrag();

				stopAutoScroll();
				break;

			case MotionEvent.ACTION_CANCEL:
				Log.d(TAG, "onTouchEvent() ACTION_CANCEL");

				cancelDrag();

				stopAutoScroll();
				break;
		}

		return true;
	}

	DropTarget enteredCell;

	/**
	 * @param coordinates
	 * @param dropTarget
	 */
	private void fireEvent(final int[] coordinates, DropTarget dropTarget) {
		if (dropTarget != null) {
			if (mLastDropTarget == dropTarget) {
				dropTarget.onDragOver(mDragSource, coordinates[0], coordinates[1],
					(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo);
			} else {
				if (mLastDropTarget != null) {
					mLastDropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
						(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo);
				}
				dropTarget.onDragEnter(mDragSource, coordinates[0], coordinates[1],
					(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo);

				enteredCell = dropTarget;
			}
		} else {
			if (mLastDropTarget != null) {
				mLastDropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
					(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo);
			}
		}
	}

	private void stopAutoScroll() {
		if (isAutoScrollable && autoScrollingType == AutoScrollingType.BY_TIMER) {
			shouldRun = false;
		}
	}

	private void autoScroll(MotionEvent ev) {
		int x = (int)ev.getX();
		int y = (int)ev.getY();

		scrollSpeed = 0;

		adjustScrollBounds(x/*y*/);

		calulateScrollSpeed(x/*y*/);

		if (autoScrollingType == AutoScrollingType.BY_TOUCH_MOVE) {
			autoScrollByTouch();
		} else if (autoScrollingType == AutoScrollingType.BY_TIMER) {
			autoScrollByTimer();
		}
	}

	private void calulateScrollSpeed(int x) {
		if (x > mLeftBound) {
			int criteria = (mWidth - mLeftBound) / divider;

			if (x < mLeftBound + criteria) {
				scrollSpeed = 8;
			} else if (x < mLeftBound + (criteria * 2)) {
				scrollSpeed = 16;
			} else if (x < mLeftBound + (criteria * 3)) {
				scrollSpeed = 32;
			} else {
				scrollSpeed = 64;
			}
		} else if (x < mRightBound) {
			int criteria = mRightBound / divider;

			if (x < mRightBound - (criteria * 3)) {
				scrollSpeed = -64;
			} else if (x < mRightBound - (criteria * 2)) {
				scrollSpeed = -32;
			} else if (x < mRightBound - criteria) {
				scrollSpeed = -16;
			} else {
				scrollSpeed = -8;
			}
		}

		Log.d(TAG, "calulateScrollSpeed(" + x + ") : " + scrollSpeed);
	}

	private void autoScrollByTouch() {
		if (scrollSpeed != 0) {
			adapterView.scrollBy(scrollSpeed, 0);
		}
	}

	private void autoScrollByTimer() {
		if (scrollSpeed != 0) {
			if (!shouldRun) {
				shouldRun = true;
				if (timer == null) {
					timer = new Thread(new Timer());
					timer.start();
				}
			}
		} else {
			shouldRun = false;
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			autoScrollByTouch();
		}
	};

	private class Timer implements Runnable {
		@Override
		public void run() {
			try {
				while (shouldRun) {
					handler.sendMessage(handler.obtainMessage());

					Thread.sleep(timerInterval);
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	 * ListView의 전체 길이에서 상단과 하단 경계선을 얻는다. y좌표가 이 경계선을 넘어가면 자동 스크롤 된다.
	 * 
	 * @param x
	 */
	private void adjustScrollBounds(int x) {
		if (x >= mWidth / divider) {
			mRightBound = mWidth / divider;
		}
		if (x <= mWidth * (divider - 1) / divider) {
			mLeftBound = mWidth * (divider - 1) / divider;
		}
	}

	private boolean drop(float x, float y) {
		Log.d(TAG, "drop()");

		final int[] coordinates = mCoordinatesTemp;
		DropTarget dropTarget = findDropTarget((int)x, (int)y, coordinates);

		if (dropTarget != null) {
			/* 갤럭시S2와 같이 화면 바깥으로 드래그하면 자동으로 UP 이벤트가 발생되어 버리는 폰 같은 경우,
			 * 뷰가 재활용되는 상황에서(뷰가 재활용되는 것과 상관없을 수도 있다.)
			 * 이전에 onDragEnter() 메소드가 호출되엇을 때 dropTarget의 cellNumber가 x였는데,
			 * 순간 뷰가 재활용되면서 아래 onDragExit() 메소드 호출시 dropTarget의 cellNumber가 y가 되어 버릴수 있다.
			 * 그러면, onDragEnter() 메소드에서 강조된 모양으로 바뀐 ImageCell을 원래 모양대로 바꿀 방법이 없다. onDragExit() 메소드가 호출되지 않으므로.
			 * 따라서 아래처럼 강제로 원래 모양으로 되돌린다.
			 * 다른 폰은 화면 바깥으로 드래그해도 계속 MOVE 이벤트가 발생해서 문제없다.
			 */
			if (enteredCell != null
				&& ((ImageCell)enteredCell).getCellNumber() != ((ImageCell)dropTarget).getCellNumber()) {
				((ImageCell)enteredCell).changeToInitialShape();
			}

			dropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
				(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo);
			if (dropTarget.acceptDrop(mDragSource, coordinates[0], coordinates[1],
				(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo)) {
				dropTarget.onDrop(mDragSource, coordinates[0], coordinates[1],
					(int)mTouchOffsetX, (int)mTouchOffsetY, mDragView, mDragInfo);
				mDragSource.onDropCompleted((View)dropTarget, true);
				return true;
			} else {
				mDragSource.onDropCompleted((View)dropTarget, false);
				return true;
			}
		} else {
			// dropTarget이 null인 경우에도 enteredCell을 원래 모양으로 바꾸어 주는 것을 잊지 마라.  
			if (enteredCell != null) {
				((ImageCell)enteredCell).changeToInitialShape();
			}
		}

		return false;
	}

	private DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
		final Rect r = mRectTemp;
		final Map<Integer, DropTarget> dropTargets = mDropTargets;
		Set<Integer> set = dropTargets.keySet();
		Iterator<Integer> iterator = set.iterator();

		while (iterator.hasNext()) {
			final DropTarget target = dropTargets.get(iterator.next());
			target.getHitRect(r);
			target.getLocationOnScreen(dropCoordinates);
			r.offset(dropCoordinates[0] - target.getLeft(), dropCoordinates[1] - target.getTop());

			if (r.contains(x, y)) {
				dropCoordinates[0] = x - dropCoordinates[0];
				dropCoordinates[1] = y - dropCoordinates[1];

				return target;
			}
		}

		return null;
	}

	/**
	 * Get the screen size so we can clamp events to the screen size so even if
	 * you drag off the edge of the screen, we find something.
	 */
	private void recordScreenSize() {
		((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE))
			.getDefaultDisplay().getMetrics(mDisplayMetrics);
	}

	/**
	 * Clamp val to be &gt;= min and &lt; max.
	 */
	private static int clamp(int val, int min, int max) {
		if (val < min) {
			return min;
		} else if (val >= max) {
			return max - 1;
		} else {
			return val;
		}
	}

	public void setWindowToken(IBinder token) {
		mWindowToken = token;
	}

	public void setDragListener(DragListener l) {
		mListener = l;
	}

	public void removeDragListener(DragListener l) {
		mListener = null;
	}

	public void addDropTarget(int position, DropTarget target) {
		mDropTargets.put(position, target);
	}

	public void removeDropTarget(Integer position) {
		mDropTargets.remove(position);
	}

	public void removeAllDropTargets() {
		mDropTargets = new HashMap<Integer, DropTarget>();
	}

	public AutoScrollingType getAutoScrollingType() {
		return autoScrollingType;
	}

	public void setAutoScrollingType(AutoScrollingType autoScrollingType) {
		this.autoScrollingType = autoScrollingType;
	}

	public boolean isAutoScrollable() {
		return isAutoScrollable;
	}

	/**
	 * 자동 스크롤 기능을 사용할 것인지 설정한다.
	 * 단, DragController 생성시 ListView 외의 객체를 인수로 주었다면, 무조건 스크롤 불가능한 것으로 설정한다.
	 * ListView만이 스크롤 가능하기 때문이다. GridView 불가.
	 *   
	 * @param isAutoScrollable
	 */
	public void setAutoScrollable(boolean isAutoScrollable) {
		this.isAutoScrollable = isAutoScrollable;
	}

	public VisibleChildDetectableHorizontalScrollView getAdapterView() {
		return this.adapterView;
	}
}
