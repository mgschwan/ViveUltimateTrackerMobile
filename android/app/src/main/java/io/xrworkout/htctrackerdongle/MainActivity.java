package io.xrworkout.htctrackerdongle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;




public class MainActivity extends AppCompatActivity {

    final String TAG = "HIDTester";




    //public HTCConnectionHandler hidHandler = new HTCConnectionHandler();
    //public UsbHidDevice hidDevice;

    TabLayout mainTabLayout;
    ViewPager2 viewPager;
    ViewPagerAdapter viewPagerAdapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainTabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter( viewPagerAdapter );



        mainTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem( tab.getPosition() );
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager.registerOnPageChangeCallback(new OnPageChangeCallback() {
                                                   @Override
                                                   public void onPageSelected(int position) {
                                                       super.onPageSelected(position);
                                                       mainTabLayout.getTabAt( position ).select();
                                                   }
                                               }
        );









        //if ( deviceOpen ) bridge.CloseTheDevice();



        //hidDevice = new UsbHidDevice( this, hidHandler, 2996, 848);

    }




}