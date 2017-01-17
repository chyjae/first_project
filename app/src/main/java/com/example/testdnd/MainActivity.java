package com.example.testdnd;

import java.util.LinkedList;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.testdnd.dnd.DragLayer;
import com.example.testdnd.dnd.DragSource;
import com.example.testdnd.dnd.HDragController;
import com.example.testdnd.dnd.ImageCell;

public class MainActivity extends Activity implements View.OnLongClickListener {
	private Gallery gallery;
	private LinkedList<Item> itemList;
	private GalleryAdapter adapter;
	private HDragController dragController;
	private DragLayer dragLayer;

	private VisibleChildDetectableHorizontalScrollView hScrollView;

	//	private LinearLayout innerLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initData();
		//		initGallery();
		initHorizontalScrollView();
		//		initDragLayer();
	}

	private void initData() {
		this.itemList = new LinkedList<Item>();

		for (int i = 0; i < 20; i++) {
			Item item = new Item(R.drawable.ic_launcher, String.format("Item_%02d", i));
			itemList.add(item);
		}
	}

	private void initGallery() {
		//		this.gallery = (Gallery)findViewById(R.id.gallery);
		//		this.dragLayer = (DragLayer)findViewById(R.id.dragLayer);
		//
		//		this.dragController = new HDragController(this, gallery);
		//		dragLayer.setDragController(dragController);
		//		dragController.setDragListener(dragLayer);
		//
		//		this.adapter = new GalleryAdapter();
		//		gallery.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	class GalleryAdapter extends BaseAdapter {
		public GalleryAdapter() {
			super();
		}

		@Override
		public int getCount() {
			return itemList.size();
		}

		@Override
		public Object getItem(int position) {
			return itemList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = View.inflate(parent.getContext(), R.layout.list_item_cell, null);
			ImageCell imageCell = (ImageCell)view.findViewById(R.id.npa_myalbum_photolist_order_change_image);

			imageCell.cellNumber = position;
			imageCell.empty = false;

			imageCell.setDragController(dragController);

			imageCell.setOnLongClickListener(MainActivity.this);

			// 드래그중일때는 드래그를 시작한 source는 안 보이는 상태로 있어야 한다. view가 재활용되기 때문 필요
			if (dragController.mDragging) {
				int dragSourcePosition = dragController.mDragInfo != null ? (Integer)dragController.mDragInfo : -1;
				if (dragSourcePosition != -1) {
					if (dragSourcePosition == position) {
						imageCell.setVisibility(View.INVISIBLE);

						((View)imageCell.getParent()).setVisibility(View.INVISIBLE);
					} else {
						imageCell.setVisibility(View.VISIBLE);

						((View)imageCell.getParent()).setVisibility(View.VISIBLE);
					}
				}
			}

			dragController.addDropTarget(position, imageCell);

			return view;

			//			View view = convertView;
			//			if (view == null) {
			//				view = View.inflate(parent.getContext(), R.layout.list_item_gallery, null);
			//				view = Holder.getHolderAttached(view);
			//			}
			//
			//			Item item = itemList.get(position);
			//
			//			Holder holder = (Holder)view.getTag();
			//			holder.image.setImageResource(item.getResId());
			//			holder.text.setText(item.getName());
			//
			//			return view;
		}
	}

	static class Holder {
		ImageView image;
		TextView text;

		static Holder newHolder(View v) {
			Holder holder = new Holder();
			holder.image = (ImageView)v.findViewById(R.id.image);
			holder.text = (TextView)v.findViewById(R.id.text);
			return holder;
		}

		static View getHolderAttached(View v) {
			v.setTag(newHolder(v));

			return v;
		}
	}

	static class Item {
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
		DragSource dragSource = (DragSource)view;

		Integer position = new Integer(((ImageCell)view).cellNumber);

		dragController.startDrag(view, dragSource, position, HDragController.DRAG_ACTION_MOVE);

		return true;
	}

	private void initHorizontalScrollView() {

		this.hScrollView = (VisibleChildDetectableHorizontalScrollView)findViewById(R.id.scrollView);
		//		this.innerLayout = (LinearLayout)hScrollView.findViewById(R.id.linearLayout);

		this.dragLayer = (DragLayer)findViewById(R.id.dragLayer);
		this.dragController = new HDragController(this, hScrollView);

		dragLayer.setDragController(dragController);
		dragController.setDragListener(dragLayer);

		for (int i = 0; i < itemList.size(); i++) {
			Item item = itemList.get(i);

			//			View view = View.inflate(this, R.layout.list_item_gallery, null);
			//
			//			Holder holder = Holder.newHolder(view);
			//			holder.image.setImageResource(item.getResId());
			//			holder.text.setText(item.getName());

			View view = View.inflate(this, R.layout.list_item_cell, null);
			ImageCell imageCell = (ImageCell)view.findViewById(R.id.npa_myalbum_photolist_order_change_image);

			imageCell.cellNumber = i;
			imageCell.empty = false;

			imageCell.setDragController(dragController);

			imageCell.setOnLongClickListener(this);

			// 드래그중일때는 드래그를 시작한 source는 안 보이는 상태로 있어야 한다. view가 재활용되기 때문 필요
			if (dragController.mDragging) {
				int dragSourcePosition = dragController.mDragInfo != null ? (Integer)dragController.mDragInfo : -1;
				if (dragSourcePosition != -1) {
					if (dragSourcePosition == i) {
						imageCell.setVisibility(View.INVISIBLE);

						((View)imageCell.getParent()).setVisibility(View.INVISIBLE);
					} else {
						imageCell.setVisibility(View.VISIBLE);

						((View)imageCell.getParent()).setVisibility(View.VISIBLE);
					}
				}
			}

			dragController.addDropTarget(i, imageCell);

			hScrollView.addChild(view);
			//			innerLayout.addView(view);
		}
	}
}
