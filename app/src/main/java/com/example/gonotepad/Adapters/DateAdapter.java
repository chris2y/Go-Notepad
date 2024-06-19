package com.example.gonotepad.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gonotepad.NotesByDateActivity;
import com.example.gonotepad.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private List<Long> dates;
    Context context;

    public DateAdapter(Context mainActivity) {
        context = mainActivity;
    }

    public void setDates(List<Long> dates) {
        this.dates = dates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.date_item, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        long date = dates.get(position);
        holder.bind(formatDate(date));

        holder.date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, NotesByDateActivity.class);
                intent.putExtra("itemId", date);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates == null ? 0 : dates.size();
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewDate;
        private final CardView date;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDate = itemView.findViewById(R.id.dateTextView);
            date = itemView.findViewById(R.id.cardViewDate);
        }

        public void bind(String date) {
            textViewDate.setText(date);
        }
    }

    private String formatDate(long currentTimeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(currentTimeMillis));
    }
}
