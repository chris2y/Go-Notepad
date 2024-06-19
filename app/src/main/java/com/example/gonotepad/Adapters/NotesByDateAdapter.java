package com.example.gonotepad.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gonotepad.AddNoteActivity;
import com.example.gonotepad.R;
import com.example.gonotepad.RoomDatabase.NotesByDateEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesByDateAdapter extends RecyclerView.Adapter<NotesByDateAdapter.NotesByDateViewHolder> {

    private List<NotesByDateEntity> notesByDateList = new ArrayList<>();
    private Activity activity;

    private static final int EDIT_NOTE_REQUEST_CODE = 2;

    public NotesByDateAdapter(Activity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public NotesByDateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.note_item, parent, false);
        return new NotesByDateViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotesByDateViewHolder holder, int position) {
        NotesByDateEntity currentNote = notesByDateList.get(position);
        holder.textViewNote.setText(currentNote.getCompanyName());
        holder.timeTextview.setText(formatTime(currentNote.getNoteId()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // When a note is clicked, start AddNoteActivity with the note's data
                Intent intent = new Intent(activity, AddNoteActivity.class);
                intent.putExtra("phoneNumber", currentNote.getPhoneNumber());
                intent.putExtra("companyName", currentNote.getCompanyName());
                intent.putExtra("noteText", currentNote.getNoteText());
                intent.putExtra("dateId", currentNote.getDateId());
                intent.putExtra("noteId", currentNote.getNoteId());
                intent.putExtra("email", currentNote.getEmail());  // New field
                intent.putExtra("location", currentNote.getLocation());  // New field
                intent.putExtra("additionalInfo", currentNote.getAdditionalInfo());  // New field
                intent.putExtra("followUp", currentNote.getFollowUp());  // New field
                intent.putExtra("interestRate", currentNote.getInterestRate());  // New field

                activity.startActivityForResult(intent, EDIT_NOTE_REQUEST_CODE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notesByDateList.size();
    }

    public void setNotesByDate(List<NotesByDateEntity> notesByDateList) {
        this.notesByDateList = notesByDateList;
        notifyDataSetChanged();
    }

    static class NotesByDateViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewNote;
        private final TextView timeTextview;

        public NotesByDateViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNote = itemView.findViewById(R.id.noteTextView);
            timeTextview = itemView.findViewById(R.id.timeTextView);
        }
    }

    private String formatTime(long currentTimeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(currentTimeMillis));
    }
}
