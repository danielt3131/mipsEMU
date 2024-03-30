package io.github.danielt3131.mipsemu.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.LayoutInflaterCompat;
import androidx.fragment.app.DialogFragment;

import io.github.danielt3131.mipsemu.R;

public class ProgramCounterDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view  = inflater.inflate(R.layout.dialog_programcounter, null);
        builder.setView(view);
        builder.setTitle("PC Edit");
        EditText text = view.findViewById(R.id.editPCValue);
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("PC Edit", String.valueOf(text.getText()));
                listener.onPositiveClick(ProgramCounterDialog.this, text.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProgramCounterDialog.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    ProgramCounterDialogListener listener;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (ProgramCounterDialogListener) context;
        } catch (ClassCastException e) {
            Log.e(getActivity().toString(), e.getMessage());
            throw new ClassCastException(getActivity().toString());
        }
    }

    public interface ProgramCounterDialogListener {
        void onPositiveClick(DialogFragment dialog, String programCounterValue);
    }
}
