package bd.org.notetaker.widget;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import bd.org.notetaker.R;
import bd.org.notetaker.inner.Formatter;
import bd.org.notetaker.model.DatabaseModel;
import bd.org.notetaker.model.Note;
import bd.org.notetaker.widget.template.ModelViewHolder;

public class NoteViewHolder extends ModelViewHolder<Note> {
	public ImageView badge;
	public TextView title;
	public TextView date;

	public NoteViewHolder(View itemView) {
		super(itemView);
		badge = (ImageView) itemView.findViewById(R.id.badge_icon);
		title = (TextView) itemView.findViewById(R.id.title_txt);
		date = (TextView) itemView.findViewById(R.id.date_txt);
	}

	@Override
	public void populate(Note item) {
		if (item.type == DatabaseModel.TYPE_NOTE_DRAWING) {
			badge.setImageResource(R.drawable.fab_drawing);
		} else {
			badge.setImageResource(R.drawable.fab_type);
		}
		title.setText(item.title);
		date.setText(Formatter.formatShortDate(item.createdAt));
	}
}
