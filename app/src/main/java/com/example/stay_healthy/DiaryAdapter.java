package com.example.stay_healthy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiaryAdapter extends BaseAdapter {

    private Context context;
    private List<DiaryEntry> diaryList;
    private boolean isMultiSelectMode = false;
    private Set<String> selectedKeys = new HashSet<>();

    public DiaryAdapter(Context context, List<DiaryEntry> diaryList) {
        this.context = context;
        this.diaryList = diaryList;
    }

    public void setMultiSelectMode(boolean enable) {
        isMultiSelectMode = enable;
        selectedKeys.clear();
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void toggleSelection(String key) {
        if (selectedKeys.contains(key)) {
            selectedKeys.remove(key);
        } else {
            selectedKeys.add(key);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedKeys() {
        return selectedKeys;
    }

    @Override
    public int getCount() {
        return diaryList.size();
    }

    @Override
    public Object getItem(int position) {
        return diaryList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_diary, parent, false);
        }

        DiaryEntry entry = diaryList.get(position);

        TextView tvDate = convertView.findViewById(R.id.tv_item_date);
        TextView tvContent = convertView.findViewById(R.id.tv_item_content);
        CheckBox checkBox = convertView.findViewById(R.id.checkbox_select);

        tvDate.setText(entry.fullDate);
        tvContent.setText(entry.content);

        if (isMultiSelectMode) {
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setChecked(selectedKeys.contains(entry.key));
        } else {
            checkBox.setVisibility(View.GONE);
        }

        checkBox.setClickable(false);

        return convertView;
    }
}