/*
 * This is a modified version of a class from the Android Open Source Project. 
 * The original copyright and license information follows.
 * 
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.testdnd.dnd;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 드래깅을 지원하는 ViewGroup.
 * 드래깅은 DragSource 인터페이스를 구현한 객체에서 시작해서 DropTarget 인터페이스를 구현한 객체에서 끝난다.
 *
 * @author 박성현
 */
public class DragLayer extends FrameLayout implements HDragController.DragListener {
	HDragController mDragController;

	public DragLayer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setDragController(HDragController controller) {
		mDragController = controller;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return mDragController.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return mDragController.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		return mDragController.onTouchEvent(ev);
	}

	@Override
	public boolean dispatchUnhandledMove(View focused, int direction) {
		return mDragController.dispatchUnhandledMove(focused, direction);
	}

	/**
	 * 드래그가 시작되었을 때
	 * 
	 * @param source
	 * @param info
	 * @param dragAction 드래그 액션. {@link HDragController#DRAG_ACTION_MOVE} 또는 {@link HDragController#DRAG_ACTION_COPY}
	 */
	@Override
	public void onDragStart(DragSource source, Object dragInfo, int dragAction) {
	}

	/**
	 * 드래그 작업이 끝났을 때 
	 */
	@Override
	public void onDragEnd() {
	}
}
