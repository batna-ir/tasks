/*
 * ASTRID: Android's Simple Task Recording Dashboard
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.timsu.astrid.activities;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.timsu.astrid.R;
import com.timsu.astrid.data.tag.TagController;
import com.timsu.astrid.data.tag.TagModelForView;
import com.timsu.astrid.data.task.TaskController;
import com.timsu.astrid.data.task.TaskModelForList;
import com.timsu.astrid.utilities.DateUtilities;
import com.timsu.astrid.utilities.Preferences;

/** Adapter for displaying a list of TaskModelForList entities
 *
 * @author timsu
 *
 */
public class TaskListAdapter extends ArrayAdapter<TaskModelForList> {

    public static final int CONTEXT_EDIT_ID       = Menu.FIRST + 50;
    public static final int CONTEXT_DELETE_ID     = Menu.FIRST + 51;
    public static final int CONTEXT_TIMER_ID      = Menu.FIRST + 52;

    private final Activity activity;
    private List<TaskModelForList> objects;
    private int resource;
    private LayoutInflater inflater;
    private TaskListAdapterHooks hooks;

    public interface TaskListAdapterHooks {
        List<TaskModelForList> getTaskArray();
        List<TagModelForView> getTagsFor(TaskModelForList task);
        TaskController getTaskController();
        TagController getTagController();
        void performItemClick(View v, int position);
        void onCreatedTaskListView(View v, TaskModelForList task);
    }

    public TaskListAdapter(Activity activity, Context context, int resource,
            List<TaskModelForList> objects, TaskListAdapterHooks hooks) {
        super(context, resource, objects);

        inflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        this.objects = objects;
        this.resource = resource;
        this.activity = activity;
        this.hooks = hooks;
    }

    // --- code for setting up each view

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;

        view = inflater.inflate(resource, parent, false);
        setupView(view, objects.get(position));
        addListeners(position, view);

