package bd.org.notetaker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Locale;

import bd.org.notetaker.model.DatabaseModel;
import bd.org.notetaker.model.Note;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class Controller {
	public static final int SORT_TITLE_ASC = 0;
	public static final int SORT_TITLE_DESC = 1;
	public static final int SORT_DATE_ASC = 2;
	public static final int SORT_DATE_DESC = 3;

	/**
	 * The singleton instance of Controller class
	 */
	public static Controller instance = null;

	private SQLiteOpenHelper helper;
	private String[] sorts = {
		OpenHelper.COLUMN_TITLE + " ASC",
		OpenHelper.COLUMN_TITLE + " DESC",
		OpenHelper.COLUMN_ID + " ASC",
		OpenHelper.COLUMN_ID + " DESC",
	};

	private Controller(Context context) {
		helper = new OpenHelper(context);
	}

	/**
	 * Instantiates the singleton instance of Controller class
	 * @param context the application context
	 */
	public static void create(Context context) {
		instance = new Controller(context);
	}



	public <T extends DatabaseModel> ArrayList<T> findNotes(Class<T> cls, String[] columns, String where, String[] whereParams, int sortId) {
		ArrayList<T> items = new ArrayList<>();

		SQLiteDatabase db = helper.getReadableDatabase();

		try {
			Cursor c = db.query(
				OpenHelper.TABLE_NOTES,
				columns,
				where,
				whereParams,
				null, null,
				sorts[sortId]
			);

			if (c != null) {
				while (c.moveToNext()) {
					try {
						items.add(cls.getDeclaredConstructor(Cursor.class).newInstance(c));
					} catch (Exception ignored) {
					}
				}

				c.close();
			}

			return items;
		} finally {
			db.close();
		}
	}



	public <T extends DatabaseModel> T findNote(Class<T> cls, long id) {
		SQLiteDatabase db = helper.getReadableDatabase();

		try {
			Cursor c = db.query(
				OpenHelper.TABLE_NOTES,
				null,
				OpenHelper.COLUMN_ID + " = ?",
				new String[] {
					String.format(Locale.US, "%d", id)
				},
				null, null, null
			);

			if (c == null) return null;

			if (c.moveToFirst()) {
				try {
					return cls.getDeclaredConstructor(Cursor.class).newInstance(c);
				} catch (Exception e) {
					return null;
				}
			}

			return null;
		} finally {
			db.close();
		}
	}

	/**
	 * Change the amount of category counter
	 * @param categoryId the id of category
	 * @param amount to be added (negative or positive)
	 */
	public void addCategoryCounter(long categoryId, int amount) {
		SQLiteDatabase db = helper.getWritableDatabase();

		try {
			Cursor c = db.rawQuery(
				"UPDATE " + OpenHelper.TABLE_NOTES + " SET " + OpenHelper.COLUMN_COUNTER + " = " + OpenHelper.COLUMN_COUNTER + " + ? WHERE " + OpenHelper.COLUMN_ID + " = ?",
				new String[]{
					String.format(Locale.US, "%d", amount),
					String.format(Locale.US, "%d", categoryId)
				}
			);

			if (c != null) {
				c.moveToFirst();
				c.close();
			}
		} finally {
			db.close();
		}
	}

	/**
	 * Restores last deleted notes
	 */
	public void undoDeletion() {
		SQLiteDatabase db = helper.getWritableDatabase();

		try {
			Cursor c = db.query(
				OpenHelper.TABLE_UNDO,
				null, null, null, null, null, null
			);

			if (c != null) {
				while (c.moveToNext()) {
					String query = c.getString(c.getColumnIndex(OpenHelper.COLUMN_SQL));
					if (query != null) {
						Cursor nc = db.rawQuery(
							query,
							null
						);

						if (nc != null) {
							nc.moveToFirst();
							nc.close();
						}
					}
				}

				c.close();
			}

			clearUndoTable(db);
		} finally {
			db.close();
		}
	}

	/**
	 * Clears the undo table
	 * @param db an object of writable SQLiteDatabase
	 */
	public void clearUndoTable(SQLiteDatabase db) {
		Cursor uc = db.rawQuery("DELETE FROM " + OpenHelper.TABLE_UNDO, null);
		if (uc != null) {
			uc.moveToFirst();
			uc.close();
		}
	}

	/**
	 * Deletes a note or category (and its children) from the database
	 * @param ids a list of the notes' IDs
	 * @param categoryId the id of parent category
	 */
	public void deleteNotes(String[] ids, long categoryId) {
		SQLiteDatabase db = helper.getWritableDatabase();

		try {
			clearUndoTable(db);

			StringBuilder where = new StringBuilder();
			StringBuilder childWhere = new StringBuilder();

			boolean needOR = false;
			for (int i = 0; i < ids.length; i++) {
				if (needOR) {
					where.append(" OR ");
					childWhere.append(" OR ");
				} else {
					needOR = true;
				}
				where.append(OpenHelper.COLUMN_ID).append(" = ?");
				childWhere.append(OpenHelper.COLUMN_PARENT_ID).append(" = ?");
			}

			int count = db.delete(
				OpenHelper.TABLE_NOTES,
				where.toString(),
				ids
			);

			if (categoryId == DatabaseModel.NEW_MODEL_ID) {
				db.delete(
					OpenHelper.TABLE_NOTES,
					childWhere.toString(),
					ids
				);
			} else {
				Cursor c = db.rawQuery(
					"UPDATE " + OpenHelper.TABLE_NOTES + " SET " + OpenHelper.COLUMN_COUNTER + " = " + OpenHelper.COLUMN_COUNTER + " - ? WHERE " + OpenHelper.COLUMN_ID + " = ?",
					new String[]{
						String.format(Locale.US, "%d", count),
						String.format(Locale.US, "%d", categoryId)
					}
				);

				if (c != null) {
					c.moveToFirst();
					c.close();
				}
			}
		} finally {
			db.close();
		}
	}

	/**
	 * Inserts or updates a note or category in the database and increments the counter
	 * of category if the deleted object is an instance of Note class
	 * @param note the object of type T
	 * @param values ContentValuse of the object to be inserted or updated
	 * @param <T> a type which extends DatabaseModel
	 * @return the id of saved note
	 */
	public <T extends DatabaseModel> long saveNote(T note, ContentValues values) {
		SQLiteDatabase db = helper.getWritableDatabase();

		try {
			if (note.id > DatabaseModel.NEW_MODEL_ID) {
				// Update note
				db.update(
					OpenHelper.TABLE_NOTES,
					note.getContentValues(),
					OpenHelper.COLUMN_ID + " = ?",
					new String[]{
						String.format(Locale.US, "%d", note.id)
					}
				);
				return note.id;
			} else {
				// Create a new note
				note.id = db.insert(
					OpenHelper.TABLE_NOTES,
					null,
					values
				);

				if (note instanceof Note) {
					// Increment the counter of category
					Cursor c = db.rawQuery(
						"UPDATE " + OpenHelper.TABLE_NOTES + " SET " + OpenHelper.COLUMN_COUNTER + " = " + OpenHelper.COLUMN_COUNTER + " + 1 WHERE " + OpenHelper.COLUMN_ID + " = ?",
						new String[]{
							String.format(Locale.US, "%d", ((Note) note).categoryId)
						}
					);

					if (c != null) {
						c.moveToFirst();
						c.close();
					}
				}

				return note.id;
			}
		} catch (Exception e) {
			return DatabaseModel.NEW_MODEL_ID;
		} finally {
			db.close();
		}
	}
}
