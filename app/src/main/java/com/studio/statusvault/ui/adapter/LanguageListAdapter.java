package com.studio.statusvault.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.studio.statusvault.R;

/**
 * Renders a vertical list of languages; one row can be active (different design from a radio list).
 */
public class LanguageListAdapter extends RecyclerView.Adapter<LanguageListAdapter.Holder> {

    public interface OnSelectionListener {
        void onSelectionChanged(int position);
    }

    private final Context context;
    private int selected;
    @NonNull
    private final OnSelectionListener listener;
    private final String[] line1;
    private final String[] line2;

    public LanguageListAdapter(
            @NonNull Context context,
            int preselected,
            @NonNull OnSelectionListener listener) {
        this.context = context;
        this.selected = preselected;
        this.listener = listener;
        line1 = context.getResources().getStringArray(R.array.language_line_primary);
        line2 = context.getResources().getStringArray(R.array.language_line_secondary);
    }

    public int getSelectedPosition() {
        return selected;
    }

    @Override
    public int getItemCount() {
        return line1.length;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_language_row, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        boolean isSel = position == selected;
        h.text1.setText(line1[position]);
        h.text2.setText(line2[position]);
        h.card.setStrokeColor(ContextCompat.getColor(context, isSel
                ? R.color.colorAccent
                : R.color.lang_row_stroke_idle));
        float d = context.getResources().getDisplayMetrics().density;
        h.card.setStrokeWidth((int) ((isSel ? 2f : 1f) * d + 0.5f));
        h.stripe.setVisibility(isSel ? View.VISIBLE : View.GONE);
        h.iconMark.setImageResource(isSel ? R.drawable.ic_lang_selected : R.drawable.ic_lang_unselected);
        h.itemView.setOnClickListener(v -> {
            int old = selected;
            int pos = h.getAbsoluteAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            selected = pos;
            if (old != selected) {
                notifyItemChanged(old);
                notifyItemChanged(selected);
            } else {
                notifyItemChanged(selected);
            }
            listener.onSelectionChanged(pos);
        });
    }

    static class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View stripe;
        final TextView text1;
        final TextView text2;
        final androidx.appcompat.widget.AppCompatImageView iconMark;

        Holder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.cardLanguageRow);
            stripe = v.findViewById(R.id.viewLeftStripe);
            text1 = v.findViewById(R.id.textLanguageLine1);
            text2 = v.findViewById(R.id.textLanguageLine2);
            iconMark = v.findViewById(R.id.imageLanguageCheck);
        }
    }
}