        return view;
    }

    private void setupView(View view, final TaskModelForList task) {
        Resources r = activity.getResources();

        // find UI components
        final TextView name = ((TextView)view.findViewById(R.id.text1));
        final TextView dueDateView = ((TextView)view.findViewById(R.id.text_dueDate));
        final TextView remainingTimeView = ((TextView)view.findViewById(R.id.text_remainingTime));
        final TextView tagsView = ((TextView)view.findViewById(R.id.text_tags));
        final CheckBox progress = ((CheckBox)view.findViewById(R.id.cb1));
        final ImageView timer = ((ImageView)view.findViewById(R.id.imageLeft));
        final ImageView importance = ((ImageView)view.findViewById(R.id.imageRight));
        boolean hasProperties = false;

        view.setTag(task);
        progress.setTag(task);

        // name
        String nameValue = task.getName();
        if(task.getHiddenUntil() != null && task.getHiddenUntil().after(new Date()))
            nameValue = "(" + r.getString(R.string.taskList_hiddenPrefix) + ") " + nameValue;
        name.setText(nameValue);
        Integer fontSizePreference = Preferences.getTaskListFontSize(getContext());
        if(fontSizePreference != null && fontSizePreference > 0)
            name.setTextSize(fontSizePreference);
        if(task.getTimerStart() != null)
            timer.setImageDrawable(r.getDrawable(R.drawable.ic_dialog_time));
        importance.setImageDrawable(r.getDrawable(task.getImportance().getIconResource()));
        progress.setChecked(task.isTaskCompleted());

        // due date / completion date
        if(task.isTaskCompleted()) {
            if(task.getCompletionDate() != null) {
                int secondsLeft = (int)((task.getCompletionDate().getTime() -
                        System.currentTimeMillis()) / 1000);
                StringBuilder label = new StringBuilder().
                    append(r.getString(R.string.taskList_completedPrefix)).
                    append(" ").
                    append(DateUtilities.getDurationString(r, Math.abs(secondsLeft), 1)).
                    append(" " + r.getString(R.string.ago_suffix));
                dueDateView.setText(label);
                dueDateView.setTextColor(r.getColor(R.color.taskList_completedDate));
                hasProperties = true;
                dueDateView.setVisibility(View.VISIBLE);
            } else
                dueDateView.setVisibility(View.GONE);
        } else {
            Date dueDate = task.getDefiniteDueDate();
            String dueString = "";
            if(dueDate == null || (task.getPreferredDueDate() != null &&
                    task.getPreferredDueDate().before(dueDate))) {
                // only prefix with "goal:" if the real deadline isn't overdue
                if(task.getDefiniteDueDate() == null || task.getDefiniteDueDate().
                        after(new Date()))
                    dueString = r.getString(R.string.taskList_goalPrefix) + " ";
                dueDate = task.getPreferredDueDate();
            }
            if(dueDate != null) {
                long timeLeft = dueDate.getTime() - System.currentTimeMillis();

                if(timeLeft > 0)
                    dueString += r.getString(R.string.taskList_dueIn) + " ";
                else {
                    dueString += r.getString(R.string.taskList_overdueBy) + " ";
                    dueDateView.setTextColor(r.getColor(R.color.taskList_dueDateOverdue));
                }

                dueString += DateUtilities.getDurationString(r,
                        (int)Math.abs(timeLeft/1000), 1);
                dueDateView.setText(dueString);
                hasProperties = true;
                dueDateView.setVisibility(View.VISIBLE);
            } else
                dueDateView.setVisibility(View.GONE);
        }

        // remaining time
        if(task.getElapsedSeconds() > 0 || task.getEstimatedSeconds() > 0 ||
                task.getTimerStart() != null) {
            remainingTimeView.setVisibility(View.VISIBLE);
            int elapsed = task.getElapsedSeconds();
            if(task.getTimerStart() != null)
                elapsed += ((System.currentTimeMillis() - task.getTimerStart().getTime())/1000);
            String remainingString = "";
            if(!task.isTaskCompleted() && task.getEstimatedSeconds() > 0 &&
                    task.getRepeat() != null) {
                int remaining = task.getEstimatedSeconds() - elapsed;
                remainingString = DateUtilities.getShortDurationString(r,
                        (int)Math.abs(remaining), 1);
                if(remaining >= 0)
                    remainingString += r.getString(R.string.taskList_remaining);
                else
                    remainingString += r.getString(R.string.taskList_overtime);
            } else if(elapsed > 0) {
                remainingString = DateUtilities.getShortDurationString(r,
                        Math.abs(elapsed), 1) +
                        r.getString(R.string.taskList_spent);
            }

            if(remainingString.length() == 0) {
                remainingTimeView.setVisibility(View.GONE);
            } else {
                hasProperties = true;
                remainingTimeView.setText(remainingString);
                if(task.isTaskCompleted())
                    remainingTimeView.setTextColor(r.getColor(R.color.taskList_completedDate));
            }
        } else {
            remainingTimeView.setVisibility(View.GONE);
        }

        // tags
        List<TagModelForView> tags = hooks.getTagsFor(task);
        StringBuilder tagString = new StringBuilder();
        for(Iterator<TagModelForView> i = tags.iterator(); i.hasNext(); ) {
            TagModelForView tag = i.next();
            tagString.append(tag.getName());
            if(i.hasNext())
                tagString.append(", ");
        }
        if(tagString.length() > 0) {
            tagsView.setText(r.getString(R.string.tags_prefix) + " " +
                    tagString);
        } else if(!hasProperties) {
            tagsView.setText(r.getString(R.string.no_tags));
        }

        setTaskAppearance(task, name, progress);
        hooks.onCreatedTaskListView(view, task);
    }

    private void addListeners(final int position, final View view) {
        final CheckBox progress = ((CheckBox)view.findViewById(R.id.cb1));

        // clicking the check box
        progress.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                TaskModelForList task = (TaskModelForList)buttonView.getTag();

                int newProgressPercentage;
                if(isChecked)
                    newProgressPercentage =
                        TaskModelForList.getCompletedPercentage();
                else
                    newProgressPercentage = 0;

                if(newProgressPercentage != task.getProgressPercentage()) {
                    setTaskProgress(task, view, newProgressPercentage);
                    setupView(view, task);
                }
            }
        });

        // clicking the text field
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hooks.performItemClick(view, position);
            }
        });

        // long-clicking the text field
        view.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenuInfo menuInfo) {
                TaskModelForList task = (TaskModelForList)v.getTag();

                menu.add(position, CONTEXT_EDIT_ID, Menu.NONE,
                        R.string.taskList_context_edit);
                menu.add(position, CONTEXT_DELETE_ID, Menu.NONE,
                        R.string.taskList_context_delete);

                int timerTitle;
                if(task.getTimerStart() == null)
                    timerTitle = R.string.taskList_context_startTimer;
                else
                    timerTitle = R.string.taskList_context_stopTimer;
                menu.add(position, CONTEXT_TIMER_ID, Menu.NONE, timerTitle);

                menu.setHeaderTitle(task.getName());
            }
        });
    }

    private void setTaskProgress(final TaskModelForList task, View view, int progress) {
        final ImageView timer = ((ImageView)view.findViewById(R.id.imageLeft));
        task.setProgressPercentage(progress);
        hooks.getTaskController().saveTask(task);

        // if our timer is on, ask if we want to stop
        if(progress == 100 && task.getTimerStart() != null) {
            new AlertDialog.Builder(activity)
            .setTitle(R.string.question_title)
            .setMessage(R.string.stop_timer_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    task.stopTimerAndUpdateElapsedTime();
                    hooks.getTaskController().saveTask(task);
                    timer.setVisibility(View.GONE);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
        }
    }

    private void setTaskAppearance(TaskModelForList task, TextView name, CheckBox progress) {
        Resources r = activity.getResources();

        if(task.isTaskCompleted()) {
            name.setBackgroundDrawable(r.getDrawable(R.drawable.strikeout));
            name.setTextColor(r.getColor(R.color.task_list_done));
            progress.setButtonDrawable(R.drawable.btn_check0);
        } else {
            name.setBackgroundDrawable(null);
            name.setTextColor(r.getColor(task.getTaskColorResource(getContext())));

            if(task.getProgressPercentage() >= 75)
                progress.setButtonDrawable(R.drawable.btn_check75);
            else if(task.getProgressPercentage() >= 50)
                progress.setButtonDrawable(R.drawable.btn_check50);
            else if(task.getProgressPercentage() >= 25)
                progress.setButtonDrawable(R.drawable.btn_check25);
            else
                progress.setButtonDrawable(R.drawable.btn_check0);
        }
    }

}