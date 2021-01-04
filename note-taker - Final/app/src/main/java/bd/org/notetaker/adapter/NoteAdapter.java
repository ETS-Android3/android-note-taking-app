package bd.org.notetaker.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import java.util.ArrayList;
import bd.org.notetaker.R;
import bd.org.notetaker.adapter.template.ModelAdapter;
import bd.org.notetaker.model.Note;
import bd.org.notetaker.widget.NoteViewHolder;

public class NoteAdapter extends ModelAdapter<Note, NoteViewHolder> {
	public NoteAdapter(ArrayList<Note> items, ArrayList<Note> selected, ClickListener<Note> listener) {
		super(items, selected, listener);
	}
	@Override
	public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false));
	}
}
