/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.danielt3131.mipsemu.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.LayoutInflaterCompat;
import androidx.fragment.app.DialogFragment;

import io.github.danielt3131.mipsemu.R;

public class ProgramCounterDialog extends DialogFragment {
    private String programCounterValue;
    public ProgramCounterDialog(String programCounterValue) {
        this.programCounterValue = programCounterValue;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view  = inflater.inflate(R.layout.dialog_programcounter, null);
        builder.setView(view);
        builder.setTitle("Change Program Counter\nCurrent Value: " + programCounterValue);
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
                Toast.makeText(getContext(), "The program counter hasn't been modified", Toast.LENGTH_SHORT).show();
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
