/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.model.task;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.content.ContentValues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.helper.DateTimeHelper;
import de.azapps.mirakel.helper.Log;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.list.SpecialList;
import de.azapps.mirakel.model.recurring.Recurring;
import de.azapps.mirakel.sync.SyncAdapter.SYNC_STATE;

class TaskBase {
	private static final String TAG = "TaskBase";
	private long id = 0;
	private String uuid = "";
	private ListMirakel list;
	private String name;
	private String content;
	private boolean done;
	private Calendar due;
	private int priority;
	private Calendar createdAt;
	private Calendar updatedAt;
	protected Map<String, Boolean> edited = new HashMap<String, Boolean>();
	private Map<String, String> additionalEntries = null;
	private String additionalEntriesString;
	private SYNC_STATE sync_state;
	private Calendar reminder;
	private int recurring;
	private int recurring_reminder;

	TaskBase(long id, String uuid, ListMirakel list, String name,
			String content, boolean done, Calendar due, Calendar reminder,
			int priority, Calendar created_at, Calendar updated_at,
			SYNC_STATE sync_state, String additionalEntriesString,
			int recurring, int recurring_reminder) {
		this.id = id;
		this.uuid = uuid;
		this.setList(list);
		this.setName(name);
		this.setContent(content);
		this.setDone(done);
		this.setDue(due);
		this.setReminder(reminder);
		this.setPriority(priority);
		this.setCreatedAt(created_at);
		this.setUpdatedAt(updated_at);
		this.setSyncState(sync_state);
		this.additionalEntriesString = additionalEntriesString;
		this.recurring = recurring;
		this.recurring_reminder = recurring_reminder;
	}

	TaskBase() {

	}

	TaskBase(String name) {
		this.id = 0;
		this.uuid = java.util.UUID.randomUUID().toString();
		this.setList(SpecialList.first());
		this.setName(name);
		this.setContent("");
		this.setDone(false);
		this.setDue(null);
		this.setReminder(null);
		this.setPriority(0);
		this.setCreatedAt((Calendar) null);
		this.setUpdatedAt((Calendar) null);
		this.setSyncState(SYNC_STATE.NOTHING);
		this.recurring = -1;
		this.recurring_reminder = -1;
	}

	public Recurring getRecurring() {
		return Recurring.get(recurring);
	}

	public void setRecurring(int recurring) {
		this.recurring = recurring;
		edited.put("recurring", true);
	}

	public Recurring getRecurringReminder() {
		return Recurring.get(recurring_reminder);
	}

