package bd.org.notetaker.fragment.template;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import bd.org.notetaker.R;
import bd.org.notetaker.adapter.template.ModelAdapter;
import bd.org.notetaker.db.Controller;
import bd.org.notetaker.db.OpenHelper;
import bd.org.notetaker.inner.Animator;
import bd.org.notetaker.model.Category;
import bd.org.notetaker.model.DatabaseModel;
import bd.org.notetaker.model.Note;

abstract public class RecyclerFragment<T extends DatabaseModel, A extends ModelAdapter> extends Fragment {
	public View fab;
	private RecyclerView recyclerView;
	private View empty;
	public Toolbar selectionToolbar;
	private TextView selectionCounter;
	public boolean selectionState = false;
	private A adapter;
	public ArrayList<T> items;
	public ArrayList<T> selected = new ArrayList<>();
	public Callbacks activity;
	public long categoryId = DatabaseModel.NEW_MODEL_ID;
	public String categoryTitle;
	public int categoryTheme;
	public int categoryPosition = 0;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(getLayout(), container, false);
	}
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		fab = view.findViewById(R.id.fab);
		recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
		empty = view.findViewById(R.id.empty);
		selectionToolbar = (Toolbar) getActivity().findViewById(R.id.selection_toolbar);
		selectionCounter = (TextView) selectionToolbar.findViewById(R.id.selection_counter);
		init(view);
		selectionToolbar.findViewById(R.id.selection_back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				toggleSelection(false);
			}
		});
		selectionToolbar.findViewById(R.id.selection_delete).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final ArrayList<T> undos = new ArrayList<>();
				undos.addAll(selected);
				toggleSelection(false);
			}
		});
		if (fab != null) {
			fab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					onClickFab();
				}
			});
		}
		Intent data = getActivity().getIntent();
		if (data != null) {
			// Get the parent data
			categoryId = data.getLongExtra(OpenHelper.COLUMN_ID, DatabaseModel.NEW_MODEL_ID);
			categoryTitle = data.getStringExtra(OpenHelper.COLUMN_TITLE);
			categoryTheme = data.getIntExtra(OpenHelper.COLUMN_THEME, Category.THEME_GREEN);
			categoryPosition = data.getIntExtra("position", 0);
			if (categoryTitle != null) {
				((TextView) getActivity().findViewById(R.id.title)).setText(categoryTitle);
			}
		}
		loadItems();
	}
	public void onChangeCounter(int count) {
		selectionCounter.setText(String.format(Locale.US, "%d", count));
	}
	public void toggleSelection(boolean state) {
		selectionState = state;
		activity.onChangeSelection(state);
		if (state) {
			Animator.create(getContext())
				.on(selectionToolbar)
				.setStartVisibility(View.VISIBLE)
				.animate(R.anim.fade_in);
		} else {
			Animator.create(getContext())
				.on(selectionToolbar)
				.setEndVisibility(View.GONE)
				.animate(R.anim.fade_out);
			deselectAll();
		}
	}
	private void deselectAll() {
		while (!selected.isEmpty()) {
			adapter.notifyItemChanged(items.indexOf(selected.remove(0)));
		}
	}
	public void loadItems() {
	}
	private void toggleEmpty() {
		if (items.isEmpty()) {
			empty.setVisibility(View.VISIBLE);
			recyclerView.setVisibility(View.GONE);
		} else {
			empty.setVisibility(View.GONE);
			recyclerView.setVisibility(View.VISIBLE);
		}
	}
	public void refreshItem(int position) {
		adapter.notifyItemChanged(position);
	}
	public T deleteItem(int position) {
		T item = items.remove(position);
		adapter.notifyItemRemoved(position);
		toggleEmpty();
		return item;
	}
	public void addItem(T item, int position) {
		if (items.isEmpty()) {
			empty.setVisibility(View.GONE);
			recyclerView.setVisibility(View.VISIBLE);
		}
		items.add(position, item);
		adapter.notifyItemInserted(position);
	}
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		this.activity = (Callbacks) context;
	}
	public void init(View view) {}
	public abstract void onClickFab();
	public abstract int getLayout();
	public abstract String getItemName();
	public abstract Class<A> getAdapterClass();
	public abstract ModelAdapter.ClickListener getListener();
	public interface Callbacks {
		void onChangeSelection(boolean state);
		void toggleOneSelection(boolean state);
	}
}
