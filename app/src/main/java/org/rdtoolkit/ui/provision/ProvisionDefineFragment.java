package org.rdtoolkit.ui.provision;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.rdtoolkit.R;
import org.rdtoolkit.util.ContextUtils;

public class ProvisionDefineFragment extends Fragment {

    private ProvisionViewModel mViewModel;

    public static ProvisionDefineFragment newInstance() {
        return new ProvisionDefineFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_provision_define, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(ProvisionViewModel.class);
        mViewModel.getViewInstructions().observe(getViewLifecycleOwner(), value -> {
            ((CheckBox)view.findViewById(R.id.provision_cbx_instructions)).setChecked(value);
        });
        ((CheckBox)view.findViewById(R.id.provision_cbx_instructions)).setOnCheckedChangeListener((listener, checked) -> {
            mViewModel.setViewInstructions(checked);
        });

        mViewModel.getSelectedTestProfile().observe(getViewLifecycleOwner(), value -> {
            if (value == null) {
                ((TextView)view.findViewById(R.id.define_txt_type)).setText(" - No Test Selected");
                ((TextView)view.findViewById(R.id.define_txt_process)).setText("");
            } else {
                ((TextView)view.findViewById(R.id.define_txt_type)).setText(" - " + value.readableName());
                Fragment f = this;
                ((TextView)view.findViewById(R.id.define_txt_process)).setText(String.format(" - Time to Resolve %s", new ContextUtils(requireContext()).getReadableTime(value.timeToResolve())));
            }
        });
    }
}