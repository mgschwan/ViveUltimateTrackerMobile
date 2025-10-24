package io.xrworkout.htctrackerdongle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {


    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch ( position )
        {
            case 0:
                return new ConnectionFragment();
            case 1:
                return new ToolsFragment();
            case 2:
                DeviceContainer.statusFragment = new StatusFragment();
                return DeviceContainer.statusFragment;
            default:
                return new ConnectionFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
