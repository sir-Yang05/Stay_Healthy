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
    private boolean isMultiSelectMode = false; // 是否处于多选模式
    private Set<String> selectedKeys = new HashSet<>(); // 存被选中的日记ID

    public DiaryAdapter(Context context, List<DiaryEntry> diaryList) {
        this.context = context;
        this.diaryList = diaryList;
    }

    // 开启/关闭多选模式
    public void setMultiSelectMode(boolean enable) {
        isMultiSelectMode = enable;
        selectedKeys.clear(); // 切换模式时清空选择
        notifyDataSetChanged();
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    // 选中或取消选中某一项
    public void toggleSelection(String key) {
        if (selectedKeys.contains(key)) {
            selectedKeys.remove(key);
        } else {
            selectedKeys.add(key);
        }
        notifyDataSetChanged();
    }

    // 获取所有选中的ID（用于删除）
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

        // 核心逻辑：如果是多选模式，显示复选框；否则隐藏
        if (isMultiSelectMode) {
            checkBox.setVisibility(View.VISIBLE);
            // 检查这个条目是否被选中
            checkBox.setChecked(selectedKeys.contains(entry.key));
        } else {
            checkBox.setVisibility(View.GONE);
        }

        // 防止点击复选框本身导致冲突，我们主要靠点击整行来操作
        checkBox.setClickable(false);

        return convertView;
    }
}