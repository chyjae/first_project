package com.example.testdnd;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

/**
 * @author Choi Yunjae (KR15548, yunjae.choi@nhn.com)
 *
 */
public class VisibleChildDetectableHorizontalScrollView extends HorizontalScrollView {
	private static final String TAG = "HorizontalScrollView";
	private OnVisibleItemChangedListener onVisibleItemChangedListener;
	private LinearLayout layout;

	/**
	 * View.mAttachInfo가 있을 때 post() 메소드가 제대로 동작한다.
	 * onAttachToWindow() 메소드가 실행된 후에, 이전에 들어온 요청이 있으면 수행해준다.
	 */
	private boolean isResetScrollTaskPending = false;
	private Runnable resetScrollTask = new Runnable() {
		@Override
		public void run() {
			isResetScrollTaskPending = false;

			doResetScroll();
		}
	};

	private int fixedItemWidth = -1;
	private int fixedMarginLeft = -1;
	private int fixedScrollViewWidth = -1;

	public VisibleChildDetectableHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public VisibleChildDetectableHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public VisibleChildDetectableHorizontalScrollView(Context context) {
		super(context);
		init();
	}

	private void init() {
		this.setHorizontalScrollBarEnabled(false);

		initInnerLayout();
	}

	private void initInnerLayout() {
		this.layout = new LinearLayout(getContext());
		this.addView(layout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
	}

	public void enableFixedWidthMode(int itemWidth, int marginLeft, int scrollViewWidth) {
		this.fixedItemWidth = itemWidth;
		this.fixedMarginLeft = marginLeft;
		this.fixedScrollViewWidth = scrollViewWidth;
	}

	public void disableFixedWidthMode() {
		this.fixedItemWidth = -1;
		this.fixedMarginLeft = -1;
		this.fixedScrollViewWidth = -1;
	}

	public void reset() {
		this.removeAllViews();
		init();
	}

	public void addChild(View child) {
		layout.addView(child);
	}

	public LinearLayout getLayout() {
		return layout;
	}

	public void setSelection(int position) {
		int offsetX = 0;
		int len = Math.min(position, getGrandChildCount() - 1);

		for (int i = 0; i < len; i++) {
			View child = getGrandChildAt(i);
			offsetX += child.getWidth();
		}

		this.scrollTo(offsetX, 0);
	}

	public void setOnVisibleItemChangedListener(OnVisibleItemChangedListener onVisibleItemChangedListener) {
		this.onVisibleItemChangedListener = onVisibleItemChangedListener;
	}

	public void removeGrandChildAt(int index) {
		View v = getGrandChildAt(index);
		getLayout().removeView(v);
	}

	public View getGrandChildAt(int index) {
		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
		if (mainView == null || mainView.getChildCount() <= index) {
			return null;
		}

		return mainView.getChildAt(index);
	}

	public int getGrandChildCount() {
		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
		if (mainView == null) {
			return 0;
		}

		return mainView.getChildCount();
	}

	public void resetScroll() {
		if (this.getChildCount() <= 0) {
			return;
		}

		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
		if (mainView == null) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("width=").append(getWidth()).append(", mainView.getWidth()=").append(mainView.getWidth());

		boolean isLayoutFinished = (this.getWidth() > 0) && (mainView.getWidth() > 0);
		if (isLayoutFinished) {
			for (int i = 0; i < mainView.getChildCount(); i++) {
				View child = mainView.getChildAt(i);
				int width = child.getWidth();
				sb.append(width).append(", ");

				if (width <= 0) {
					isLayoutFinished = false;
					break;
				}
			}
		}
		Log.d("HorizontalScrollView", "resetScroll() : " + sb.toString());

		if (mainView.getWidth() > 0 && this.getWidth() > 0 && isLayoutFinished) {
			// [Choi Yunjae] width가 계산되었다면, 즉 measure가 끝난 상태라면 바로 처리해도 된다.
			doResetScroll();
		} else {
			postResetScroll();
		}
	}

	private void postResetScroll() {
		isResetScrollTaskPending = true;

		this.post(resetScrollTask);
	}

	private void doResetScroll() {
		this.scrollTo(0, 0);
		this.triggerOnVisibleItemChanged(0);
	}

	@Override
	protected void onAttachedToWindow() {
		Log.d(TAG, "++onAttachedToWindow()");
		super.onAttachedToWindow();

		if (isResetScrollTaskPending) {
			this.removeCallbacks(resetScrollTask);
			postResetScroll();
		}

		Log.d(TAG, "--onAttachedToWindow()");
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		getParent().requestDisallowInterceptTouchEvent(true);

		triggerOnVisibleItemChanged(l);

		super.onScrollChanged(l, t, oldl, oldt);
	}

	public void updateViewsInLayout() {
		triggerOnVisibleItemChanged(this.getScrollX());
	}

	public void triggerOnVisibleItemChanged(int newX) {
		ViewGroup mainView = (ViewGroup)this.getChildAt(0);

		boolean isFixedWidthMode = (fixedItemWidth > 0);
		int scrollViewWidth = this.getWidth();
		int widthTotal = 0;
		int widthVisible = 0;
		int firstVisibleItemIndex = -1;
		int lastVisibleItemIndex = -1;

		if (mainView == null || mainView.getChildCount() <= 0) {
			return;
		}

		if (onVisibleItemChangedListener == null) {
			return;
		}

		if (isFixedWidthMode) {
			scrollViewWidth = fixedScrollViewWidth;
			widthTotal += fixedMarginLeft;
		}

		for (int i = 0; i < mainView.getChildCount(); i++) {
			int width = isFixedWidthMode ? fixedItemWidth : mainView.getChildAt(i).getWidth();

			widthTotal += width;

			if (firstVisibleItemIndex < 0) {
				if (newX <= widthTotal) {
					// find first visible index
					firstVisibleItemIndex = i;
					widthVisible = widthTotal - newX;
				}
			} else {
				// find last visible index
				widthVisible += width;
				if (widthVisible >= scrollViewWidth) {
					lastVisibleItemIndex = i;
					break;
				}
			}
		}

		if (lastVisibleItemIndex < 0) {
			// overscroll
			lastVisibleItemIndex = mainView.getChildCount() - 1;
		}

		Log.d(TAG, "triggerOnVisibleItemChanged(" + newX + ") : " + firstVisibleItemIndex + "~"
			+ lastVisibleItemIndex);

		// trigger
		if (firstVisibleItemIndex < 0) {
			firstVisibleItemIndex = 0;
		}

		onVisibleItemChangedListener.onVisibleItemChanged(this, firstVisibleItemIndex, lastVisibleItemIndex);
	}

	public static String getActionName(int action) {
		switch (action) {
			case MotionEvent.ACTION_CANCEL:
				return "ACTION_CANCEL";
			case MotionEvent.ACTION_DOWN:
				return "ACTION_DOWN";
			case MotionEvent.ACTION_MOVE:
				return "ACTION_MOVE";
			case MotionEvent.ACTION_OUTSIDE:
				return "ACTION_OUTSIDE";
			case MotionEvent.ACTION_UP:
				return "ACTION_UP";
			default:
				return String.valueOf(action);
		}
	}

	public static interface OnVisibleItemChangedListener {
		public void onVisibleItemChanged(VisibleChildDetectableHorizontalScrollView hScrollView, int first, int last);
	}
}

//
//import android.content.Context;
//import android.util.AttributeSet;
//import android.util.Log;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.HorizontalScrollView;
//
//public class VisibleChildDetectableHorizontalScrollView extends HorizontalScrollView {
//	private static final String TAG = "HorizontalScrollView";
//	private OnVisibleItemChangedListener onVisibleItemChangedListener;
//	public static final int INVALID_POSITION = -1;
//
//	public VisibleChildDetectableHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
//		super(context, attrs, defStyle);
//		init();
//	}
//
//	public VisibleChildDetectableHorizontalScrollView(Context context, AttributeSet attrs) {
//		super(context, attrs);
//		init();
//	}
//
//	public VisibleChildDetectableHorizontalScrollView(Context context) {
//		super(context);
//		init();
//	}
//
//	private void init() {
//		this.setFocusable(false);
//		this.setHorizontalScrollBarEnabled(false);
//	}
//
//	public void setOnVisibleItemChangedListener(OnVisibleItemChangedListener onVisibleItemChangedListener) {
//		this.onVisibleItemChangedListener = onVisibleItemChangedListener;
//	}
//
//	public View getGrandChildAt(int index) {
//		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
//		if (mainView == null || mainView.getChildCount() <= index) {
//			return null;
//		}
//
//		return mainView.getChildAt(index);
//	}
//
//	public int getGrandChildCount() {
//		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
//		if (mainView == null) {
//			return 0;
//		}
//
//		return mainView.getChildCount();
//	}
//
//	public void resetScroll() {
//		if (this.getChildCount() <= 0) {
//			return;
//		}
//
//		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
//		if (mainView == null) {
//			return;
//		}
//
//		StringBuilder sb = new StringBuilder();
//		sb.append("width=").append(getWidth()).append(", mainView.getWidth()=").append(mainView.getWidth()).append("\nchildren : ");
//
//		boolean isLayoutFinished = (this.getWidth() > 0) && (mainView.getWidth() > 0);
//		if (isLayoutFinished) {
//			for (int i = 0; i < mainView.getChildCount(); i++) {
//				View child = mainView.getChildAt(i);
//				int width = child.getWidth();
//				sb.append(width).append(", ");
//
//				if (width <= 0) {
//					isLayoutFinished = false;
//					break;
//				}
//			}
//		}
//		Log.d("HorizontalScrollView", "resetScroll() : " + sb.toString());
//
//		if (mainView.getWidth() > 0 && this.getWidth() > 0 && isLayoutFinished) {
//			// [Choi Yunjae] width가 계산되었다면, 즉 measure가 끝난 상태라면 바로 처리해도 된다.
//			doResetScroll();
//		} else {
//			postResetScroll();
//		}
//	}
//
//	private void postResetScroll() {
//		this.post(new Runnable() {
//			public void run() {
//				doResetScroll();
//			}
//		});
//	}
//
//	private void doResetScroll() {
//		this.scrollTo(0, 0);
//		this.triggerOnVisibleItemChanged(0);
//	}
//
//	@Override
//	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
//		triggerOnVisibleItemChanged(l);
//
//		super.onScrollChanged(l, t, oldl, oldt);
//	}
//
//	public void triggerOnVisibleItemChanged(int newX) {
//		ViewGroup mainView = (ViewGroup)this.getChildAt(0);
//
//		int scrollViewWidth = this.getWidth();
//		int widthTotal = 0;
//		int widthVisible = 0;
//		int firstVisibleItemIndex = -1;
//		int lastVisibleItemIndex = -1;
//		for (int i = 0; i < mainView.getChildCount(); i++) {
//			View child = mainView.getChildAt(i);
//			int width = child.getWidth();
//
//			widthTotal += width;
//
//			if (firstVisibleItemIndex < 0) {
//				if (newX <= widthTotal) {
//					// find first visible index
//					firstVisibleItemIndex = i;
//					widthVisible = widthTotal - newX;
//				}
//			} else {
//				// find last visible index
//				widthVisible += width;
//				if (widthVisible >= scrollViewWidth) {
//					lastVisibleItemIndex = i;
//					break;
//				}
//			}
//		}
//
//		if (lastVisibleItemIndex < 0) {
//			// overscroll
//			lastVisibleItemIndex = mainView.getChildCount() - 1;
//		}
//
//		Log.d(TAG, "triggerOnVisibleItemChanged(" + newX + ") : " + firstVisibleItemIndex + "~"
//			+ lastVisibleItemIndex);
//
//		// trigger
//		if (onVisibleItemChangedListener != null) {
//			onVisibleItemChangedListener.onVisibleItemChanged(firstVisibleItemIndex, lastVisibleItemIndex);
//		}
//	}
//
//	public static String getActionName(int action) {
//		switch (action) {
//			case MotionEvent.ACTION_CANCEL:
//				return "ACTION_CANCEL";
//			case MotionEvent.ACTION_DOWN:
//				return "ACTION_DOWN";
//			case MotionEvent.ACTION_MOVE:
//				return "ACTION_MOVE";
//			case MotionEvent.ACTION_OUTSIDE:
//				return "ACTION_OUTSIDE";
//			case MotionEvent.ACTION_UP:
//				return "ACTION_UP";
//			default:
//				return String.valueOf(action);
//		}
//	}
//
//	public static interface OnVisibleItemChangedListener {
//		public void onVisibleItemChanged(int first, int last);
//	}
//
//	// TODO-------------------------
//	public int pointToPosition(int x, int y) {
//		return INVALID_POSITION;
//	}
//
//	public int getFirstVisiblePosition() {
//		return INVALID_POSITION;
//	}
//}
