package bd.org.notetaker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import java.util.ArrayList;
import bd.org.notetaker.R;
import bd.org.notetaker.adapter.template.ModelAdapter;
import bd.org.notetaker.model.Category;
import bd.org.notetaker.widget.CategoryViewHolder;

public class CategoryAdapter extends ModelAdapter<Category, CategoryViewHolder> {
	public CategoryAdapter(ArrayList<Category> items, ArrayList<Category> selected, ClickListener<Category> listener) {
		super(items, selected, listener);
	}
	@Override
	public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new CategoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false));
	}
}
