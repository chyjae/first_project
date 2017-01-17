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

import android.graphics.Rect;

/**
 * 드래그되거나 드롭되는 객체에 대해 반응하는 객체를 정의하는 인터페이스.
 * 
 * @author 박성현
 */
public interface DropTarget {
	/**
	 * DropTarget으로 드롭되는 객체를 처리하기 위해 실행된다.
	 * DragSource는 뷰가 재활용되는 경우에는 변경될 수 있음을 기억하라.
	 * 
	 * @param source 드래그를 시작한 DragSource
	 * @param x 드롭 위치의 x 좌표
	 * @param y 드롭 위치의 y 좌표
	 * @param xOffset Horizontal offset with the object being dragged where the original
	 *          touch happened
	 * @param yOffset Vertical offset with the object being dragged where the original
	 *          touch happened
	 * @param dragView 화면상에 드래그되는 DragView
	 * @param dragInfo 드래그되는 객체와 연관된 데이터
	 * 
	 */
	void onDrop(DragSource source, int x, int y, int xOffset, int yOffset,
			DragView dragView, Object dragInfo);

	/**
	 * 드래그되는 객체가 DropTarget에 진입했을 때
	 * DragSource는 뷰가 재활용되는 경우에는 변경될 수 있음을 기억하라.
	 */
	void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
			DragView dragView, Object dragInfo);

	/**
	 * 드래그되는 객체가 DropTarget 위에 있을 때
	 * DragSource는 뷰가 재활용되는 경우에는 변경될 수 있음을 기억하라.
	 */
	void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
			DragView dragView, Object dragInfo);

	/**
	 * 드래그 되는 객체가 DropTarget을 벗어났을 때 
	 * DragSource는 뷰가 재활용되는 경우에는 변경될 수 있음을 기억하라.
	 */
	void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
			DragView dragView, Object dragInfo);

	/**
	 * 드롭 액션이 요청된 위치에서 수행될 수 있는지 검사한다.
	 * DragSource는 뷰가 재활용되는 경우에는 변경될 수 있음을 기억하라.
	 * 
	 * @param source
	 * @param x
	 * @param y
	 * @param xOffset
	 * @param yOffset
	 * @param dragView
	 * @param dragInfo
	 * @return 드롭이 가능하면 true, 불가능하면 false를 리턴한다.
	 */
	boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
			DragView dragView, Object dragInfo);

	/**
	 * Estimate the surface area where this object would land if dropped at the
	 * given location.
	 * 
	 * @param source
	 * @param x
	 * @param y
	 * @param xOffset
	 * @param yOffset
	 * @param dragView
	 * @param dragInfo
	 * @param recycle {@link Rect} object to be possibly recycled.
	 * @return Estimated area that would be occupied if object was dropped at
	 *         the given location. Should return null if no estimate is found,
	 *         or if this target doesn't provide estimations.
	 */
	Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
			DragView dragView, Object dragInfo, Rect recycle);

	void getHitRect(Rect outRect);

	void getLocationOnScreen(int[] loc);

	int getLeft();

	int getTop();
}
