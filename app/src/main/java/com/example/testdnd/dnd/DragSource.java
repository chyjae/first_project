/*
 * @(#)DragSource.java $version 2011. 11. 18.
 *
 * Copyright 2011 NAVER JAPAN. All rights Reserved. 
 * NAVER JAPAN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.example.testdnd.dnd;

import android.view.View;

/**
 * 드래그 동작의 소스가 되는 인터페이스
 * 
 * @author 박성현
 *
 */
public interface DragSource {
	void setDragController(HDragController dragger);

	void onDropCompleted(View target, boolean success);
}
