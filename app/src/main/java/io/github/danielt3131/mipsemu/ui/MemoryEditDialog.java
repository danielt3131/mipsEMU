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
import androidx.appcompat.app.AlertDialog;;
import androidx.fragment.app.DialogFragment;

import io.github.danielt3131.mipsemu.R;

public class MemoryEditDialog extends DialogFragment {
    private String currentMemoryValue;
    public MemoryEditDialog(String currentMemoryValue) {
        this.currentMemoryValue = currentMemoryValue;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view  = inflater.inflate(R.layout.dialog_memoryset, null);
        builder.setView(view);
        builder.setTitle("Set Machine Memory\nCurrent Value in KB: " + currentMemoryValue);
        EditText text = view.findViewById(R.id.editMemorySize);
        text.setHint("This will reset the machine");
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("Memory Size: KB", String.valueOf(text.getText()));
                listener.onPositiveClick(MemoryEditDialog.this, Integer.parseInt(text.getText().toString()));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getContext(), "The memory size hasn't been modified", Toast.LENGTH_SHORT).show();
                MemoryEditDialog.this.getDialog().cancel();
            }
        });
        return builder.create();
    }

    MemoryEditDialog.MemoryEditDialogListener listener;
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (MemoryEditDialog.MemoryEditDialogListener) context;
        } catch (ClassCastException e) {
            Log.e(getActivity().toString(), e.getMessage());
            throw new ClassCastException(getActivity().toString());
        }
    }

    public interface MemoryEditDialogListener {
        void onPositiveClick(DialogFragment dialog, int memorySize);
    }

}
