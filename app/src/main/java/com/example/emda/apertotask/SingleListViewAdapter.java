package com.example.emda.apertotask;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by emda on 2/16/2018.
 */

public class SingleListViewAdapter extends RecyclerView.Adapter<SingleListViewAdapter.ItemsViewHolder> {

    private List<String> listItems;

    public static class ItemsViewHolder extends RecyclerView.ViewHolder {
        public View mView;
        // each item is only a string
        TextView textView;

        public ItemsViewHolder(View view) {
            super(view);
            mView = view;
            textView = view.findViewById(R.id.listItemTextview);
        }
    }

    public SingleListViewAdapter(List<String> listItems) {
        this.listItems = listItems;
    }

    @Override
    public ItemsViewHolder onCreateViewHolder(ViewGroup parent,
                                              int viewType) {
        // specify the layout and create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_trend_item_view, parent, false);
        ItemsViewHolder vh = new ItemsViewHolder(v);
        return vh;
    }

    @Override
    // specify the contents of each item of the RecyclerView
    public void onBindViewHolder(ItemsViewHolder holder, int position) {
        holder.textView.setText(listItems.get(position));

    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }
}