	public void setRecurringReminder(int recurring) {
		this.recurring_reminder = recurring;
		edited.put("recurring_reminder", true);
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
		edited.put("id", true);
	}

	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
		edited.put("uuid", true);
	}

	public ListMirakel getList() {
		return list;
	}

	public void setList(ListMirakel list) {
		this.list = list;
		edited.put("list", true);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		edited.put("name", true);
	}

	public String getContent() {
		if (content == null)
			return "";
		else
			return content;
	}

	public void setContent(String content) {
		if (content != null) {
			this.content = StringUtils.replaceEach(content.trim(),
					new String[] { "\\n", "\\\"", "\b" }, new String[] { "\n",
							"\"", "" });
		} else {
			this.content = null;
		}
		edited.put("content", true);
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
		edited.put("done", true);
	}

	public void toggleDone() {
		this.done = !this.done;
		edited.put("done", true);
	}

	public Calendar getDue() {
		if (due == null)
			return due;
		if (recurring != -1 && due.before(new GregorianCalendar())) {
			due = getRecurring().addRecurring(due);
			try {
				edited.put("due", true);
				((Task) this).save(false);
			} catch (NoSuchListException e) {
				Log.w(TAG, "List did vanish");
			}
		}
		return due;
	}

	public void setDue(Calendar due) {
		this.due = due;
		edited.put("due", true);
		if(due==null){
			setRecurring(-1);
		}
	}

	public Calendar getReminder() {
		if(reminder==null)
			return reminder;
		if (recurring_reminder != -1
				&& reminder.before(new GregorianCalendar())) {
			reminder = getRecurringReminder().addRecurring(due);
			try {
				edited.put("reminder", true);
				((Task) this).save(false);
			} catch (NoSuchListException e) {
				Log.w(TAG, "List did vanish");
			}
		}
		return reminder;
	}

	public void setReminder(Calendar reminder) {
		this.reminder = reminder;
		edited.put("reminder", true);
		if(reminder==null){
			setRecurringReminder(-1);
		}
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
		edited.put("priority", true);
	}

	public Calendar getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Calendar created_at) {
		this.createdAt = created_at;
	}

	public void setCreatedAt(String created_at) {
		try {
			setCreatedAt(DateTimeHelper.parseDateTime(created_at));
		} catch (ParseException e) {
			setCreatedAt((Calendar) null);
		}
	}

	public Calendar getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Calendar updated_at) {
		this.updatedAt = updated_at;
	}

	public void setUpdatedAt(String updated_at) {
		try {
			setUpdatedAt(DateTimeHelper.parseDateTime(updated_at));
		} catch (ParseException e) {
			setUpdatedAt((Calendar) null);
		}
	}

	public SYNC_STATE getSyncState() {
		return sync_state;
	}

	public void setSyncState(SYNC_STATE sync_state) {
		this.sync_state = sync_state;
	}

	public Map<String, Boolean> getEdited() {
		return edited;
	}

	public Map<String, String> getAdditionalEntries() {
		initAdditionalEntries();
		return additionalEntries;
	}

	public void setAdditionalEntries(Map<String, String> additionalEntries) {
		this.additionalEntries = additionalEntries;
		edited.put("additionalEntries", true);
	}

	public void addAdditionalEntry(String key, String value) {
		initAdditionalEntries();
		additionalEntries.put(key, value);
	}

	public void removeAdditionalEntry(String key) {
		initAdditionalEntries();
		additionalEntries.remove(key);
	}

	boolean isEdited() {
		return edited.size() > 0;
	}

	boolean isEdited(String key) {
		return edited.containsKey(key);
	}

	void clearEdited() {
		edited.clear();
	}

	/**
	 * This function parses the additional fields only if it is necessary
	 */
	private void initAdditionalEntries() {
		if (additionalEntries == null) {
			if (additionalEntriesString == null
					|| additionalEntriesString.trim().equals("")
					|| additionalEntriesString.trim().equals("null")) {
				this.additionalEntries = new HashMap<String, String>();
			} else {
				Gson gson = new Gson();
				Type mapType = new TypeToken<Map<String, String>>() {
				}.getType();
				this.additionalEntries = gson.fromJson(additionalEntriesString,
						mapType);
			}
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public ContentValues getContentValues() throws NoSuchListException {
		ContentValues cv = new ContentValues();
		cv.put("_id", id);
		cv.put("uuid", uuid);
		if (list == null)
			throw new NoSuchListException();
		cv.put("list_id", list.getId());
		cv.put("name", name);
		cv.put("content", content);
		cv.put("done", done);
		String due = (this.due == null ? null : DateTimeHelper
				.formatDate(getDue()));
		cv.put("due", due);
		String reminder = null;
		if (this.reminder != null)
			reminder = DateTimeHelper.formatDateTime(this.reminder);
		cv.put("reminder", reminder);
		cv.put("priority", priority);
		String createdAt = null;
		if (this.createdAt != null)
			createdAt = DateTimeHelper.formatDateTime(this.createdAt);
		cv.put("created_at", createdAt);
		String updatedAt = null;
		if (this.updatedAt != null)
			updatedAt = DateTimeHelper.formatDateTime(this.updatedAt);
		cv.put("updated_at", updatedAt);
		cv.put("sync_state", sync_state.toInt());

		Gson gson = new GsonBuilder().create();
		String additionalEntries = gson.toJson(this.additionalEntries);
		cv.put("additional_entries", additionalEntries);
		return cv;
	}

}
