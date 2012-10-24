/*
 * @(#)ImageCell.java $version 2011. 10. 7.
 *
 * Copyright 2011 NAVER JAPAN. All rights Reserved. 
 * NAVER JAPAN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.example.testdnd.dnd;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.testdnd.R;
import com.example.testdnd.VisibleChildDetectableHorizontalScrollView;

/**
 * ImageCell은 자신이 어댑터에서 어느 위치에 해당하는지 cellNumber를 통해 알 수 있다. 또한 자신이 비었는지를 empty를
 * 통해 안다.
 * 
 * ImageCell은 이미지가 드래그되는 장소이고 드롭되는 장소이기 때문에 DragSource와 DropTarget 인터페이스 모두를
 * 구현한다.
 * 
 * @author 박성현
 */
public class ImageCell extends ImageView implements DragSource, DropTarget {
	private static final String TAG = "DragAndDrop";

	private int cellNumber = -1;
	private boolean empty;

	private int originalSize;
	private int enteringSize;

	private HDragController dragController;

	public ImageCell(Context context) {
		super(context);

		init();
	}

	public ImageCell(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	public ImageCell(Context context, AttributeSet attrs, int style) {
		super(context, attrs, style);

		init();
	}

	private void init() {
		enteringSize = getResources().getDimensionPixelSize(
			R.dimen.npa_order_change_photo_entering_size);
		originalSize = getResources().getDimensionPixelSize(
			R.dimen.npa_order_change_photo_original_size);
	}

	public int getCellNumber() {
		return cellNumber;
	}

	public void setCellNumber(int cellNumber) {
		this.cellNumber = cellNumber;
	}

	@Override
	public void setDragController(HDragController dragController) {
		this.dragController = dragController;
	}

	@Override
	public void onDropCompleted(View target, boolean success) {
		Log.d(TAG, "onDropCompleted() target cellNumber: "
			+ ((ImageCell)target).cellNumber);

		if (success) {
			if (cellNumber >= 0) {
			}
		}
	}

	@Override
	public void onDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, DragView dragView, Object dragInfo) {
		Log.d(TAG, "onDrop() source cellNumber: "
			+ ((ImageCell)source).cellNumber + ", dragInfo: " + dragInfo
			+ ", this.cellNumber: " + this.cellNumber);

		if (cellNumber != (Integer)dragInfo) {
			VisibleChildDetectableHorizontalScrollView adapter = dragController.getAdapterView();

			adapter.changeOrder((Integer)dragInfo, this.cellNumber);
			adapter.updateViewsInLayout();

			// 아래 부분들을 인터페이스 기반으로 빼서 구현하거나 콜백 메소드나 다른 클래스로 들어가는게 더 좋을지도 모르겠다.
			// TODO
			// PhotoColumnMappingAdapter adapter =
			// ((PhotoColumnMappingAdapter)dragController.getAdapterView().getAdapter());
			// adapter.changeOrder((Integer)dragInfo, this.cellNumber);
			// adapter.notifyDataSetChanged();
			//
			// ((PhotoOrderChangeActivity)getContext()).appendHistoryToBulk(cellNumber);
		}
	}

	@Override
	public void onDragEnter(DragSource source, int x, int y, int xOffset,
			int yOffset, DragView dragView, Object dragInfo) {
		Log.d(TAG, "onDragEnter() source cellNumber: "
			+ ((ImageCell)source).cellNumber + ", dragInfo: " + dragInfo
			+ ", this.cellNumber: " + this.cellNumber);

		if (cellNumber != (Integer)dragInfo) {
			changeToEmphasizedShape();
		}
	}

	@Override
	public void onDragOver(DragSource source, int x, int y, int xOffset,
			int yOffset, DragView dragView, Object dragInfo) {
		Log.d(TAG, "onDragOver() source cellNumber: "
			+ ((ImageCell)source).cellNumber + ", dragInfo: " + dragInfo
			+ ", this.cellNumber: " + this.cellNumber);
	}

	@Override
	public void onDragExit(DragSource source, int x, int y, int xOffset,
			int yOffset, DragView dragView, Object dragInfo) {
		Log.d(TAG, "onDragExit() source cellNumber: "
			+ ((ImageCell)source).cellNumber + ", dragInfo: " + dragInfo
			+ ", this.cellNumber: " + this.cellNumber);

		if (cellNumber != (Integer)dragInfo) {
			changeToInitialShape();
		}
	}

	@Override
	public boolean acceptDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, DragView dragView, Object dragInfo) {
		return cellNumber >= 0 && !empty;
	}

	@Override
	public Rect estimateDropLocation(DragSource source, int x, int y,
			int xOffset, int yOffset, DragView dragView, Object dragInfo,
			Rect recycle) {
		return null;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

	@Override
	public boolean performClick() {
		if (!empty) {
			return super.performClick();
		}

		return false;
	}

	@Override
	public boolean performLongClick() {
		if (!empty) {
			return super.performLongClick();
		}

		return false;
	}

	/**
	 * 아이템을 강조된 모양으로 바꾼다. 드래그되는 source가 이 아이템위로 올라갔을 경우 아래와 같은 모양으로 바뀜.
	 */
	public void changeToEmphasizedShape() {
		RelativeLayout parentView = (RelativeLayout)getParent();

		// 이 뷰가 아닌 부모 레이아웃의 배경색을 바꾼다.
		parentView
			.setBackgroundResource(R.drawable.npa_myalbum_photo_order_change_rectangle);

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getLayoutParams();

		params.width = enteringSize;
		params.height = enteringSize;

		setAlpha(127);

		setLayoutParams(params);
	}

	/**
	 * 아이템을 원래 모양으로 바꾼다.
	 */
	public void changeToInitialShape() {
		RelativeLayout parentView = (RelativeLayout)getParent();

		int imgResId = R.drawable.npa_album_thumbnail_frame;
		parentView.setBackgroundResource(imgResId);

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getLayoutParams();

		params.width = originalSize;
		params.height = originalSize;

		setAlpha(255);

		setLayoutParams(params);
	}
}
