package com.example.testdnd;

import java.util.LinkedList;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.example.testdnd.dnd.DragLayer;
import com.example.testdnd.dnd.DragSource;
import com.example.testdnd.dnd.HDragController;
import com.example.testdnd.dnd.ImageCell;

public class MainActivity extends Activity implements View.OnLongClickListener {
	private LinkedList<Item> itemList;
	private HDragController dragController;
	private DragLayer dragLayer;

	private VisibleChildDetectableHorizontalScrollView hScrollView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initData();
		initHorizontalScrollView();
	}

	private void initData() {
		this.itemList = new LinkedList<Item>();

		for (int i = 0; i < 20; i++) {
			Item item = new Item(R.drawable.ic_launcher, String.format(
					"Item_%02d", i));
			itemList.add(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public static class Item {
		int resId;
		String name;

		public Item(int resId, String name) {
			this.resId = resId;
			this.name = name;
		}

		public int getResId() {
			return resId;
		}

		public String getName() {
			return name;
		}

	}

	@Override
	public boolean onLongClick(View v) {
		if (!v.isInTouchMode()) {
			return false;
		}

		return startDrag(v);
	}

	public boolean startDrag(View view) {
		DragSource dragSource = (DragSource) view;

		Integer position = new Integer(((ImageCell) view).cellNumber);

		dragController.startDrag(view, dragSource, position,
				HDragController.DRAG_ACTION_MOVE);

		return true;
	}

	private void initHorizontalScrollView() {

		this.hScrollView = (VisibleChildDetectableHorizontalScrollView) findViewById(R.id.scrollView);

		this.dragLayer = (DragLayer) findViewById(R.id.dragLayer);
		this.dragController = new HDragController(this, hScrollView);

		dragLayer.setDragController(dragController);
		dragController.setDragListener(dragLayer);

		for (int i = 0; i < itemList.size(); i++) {
			Item item = itemList.get(i);

			View view = View.inflate(this, R.layout.list_item_cell, null);
			view.setTag(item);

			TextView textView = (TextView) view.findViewById(R.id.text);
			textView.setText(item.getName());

			ImageCell imageCell = (ImageCell) view
					.findViewById(R.id.npa_myalbum_photolist_order_change_image);
			imageCell.setImageResource(item.getResId());

			imageCell.cellNumber = i;
			imageCell.empty = false;

			imageCell.setDragController(dragController);
			imageCell.setOnLongClickListener(this);

			// 드래그중일때는 드래그를 시작한 source는 안 보이는 상태로 있어야 한다. view가 재활용되기 때문 필요
			if (dragController.mDragging) {
				int dragSourcePosition = dragController.mDragInfo != null ? (Integer) dragController.mDragInfo
						: -1;
				if (dragSourcePosition != -1) {
					if (dragSourcePosition == i) {
						imageCell.setVisibility(View.INVISIBLE);

						((View) imageCell.getParent())
								.setVisibility(View.INVISIBLE);
					} else {
						imageCell.setVisibility(View.VISIBLE);

						((View) imageCell.getParent())
								.setVisibility(View.VISIBLE);
					}
				}
			}

			dragController.addDropTarget(i, imageCell);
			hScrollView.addChild(view);
		}
	}
}
